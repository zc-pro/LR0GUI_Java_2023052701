import java.util.*;

public class LR0Parser {

    private Map<String, List<String>> productions = new LinkedHashMap<>();      // 存储输入的文法产生式
    private Map<String, List<String>> augmentedProductions = new LinkedHashMap<>();  // 存储拓广后的文法产生式
    private Set<String> nonTerminals = new HashSet<>();  // 存储非终结符
    private Set<String> terminals = new HashSet<>();     // 存储终结符
    private Map<Integer, Map<String, String>> actionTable = new HashMap<>();    // 存储LR(0)分析表中action表
    private Map<Integer, Map<String, Integer>> gotoTable = new HashMap<>();     // 存储LR(0)分析表中goto表
    private List<Set<String>> states = new ArrayList<>();    // 存储状态集合
    private Map<Integer, Map<String, Integer>> dfaGraph = new HashMap<>();  // 存储DFA图
    private Map<Integer, List<String>> stateItems = new HashMap<>();    // 存储每个状态对应的项目集合
    private Map<String, Integer> productionIds = new HashMap<>();   // 存储每个产生式的编号
    private Map<Set<String>, Integer> stateIds = new HashMap<>();    // 存储每个状态的编号
    private int maxStateId = 0;        // 存储当前最大的状态编号
    private String augmentedStart;    // 存储拓广文法的开始符号
    private Stack<Integer> stateStack;      // 输入串分析过程的状态栈
    private Stack<String> characterStack;   // 输入串分析过程的符号栈
    private Map<Integer, List<String>> analysisProcess; //存储输入串的文法分析过程
    private int numRow = 0;   //存储输入串LR(0)分析过程对应的分析表的当前行，初始化为0

    private boolean isAcc = false; //判断输入串是否符合文法
    public Set<String> getTerminals() {
        return terminals;
    }
    public Set<String> getNonTerminals() {
        return nonTerminals;
    }
    public Map<String, List<String>> getAugmentedProductions() {
        return augmentedProductions;
    }
    public Map<Integer, List<String>> getAnalysisProcess() {
        return analysisProcess;
    }

    public String getAugmentedStart() {
        return augmentedStart;
    }
    public List<Set<String>> getStates() {
        return states;
    }
    public Map<Integer, Map<String, String>> getActionTable() {
        return actionTable;
    }
    public Map<Integer, Map<String, Integer>> getGotoTable() {
        return gotoTable;
    }


