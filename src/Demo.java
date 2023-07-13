import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.swing.JOptionPane;

public class Demo extends JFrame {
    private JButton btnOK1; //确认按钮
    private JButton btnOK2;
    private JTextArea inputGrammar; //输入文法
    private JTextArea inputString;  //输入分析串
    private JTextArea instrution;  //介绍文本框
    private JTextArea outputExtendedGrammar; //输出拓广文法
    private JTextArea outputLR0Automaton;  //输入LR0自动机
    private JTable outputLR0Table;  //输出LR0分析表
    private JTable outputLR0AnalysisProcess; //输出对分析串的LR0分析过程
    private GridBagLayout gbl;  //网格包布局
    private GridBagConstraints gbc;
    private DefaultTableModel tableModel1;
    private DefaultTableModel tableModel2;

    private JPanel basicPanel; //底层面板
    private JPanel leftJpanel; //左部面板
    private JPanel rightJpanel; //右部面板
    private JPanel midJpanel; //中部面板

    //存储LR(0)分析器的相关数据的成员变量
    private LR0Parser lr0Parser;
    private Set<String> terminals;
    private Set<String> nonTerminals;
    private Map<String, java.util.List<String>> augmentedProductions;
    private String augmentedStart;
    private Map<Integer, List<String>> analysisProcess;
    private List<Set<String>> states;
    private Font font_small, font_Large;

    //初始化界面
    public Demo(int witdh, int height) {
        super("LR(0)语法分析器 v1.0.0");
        this.setSize(witdh, height);
        setLocationRelativeTo(null);
        init();
    }
    public JScrollPane addJTextArea(JTextArea j) {  //给JTextArea添加滚动条
        JScrollPane scrollPane = new JScrollPane(j);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }
    public JScrollPane addJTable(JTable j) { //给JTable添加滚动条
        JScrollPane scrollPane = new JScrollPane(j);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        return scrollPane;
    }
    public void setSmallFont(Component ...components){
        font_small = new Font("宋体",Font.BOLD,14);
        for(Component c :components){
            c.setFont(font_small);
        }
    }
    public void setLargeFont(Component ...components){
        font_Large = new Font("宋体",Font.BOLD,16);
        for(Component c : components){
            c.setFont(font_Large);
        }
    }
    //设计界面
    public void init() {

        //初始化按钮、输入文本框和输出文本框
        btnOK1 = new JButton("确定输入");
        btnOK2 = new JButton("开始分析");

        inputGrammar = new JTextArea();
        inputString = new JTextArea();
        outputExtendedGrammar = new JTextArea();
        outputLR0Automaton = new JTextArea();
        instrution = new JTextArea();


        leftJpanel = new JPanel();
        BoxLayout left = new BoxLayout(leftJpanel, BoxLayout.Y_AXIS);
        leftJpanel.setVisible(true);
        leftJpanel.setLayout(left);

        leftJpanel.add(new JLabel("LR0语法分析器介绍及有关信息"));
        instrution.setText("1、程序作用：\n\t基于LR(0)的语法分析器\n2、操作说明：\n 输入：\n\t文法——仅限于大小写英文字母，\n\t大写英文字母代表非终结符，\n\t小写英文字母代表终结符输入\n\t分析串——与文法要求一致" +
                " \n 输出：\n\t终结符集、非终结符集、拓广文法、\n\tLR(0)状态集、LR(0)分析表\n 其它：\n\t1）点击“输入文法”按钮会清空右侧的\n\t上一次的输出数据，重新读入新文法" +
                "\n\t2）点击“开始分析”按钮进行语法分析\n3、设计者：\n  姓名：吴圳城\n  班级：软件212 \n  学号：32106300044\n  学院：计算机科学与网络工程学院");
        instrution.setVisible(true);
        instrution.setEditable(false);
        instrution.setRows(15);
        instrution.setColumns(50);
        leftJpanel.add(addJTextArea(instrution));
        leftJpanel.add(new JLabel("输入文法："));
        leftJpanel.add(addJTextArea(inputGrammar));
        leftJpanel.add(btnOK1);
        leftJpanel.add(new JLabel("输入分析串："));
        leftJpanel.add(addJTextArea(inputString));
        leftJpanel.add(btnOK2);


        midJpanel = new JPanel();
        BoxLayout mid = new BoxLayout(midJpanel, BoxLayout.Y_AXIS);
        midJpanel.setVisible(true);
        midJpanel.setLayout(mid);

        midJpanel.add(new JLabel("拓广文法及终结符、非终结符："));
        midJpanel.add(addJTextArea(outputExtendedGrammar));
        midJpanel.add(new JLabel("LR(0)项目集："));
        midJpanel.add(addJTextArea(outputLR0Automaton));


        rightJpanel = new JPanel();
        BoxLayout right = new BoxLayout(rightJpanel, BoxLayout.Y_AXIS);
        rightJpanel.setVisible(true);
        rightJpanel.setLayout(right);

        rightJpanel.add(new JLabel("LR(0)分析表："));
        outputLR0Table = new JTable(7, 6);
        rightJpanel.add(addJTable(outputLR0Table));

        rightJpanel.add(new JLabel("LR(0)分析过程"));
        outputLR0AnalysisProcess = new JTable(7,5);
        rightJpanel.add(addJTable(outputLR0AnalysisProcess));


        gbl = new GridBagLayout();
        gbc = new GridBagConstraints();
        basicPanel = new JPanel(gbl);


        //左侧组件
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 5;  //左侧宽度权重
        gbc.weighty = 15; //左侧高度权重
        gbc.gridx = 0;    //第0列
        gbc.ipadx = 50;   //左右内边距
        basicPanel.add(leftJpanel, gbc);

        //中间组件
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 5; //中间宽度权重
        gbc.weighty = 15; //中间高度权重
        gbc.gridx = 1; //第1列
        gbc.ipadx = 50; //左右内边距
        basicPanel.add(midJpanel, gbc);

        //右侧组件
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 50;  //右侧宽度权重
        gbc.weighty = 15; //右侧高度权重
        gbc.gridx = 2;    //第2列
        gbc.ipadx = 50;   //左右内边距
        basicPanel.add(rightJpanel, gbc);

        //设置字体
        setSmallFont(inputGrammar,inputString,outputExtendedGrammar,outputLR0Automaton,outputLR0Table,outputLR0AnalysisProcess);
        setLargeFont(btnOK1,btnOK2);

        this.add(basicPanel);
        this.setVisible(true);

        btnOK1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = inputGrammar.getText();
                if(s.isEmpty()) {
                    JOptionPane.showMessageDialog(null, "未输入文法！", "提示", JOptionPane.INFORMATION_MESSAGE);
                    outputExtendedGrammar.setText("");
                    outputLR0Automaton.setText("");
                    DefaultTableModel nullModel = new DefaultTableModel();
                    outputLR0Table.setModel(nullModel);
                    tableModel1.setRowCount(0);
                    outputLR0Table.repaint();
                }else {
                    outputExtendedGrammar.setText("");
                    outputLR0Automaton.setText("");

                    lr0Parser = new LR0Parser();
                    lr0Parser.readGrammarProductions(s);  //读入文法
                    lr0Parser.augmentedProductions();     //拓广文法

                    terminals = lr0Parser.getTerminals();
                    nonTerminals = lr0Parser.getNonTerminals();
                    augmentedProductions = lr0Parser.getAugmentedProductions();
                    augmentedStart = lr0Parser.getAugmentedStart();
                    //在outputExendedGrammar中输出有关信息
                    outputExtendedGrammar.append("终结符：\n");
                    for (String str : terminals) {
                        outputExtendedGrammar.append(str + " ");
                    }
                    outputExtendedGrammar.append("\n非终结符：\n");
                    for (String str : nonTerminals) {
                        outputExtendedGrammar.append(str + " ");
                    }
                    outputExtendedGrammar.append("\n拓广文法：\n");
                    for (String str : augmentedProductions.keySet()) {
                        for (String temp : augmentedProductions.get(str)) {
                            outputExtendedGrammar.append(str + " -> " + temp + "\n");
                        }
                    }
                    outputExtendedGrammar.setEditable(false); //设置为不可修改


                    //构建LR(0)自动机及LR(0)分析表
                    lr0Parser.buildLR0Automaton();
                    //在outputLR0Automaton中输出LR(0)自动机
                    for (String str : lr0Parser.printLR0Automaton()) {
                        outputLR0Automaton.append(str + "\n");
                    }
                    outputLR0Automaton.setEditable(false); //设置为不可修改

                    //在outputLR0ParseringTable中输出LR(0)分析表
                    analysisProcess = lr0Parser.getAnalysisProcess();
                    states = lr0Parser.getStates();
                    int num = terminals.size() + nonTerminals.size(); //加入了“#”，剔除了 S'
                    int i = 0;
                    String[] columnNames = new String[num + 1];
                    String[][] table = new String[states.size()][num];
                    columnNames[i++] = "状态集\\符号";
                    for (String str : terminals) {
                        columnNames[i++] = str;
                    }
                    columnNames[i++] = "#";
                    for (String str : nonTerminals) {
                        if (!str.equals(augmentedStart)) {
                            columnNames[i++] = str;
                        }
                    }
                    tableModel1 = (DefaultTableModel) outputLR0Table.getModel();
                    tableModel1.setRowCount(states.size());
                    tableModel1.setColumnCount(num + 1);
                    tableModel1.setColumnIdentifiers(columnNames);
                    table = lr0Parser.printLR0ParsingTable();
                    for (int row = 0; row < tableModel1.getRowCount(); row++) {
                        for (int col = 0; col < tableModel1.getColumnCount(); col++) {
                            tableModel1.setValueAt(table[row][col], row, col);
                        }
                    }
                    outputLR0Table.repaint();

                    outputLR0Table.setDefaultEditor(Object.class, null); //设置JTable即LR(0)分析表不能修改
                    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                    centerRenderer.setHorizontalAlignment(JLabel.CENTER);  // 设置显示居中
                    outputLR0Table.setDefaultRenderer(Object.class, centerRenderer);
                }
            }
        });

        btnOK2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String s = inputString.getText();
                if (lr0Parser == null) { //判断文法是否已输入
                    JOptionPane.showMessageDialog(null, "未输入文法！", "错误提示", JOptionPane.INFORMATION_MESSAGE);
                    tableModel2.setRowCount(0); //清空
                } else if (s.isEmpty()) { //判断要分析的字符串是否已输入
                    JOptionPane.showMessageDialog(null, "未输入分析串！", "错误提示", JOptionPane.INFORMATION_MESSAGE);
                    tableModel2.setRowCount(0); //清空
                } else {
                    boolean isAcc = lr0Parser.stringLR0ParserAnalysis(s);
                    analysisProcess = lr0Parser.getAnalysisProcess();
                    tableModel2 = (DefaultTableModel) outputLR0AnalysisProcess.getModel();
                    tableModel2.setRowCount(0); //清空
                    String[] columnNames = {"步骤", "状态栈", "符号栈", "输入串", "动作说明"};
                    tableModel2.setColumnIdentifiers(columnNames);
                    tableModel2.setRowCount(analysisProcess.size());
                    tableModel2.setColumnCount(columnNames.length);
                    tableModel2.setValueAt(String.valueOf(analysisProcess),0,0);
                    for (int i = 0; i < analysisProcess.size(); i++) {
                        int j = 0;
                        for (String str : analysisProcess.get(i)) {
                            tableModel2.setValueAt(str, i, j);
                            j++;
                        }
                    }

                    //设置outputLR0AnalysisProcess的列的宽度不同
                    for (int k=0;k<outputLR0AnalysisProcess.getColumnCount();k++){
                        outputLR0AnalysisProcess.getColumnModel().getColumn(k).setPreferredWidth(80);
                        if(k==outputLR0AnalysisProcess.getColumnCount()-1)
                             outputLR0AnalysisProcess.getColumnModel().getColumn(k).setPreferredWidth(320);
                    }

                    outputLR0AnalysisProcess.repaint();
                    outputLR0AnalysisProcess.setDefaultEditor(Object.class, null); //设置JTable即LR(0)分析表不能修改
                    DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
                    centerRenderer.setHorizontalAlignment(JLabel.CENTER);  // 设置显示居中
                    outputLR0AnalysisProcess.setDefaultRenderer(Object.class, centerRenderer);

                    if (isAcc) {
                        JOptionPane.showMessageDialog(null, "分析成功！\n输入串是给定文法的句子", "提示", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(null, "分析失败！\n输入串不是给定文法的句子", "提示", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }
        });
    }

}