    /*
     *读入文法产生式
     */
    public void readGrammarProductions(String input) {
        //Scanner scanner = new Scanner(System.in);
        //System.out.println("请输入文法产生式（最后一行输入#终止输入）：");
        String[] lines = input.split("\n");
        for (String line : lines) {
            String[] tokens = line.split("->");     // 将产生式以”->“为界划分为两部分:tokens[0]和tokens[1]
            String left = tokens[0].trim();               //left = tokens[0] 存储产生式左部， right = tokens[1] 存储产生式右部
            String right = tokens[1].trim();
            List<String> production = productions.getOrDefault(left, new ArrayList<>());
            if (right.contains("|")) {                  //当产生式右部存在 ”|“时
                String temp = right.replaceAll(" ", "");
                String[] stringlist = temp.split("\\|");      //如遇到 B -> aB | b | c 这种含多“|”的情形，按照“|”将右部分割
                for (int i = 0; i < stringlist.length; i++) {
                    production.add(stringlist[i]);
                }
                productions.put(left, production);
            } else {    //右部不存在“|”
                production.add(right);
                productions.put(left, production);
            }
            nonTerminals.add(left); //非终结符
            String[] symbols = right.split("\\s+");  //去除空格
            String strEnd = new String();       //存储终结符
            boolean isLetter = false;           //判断是否存储终结符
            for (String symbol : symbols) {
                if (!symbol.isEmpty() && !symbol.equals("|")) {
                    for (int i = 0; i < symbol.length(); i++) {
                        if (Character.isUpperCase(symbol.charAt(i))) { //当前字符为非终结符
                            nonTerminals.add(String.valueOf(symbol.charAt(i)));
                        } else if (symbol.charAt(i) != '|') {
                            while (i < symbol.length()) {
                                if (Character.isLowerCase(symbol.charAt(i))) {
                                    strEnd += symbol.charAt(i);
                                    isLetter = true;
                                    if (i == symbol.length() - 1) {
                                        terminals.add(strEnd);
                                        strEnd = "";
                                        isLetter = false;
                                    }
                                    i++;
                                } else if (Character.isUpperCase(symbol.charAt(i))) {
                                    nonTerminals.add(String.valueOf(symbol.charAt(i)));
                                    i++;
                                    if (isLetter) {
                                        if (!strEnd.isEmpty()) {
                                            terminals.add(strEnd);
                                        }
                                        strEnd = "";
                                        isLetter = false;
                                    }
                                } else if (!Character.isUpperCase(symbol.charAt(i)) && symbol.charAt(i) != '|') {  // 一些其他字符，如“，”也属于终结符
                                    terminals.add(String.valueOf(symbol.charAt(i)));
                                    i++;
                                    if (isLetter) {
                                        if (!strEnd.isEmpty()) {
                                            terminals.add(strEnd);
                                        }
                                        strEnd = "";
                                        isLetter = false;
                                    }
                                } else {
                                    i++;
                                    if (isLetter) {
                                        if (!strEnd.isEmpty()) {
                                            terminals.add(strEnd);
                                        }
                                        strEnd = "";
                                    }
                                }
                            }
                        } else {
                            continue;
                        }
                    }
                }
            }
            //line = scanner.nextLine(); //读取下一行
        }
    }
    /*
     * 拓广文法
     */
    public void augmentedProductions() {
        List<String> startProduction = new ArrayList<>();
        startProduction.add(productions.keySet().iterator().next() + " #");
        augmentedStart = productions.keySet().iterator().next() + "\'"; //新的文法开始符 如原输入的文法开始符为S，则拓广文法中为S‘
        augmentedProductions.put(augmentedStart, startProduction);
        for (Map.Entry<String, List<String>> entry : productions.entrySet()) {
            augmentedProductions.put(entry.getKey(), entry.getValue());
        }
        nonTerminals.add(augmentedStart);
    }
    /*
     * 构建LR(0)有限自动机
     */
    public void buildLR0Automaton() {
        // 计算每个产生式的编号，方便构造DFA图时使用
        int id = 0;
        for (String left : augmentedProductions.keySet()) {
            List<String> production = augmentedProductions.get(left);
            if (production.size() != 1) {
                for (String s : production) {
                    productionIds.put(left + " -> " + s, Integer.valueOf(id++));
                }
            } else
                productionIds.put(left + " -> " + augmentedProductions.get(left).get(0), Integer.valueOf(id++));
        }

        Set<String> temp = new HashSet<>();
        temp.add(augmentedStart + " -> . " + augmentedProductions.get(augmentedStart).get(0));
        Set<String> startState = computeClosure(temp); //开始项目
        stateIds.put(startState, Integer.valueOf(maxStateId++));
        states.add(startState); //将开始项目加入项目集

        dfaGraph.put(Integer.valueOf(0), new HashMap<>());
        for (int i = 0; i < states.size(); i++) {
            Set<String> state = states.get(i);
            stateItems.put(Integer.valueOf(i), new ArrayList<>(state));
            Map<String, Integer> transition = new HashMap<>();
            for (String symbol : nonTerminals) {
                if (!symbol.equals(augmentedStart)) { //当非终结符不是拓广文法开始符
                    Set<String> nextState = goTo(state, symbol);  //计算当前状态集经过symbol跳转得到的状态集
                    if (!nextState.isEmpty()) {
                        if (!stateIds.containsKey(nextState)) { //若得到的项目集不在stateIds和states中时，加入其中
                            stateIds.put(nextState, Integer.valueOf(maxStateId++));
                            states.add(nextState);
                        }
                        int nextStateId = stateIds.get(nextState);
                        transition.put(symbol, Integer.valueOf(nextStateId));// 表示当前状态集经过symbol跳转到nextStateId
                    }
                }
            }
            for (String symbol : terminals) {
                Set<String> nextState = goTo(state, symbol);
                if (!nextState.isEmpty()) {
                    if (!stateIds.containsKey(nextState)) {
                        stateIds.put(nextState, Integer.valueOf(maxStateId++));
                        states.add(nextState);
                    }
                    int nextStateId = stateIds.get(nextState);
                    transition.put(symbol, Integer.valueOf(nextStateId));
                }
            }
            dfaGraph.put(Integer.valueOf(i), transition); //将某一状态集合的goto情况添加到dfa中
        }
        //构建LR(0)分析表
        produceAnalysisTable();
    }
    /*
     *  构建LR(0)分析表
     */
    public void produceAnalysisTable() {
        for (int i = 0; i < states.size(); i++) {
            boolean findAcc = false;
            Map<String, String> actionRow = new HashMap<>();
            Map<String, Integer> gotoRow = new HashMap<>();
            if (dfaGraph.get(i).isEmpty()) { //此时状态集i没有跳转
                String state = stateItems.get(i).get(0).replaceAll(" ", "");
                if (state.endsWith(".")) {
                    for (String symbol : terminals) {
                        for (String production : productionIds.keySet()) {
                            String s1 = production.replaceAll(" ", "");
                            String s2 = stateItems.get(i).get(0).replaceAll(" ", "");
                            if (s2.equals(s1 + ".")) {
                                actionRow.put(symbol, "r" + productionIds.get(production));
                                actionRow.put("#", "r" + productionIds.get(production));
                            }
                        }
                    }
                    for (String symbol : nonTerminals) {
                        if (!symbol.equals(augmentedStart))
                            gotoRow.put(symbol, Integer.valueOf(-1));
                    }
                } else if (state.endsWith("#")) {
                    actionRow.put("#", "acc");
                    for (String symbol : terminals) {
                        actionRow.put(symbol, " ");
                    }
                    for (String symbol : nonTerminals) {
                        if (!symbol.equals(augmentedStart))
                            gotoRow.put(symbol, Integer.valueOf(-1));
                    }
                }
            } else {
                for (String j : terminals) {
                    for (String str : dfaGraph.get(i).keySet()) {
                        if (str.equals(j)) {
                            actionRow.put(str, "S" + dfaGraph.get(i).get(str));
                        }
                    }
                }
                actionRow.put("#", " ");
                for (String j : nonTerminals) {
                    if (!j.equals(augmentedStart)) {
                        boolean notEnd = true;
                        for (String str : dfaGraph.get(i).keySet()) {
                            if (str.equals(j)) {
                                gotoRow.put(str, dfaGraph.get(i).get(str));
                                notEnd = false;
                            }
                            String start = augmentedStart.replaceAll("\'", "");
                            //判断 acc 的位置
                            if (i != 0) {
                                if (!findAcc && j.equals(start)) {
                                    int num = dfaGraph.get(i).get(str);
                                    for (String s : stateItems.get(i)) {
                                        if (s.endsWith("#")) {
                                            actionRow.put("#", "acc");
                                            findAcc = true;
                                        }
                                    }
                                }
                            }
                        }
                        if (notEnd)
                            gotoRow.put(j, Integer.valueOf(-1));
                    }
                }
            }
            actionTable.put(Integer.valueOf(i), actionRow);
            gotoTable.put(Integer.valueOf(i), gotoRow);
        }
    }
    /*
     *  计算指定项目集合的闭包
     */
    private Set<String> computeClosure(Set<String> item) {
        Set<String> closure = new HashSet<>();
        for (String str : item) {
            closure.add(str);  //闭包包含本身
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (String i : new HashSet<>(closure)) {
                String[] tokens = i.split("\\s+");
                for (int j = 0; j < tokens.length; j++) {
                    if (tokens[j].equals(".") && j + 1 < tokens.length) {
                        String nextSymbol = String.valueOf(tokens[j + 1].charAt(0));
                        if (nonTerminals.contains(nextSymbol)) {
                            List<String> productions = augmentedProductions.get(nextSymbol);
                            for (String production : productions) {
                                String newItem = nextSymbol + " -> . " + production;
                                if (!closure.contains(newItem)) {
                                    closure.add(newItem);
                                    changed = true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return closure;
    }
    /*
     * goto函数
     */
    private Set<String> goTo(Set<String> state, String symbol) {
        Set<String> nextState = new HashSet<>();
        for (String item : state) {
            String tokens = item.replaceAll(" ", "");
            for (int i = 0; i < tokens.length(); i++) {
                if (symbol.length() == 1) {    //转移的符号为a、b之类的单个字符
                    if (tokens.charAt(i) == '.' && i < tokens.length() - 1 && tokens.charAt(i + 1) == symbol.charAt(0)) {
                        String newItem = tokens.charAt(0) + " ->";
                        boolean addSpaceing = true;
                        for (int j = tokens.indexOf(">") + 1; j < tokens.length(); j++) {
                            if (j == i) {
                                newItem += " " + symbol + " .";
                                j++;
                                addSpaceing = true;
                            } else if (j != i + 1) {
                                if (addSpaceing) {
                                    newItem += " " + tokens.charAt(j);
                                    addSpaceing = false;
                                } else
                                    newItem += tokens.charAt(j);
                            }
                        }
                        if (!nextState.contains(newItem)) {
                            nextState.add(newItem);
                        }
                    }
                } else {   //转移符号为多个字符 如 if、else等
                    if (tokens.charAt(i) == '.' && i + symbol.length() < tokens.length()) {
                        String sub = tokens.substring(i + 1, i + symbol.length() + 1);
                        if (sub.equals(symbol)) {
                            String newItem = tokens.charAt(0) + " ->";
                            boolean addSpacking = true;   //用于记录
                            for (int j = tokens.indexOf(">") + 1; j < tokens.length(); j++) {
                                if (j == i) {
                                    newItem += " " + symbol + " . ";
                                    j += symbol.length();
                                } else if (j != i + 1) {
                                    newItem += tokens.charAt(j);
                                }
                            }
                            if (!nextState.contains(newItem)) {
                                nextState.add(newItem);
                            }
                        }
                    }
                }
            }
        }
        return computeClosure(nextState);
    }
    /*
     * 输出LR(0)自动机
     */
    public List<String> printLR0Automaton() {
        List<String> s = new ArrayList<>();
        for (int i = 0; i < states.size(); i++) {
            String temp1 = "状态" + i;
            String temp2 = "";
            s.add(temp1);
            for (String item : stateItems.get(i)) {
                temp2 += "\t" + item;
                s.add(temp2);
                temp2 = "";
            }
            for (Map.Entry<String, Integer> entry : dfaGraph.get(i).entrySet()) {
                temp2 = "\t 输入" + entry.getKey() + " -> 状态" + entry.getValue();
                s.add(temp2);
                temp2 = "";
            }
        }
        return s;
    }
    /*
     * 输出LR(0)分析表
     */
    public String[][] printLR0ParsingTable() {
        String[][] table = new String[states.size()][terminals.size() + nonTerminals.size()+1];
        for (int i = 0; i < states.size(); i++) {
            int j = 0;
            table[i][j++] = String.valueOf(i);
            Map<String, String> actionRow = actionTable.get(i);
            if (actionRow != null) {
                for (String terminal : terminals) {
                    String action = actionRow.get(terminal);
                    if (action == null) {
                        table[i][j++] = " ";
                    } else {
                        table[i][j++] = action;
                    }
                }
                String action = actionRow.get("#");
                if (action == null) {
                    table[i][j++] = " ";
                } else {
                    table[i][j++] = action;
                }
            } else {
                for (int k = 0; k < terminals.size() + 1; k++) {
                    table[i][k] = " ";
                }
            }
            Map<String, Integer> gotoRow = gotoTable.get(i);
            if (gotoRow != null) {
                for (String nonTerminal : nonTerminals) {
                    if (!nonTerminal.equals(augmentedStart)) {
                        Integer nextStateId = gotoRow.get(nonTerminal);
                        if (nextStateId == null) {
                            table[i][j++] = " ";
                        } else {
                            table[i][j++] = String.valueOf(nextStateId);
                        }
                    }
                }
            } else {
                for (int k = 0; k < nonTerminals.size()-1; k++) { //nonTerminals。size() -1 是因为不输出拓广文法开始符（如S'）
                    table[i][k] = " ";
                }
            }
        }
        return table;
    }
    /*
     * 对输入串进行LR(0)分析
     */
    public boolean stringLR0ParserAnalysis(String str) {
        String input = str + "#";
        stateStack = new Stack<>();         //状态栈初始化
        characterStack = new Stack<>();     //符号栈初始化
        stateStack.push(Integer.valueOf(0));
        characterStack.push("#");
        analysisProcess = new LinkedHashMap<>(); //分析过程数据结构初始化
        numRow = 0 ; //分析过程表 的行数 初始化
        int index = 0;                      //当前扫描的输入串的字符位置
        isAcc =false;                       //表明未分析成功
        //对输入串的每一个字符进行判断
        while (index < input.length()) {
            String symbol = String.valueOf(input.charAt(index));
            String judge = new String();
            if (symbol.equals("#") || terminals.contains(symbol)) { //当前字符属于终结符
                judge = actionTable.get(stateStack.peek()).get(symbol);
            } else if (nonTerminals.contains(symbol)) {              // 当前字符属于非终结符
                judge = String.valueOf(gotoTable.get(stateStack.peek()).get(symbol));
            } else {    //当前字符可能是终结符的一部分，也可能不是非终结符(说明出现未能识别的字符)
                boolean isTerminals = false;
                for (String s : terminals) {     //特殊的终结符（多字母），如int 、double
                    if (index + s.length() < input.length() && s.equals(input.substring(index, index + s.length() + 1).replaceAll(" ", ""))) {
                        judge = actionTable.get(stateStack.peek()).get(s);
                        symbol = s;
                        isTerminals = true;
                        break;
                    }
                }
                if (!isTerminals) {
                    //输出分析过程，下同
                    Stack<Integer> s1 = (Stack<Integer>) stateStack.clone();  //克隆栈，避免方法传值的深拷贝问题，即方法produceAnalysisTable中修改传入的s1(s2)会同时修改方法外的stateStack(characterStack)
                    Stack<String> s2 = (Stack<String>) characterStack.clone();
                    String s3 = "Action[" + stateStack.peek() + "," + input.charAt(index) + "] = null, 出现错误";
                    produceAnalysisTable(s1, s2, input, s3, index);
                    break;
                }
            }

            if (judge == null || judge.equals(" ")) {
                Stack<Integer> s1 = (Stack<Integer>) stateStack.clone();
                Stack<String> s2 = (Stack<String>) characterStack.clone();
                String s3 = "Action[" + stateStack.peek() + "," + input.charAt(index) + "] = null, 出现错误";
                produceAnalysisTable(s1, s2, input, s3, index);
                break;
            }
            if (judge.equals("acc")) {
                Stack<Integer> s1 = (Stack<Integer>) stateStack.clone();
                Stack<String> s2 = (Stack<String>) characterStack.clone();
                String s3 = "acc:分析成功";
                isAcc = true;
                produceAnalysisTable(s1, s2, input, s3, index);
                break;
            }
            switch (judge.charAt(0)) {
                //移进操作
                case 'S': {
                    Stack<Integer> s1 = (Stack<Integer>) stateStack.clone();
                    Stack<String> s2 = (Stack<String>) characterStack.clone();
                    String s3 = "Action[" + stateStack.peek() + "," + input.charAt(index) + "] = " + judge + ", 即状态" + judge.charAt(1) + "入栈";
                    produceAnalysisTable(s1, s2, input, s3, index);

                    stateStack.push(Integer.valueOf(Integer.parseInt(judge.substring(1, 2)))); //如 S2中的2
                    characterStack.push(symbol);

                    //将指针后移，跳过空格
                    while (input.charAt(index) == ' ')
                        index++;
                    index += symbol.length();
                    break;
                }
                //规约操作
                case 'r': {
                    for (String s : productionIds.keySet()) {   //遍历产生式集，寻找出规约的产生式
                        if (productionIds.get(s) == Integer.parseInt(judge.substring(1, 2))) {
                            s = s.replaceAll(" ", "");
                            String[] tokens = s.split("->");
                            int i = 0;
                            Stack<Integer> s1 = (Stack<Integer>) stateStack.clone();
                            Stack<String> s2 = (Stack<String>) characterStack.clone();
                            while (i < tokens[1].length()) {
                                if (characterStack.peek().length() != 1) { //当符号栈的栈顶元素为多字符的终结符，如int
                                    if (characterStack.peek().equals(tokens[1].substring(0, characterStack.peek().length()))) {
                                        i += characterStack.peek().length();
                                        characterStack.pop();
                                        stateStack.pop();
                                    }
                                } else {
                                    characterStack.pop();
                                    stateStack.pop();
                                    i++;
                                }
                            }
                            characterStack.push(tokens[0]); //将产生式的右部移除后，移入左部
                            if (gotoTable.get(stateStack.peek()).get(characterStack.peek()) != -1) {    //goto表中中有选项时
                                String s3 = "r" + judge.charAt(1) + ":用" + s + "规约且GOTO(" + stateStack.peek() + "," + characterStack.peek() + ")=" + gotoTable.get(stateStack.peek()).get(characterStack.peek()) + "入栈";
                                produceAnalysisTable(s1, s2, input, s3, index);
                                stateStack.push(gotoTable.get(stateStack.peek()).get(characterStack.peek()));//将对应的goto表的状态入栈状态栈
                            }
                        }
                    }
                    break;
                }
            }
        }
        return isAcc;
    }
    /*
     *   初始化输入串的LR(0)分析过程表的表头
     */
    public void initAnalysisProcessTable() {
        //构建LR(0)分析过程表的表头
        List<String> title = new ArrayList<>();
        title.add("步骤   ");
        title.add("状态栈 ");
        title.add("符号栈 ");
        title.add("输入串 ");
        title.add("动作说明");
        analysisProcess.put(numRow++, title);   // 0为分析步骤编号
    }
    /*
     *   生成输入串LR(0)分析过程表的内容
     */
    public void produceAnalysisTable(Stack<Integer> s1, Stack<String> s2, String s3, String s4, int index) {

        String state = "";                  //存储分析过程表中每一行的状态栈
        String character = "";              //存储分析过程表中每一行的符号栈
        String inputString = "";            //存储分析过程表中每一行的输入串
        String instruction = "";            //存储分析过程表中每一行的动作说明
        String temp = "";                   //临时处理串

        while (s1.size() != 0) {  //将状态栈的栈顶元素出栈并存入temp中
            temp += s1.pop();
        }
        for (int i = temp.length() - 1; i >= 0; i--) {  //逆序将temp的字符存入state中
            state += temp.charAt(i);
        }
        temp = "";

        while (s2.size() != 0) {  //将符号栈的栈顶元素出栈并存入temp中
            temp += s2.pop();
        }
        for (int i = temp.length() - 1; i >= 0; i--) {  //逆序将temp的字符存入character中
            character += temp.charAt(i);
        }
        temp = "";

        for (int i = index; i < s3.length(); i++) {  //以当前指针所指的字符为起始，将输入串存入inputString
            inputString += s3.charAt(i);
        }

        instruction = s4; //“动作说明”
        List<String> row = new ArrayList<>();
        row.add(String.valueOf(numRow));
        row.add(state);
        row.add(character);
        row.add(inputString);
        row.add(instruction);
        analysisProcess.put(numRow++, row);  //存入分析过程表格的一行
    }
    /*
     *  输出输入串的LR(0)分析过程
     */
    public void outputAnalysisProcess() {
        System.out.println("以下为输入串LR(0)分析过程表");
        for (int i = 0; i < analysisProcess.size(); i++) {
            for (String s : analysisProcess.get(i)) {
                if (i == 0) {  //第一行的表头
                    System.out.printf("%-8s", s);
                } else {
                    System.out.printf("%-10s", s);
                }
            }
            System.out.println();
        }
        System.out.println("");
        if (isAcc) {
            System.out.println("------------------------------------");
            System.out.println("----- 该输入串是符合给定文法的句子！-----");
            System.out.println("------------------------------------");
        } else {
            System.out.println("------------------------------------");
            System.out.println("----- 该输入串不是符合给定文法的句子！-----");
            System.out.println("------------------------------------");
        }
    }
}

