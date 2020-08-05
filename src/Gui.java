/*
 * 将 MineSweeper 类呈现在 GUI 上.
 * 由于 GUI 不是很重要, 所以一开始没有重视, 写的比较随意, 但涉及的逻辑却随时间越来越复杂, 于是乎这坨代码逐渐形成了一座屎山.
 * 尤其是 负责展示棋盘的内部类 BoardCanvas, 一直想重构但是涉及的逻辑比较烦, 且远没有三个 Sweeper 类重要, 就一直没改.
 * 于是连注释都不想写了, 估计半年后包括我在内没有人看得懂了.
 */
import static java.awt.event.InputEvent.*;
import static java.awt.event.KeyEvent.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Gui extends JFrame {

    private static final String FACE_NORMAL = "\uD83D\uDE42";
    private static final String FACE_PRESS = "\uD83D\uDE2E";
    private static final String FACE_WIN = "\uD83D\uDE0E";
    private static final String FACE_LOSE = "\uD83D\uDE2B";
    private static final String FACE_RESTART = "\uD83D\uDE32";

    private static final int INFO_HEIGHT = 55;

    private int row, col, mineCount, gameRule;
    private boolean cheat, showMine;
    private MineSweeper game;

    private int cellLength;
    private BoardCanvas canvas;
    private TimeThread timeThread;
    private JMenuBar menuBar;
    private JLabel mineLabel;
    private JLabel timeLabel;
    private CellCanvas faceCanvas, mineLabelCanvas, timeLabelCanvas,
            boardBorderCanvas, infoBorderCanvas;

    private String lastMineBoardDirectory = ".";

    public Gui() {
        this.setTitle("Minesweeper");
        this.setLayout(new BorderLayout());
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(null);
        this.getContentPane().setBackground(new Color(192, 192, 192));
        this.cellLength = 30;
        this.showMine = false;
        this.initMenu();
        this.cheat = false;
        this.gameRule = MineSweeper.GAME_RULE_WIN_XP;
        this.initGame(MineSweeper.DIFFICULTY_BEGINNER);
        this.setVisible(true);
    }

    private void initMenu() {
        menuBar = new JMenuBar();
        JMenu gameMenu  = new JMenu("游戏");
        JMenu cheatMenu = new JMenu("作弊");
        JMenu aiMenu    = new JMenu("AI");
        JMenu aboutMenu = new JMenu("关于");

        JMenuItem         newGameMenuItem       = new JMenuItem("开局");
        JCheckBoxMenuItem beginnerMenuItem      = new JCheckBoxMenuItem("初级", true);
        JCheckBoxMenuItem intermediateMenuItem  = new JCheckBoxMenuItem("中级", false);
        JCheckBoxMenuItem advancedMenuItem      = new JCheckBoxMenuItem("高级", false);
        JCheckBoxMenuItem customMenuItem        = new JCheckBoxMenuItem("自定义", false);
        JCheckBoxMenuItem xpMenuItem            = new JCheckBoxMenuItem("套娃扫雷", false);
        JCheckBoxMenuItem gameRuleWinXpMenuItem = new JCheckBoxMenuItem("规则 Win XP", true);
        JCheckBoxMenuItem gameRuleWin7MenuItem  = new JCheckBoxMenuItem("规则 Win 7", false);
        JCheckBoxMenuItem allowQuestionMenuItem = new JCheckBoxMenuItem("问号标记", MineSweeper.getAllowQuestionMark());
        JMenuItem         cellLengthMenuItem    = new JMenuItem("格子大小");
        gameMenu.add(newGameMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(beginnerMenuItem);
        gameMenu.add(intermediateMenuItem);
        gameMenu.add(advancedMenuItem);
        gameMenu.add(customMenuItem);
        gameMenu.add(xpMenuItem);
        gameMenu.addSeparator();
        gameMenu.add(gameRuleWinXpMenuItem);
        gameMenu.add(gameRuleWin7MenuItem);
        gameMenu.addSeparator();
        gameMenu.add(allowQuestionMenuItem);
        gameMenu.add(cellLengthMenuItem);

        JCheckBoxMenuItem cheatMenuItem         = new JCheckBoxMenuItem("启用作弊");
        JMenuItem         undoMenuItem          = new JMenuItem("撤销操作");
        JMenuItem         mineMenuItem          = new JMenuItem("启用透视");
        JMenuItem         customMineMenuItem    = new JMenuItem("导入棋盘");
        cheatMenuItem.setText((cheat? "关闭" : "启用") + "作弊");
        undoMenuItem.setEnabled(cheat);
        mineMenuItem.setEnabled(cheat);
        customMineMenuItem.setEnabled(cheat);
        cheatMenu.add(cheatMenuItem);
        cheatMenu.addSeparator();
        cheatMenu.add(undoMenuItem);
        cheatMenu.add(mineMenuItem);
        cheatMenu.add(customMineMenuItem);


        JMenuItem checkBasicMenuItem = new JMenuItem("提示一格（快）");
        JMenuItem sweepBasicMenuItem = new JMenuItem("自动清扫（快）");
        JMenuItem sweepAdvancedMenuItem = new JMenuItem("自动清扫（慢）");
        JMenuItem sweepToEndMenuItem = new JMenuItem("扫到结束（慢）");
        JMenuItem aiDebugMenuItem = new JMenuItem("显示概率（慢）");
        aiMenu.add(checkBasicMenuItem);
        aiMenu.add(sweepBasicMenuItem);
        aiMenu.add(sweepAdvancedMenuItem);
        aiMenu.add(sweepToEndMenuItem);
        aiMenu.add(aiDebugMenuItem);

        JMenuItem versionMenuItem         = new JMenuItem("版本: " + Main.VERSION);
        JMenuItem authorMenuItem         = new JMenuItem("作者: 蟹恼板");
        aboutMenu.add(versionMenuItem);
        aboutMenu.add(authorMenuItem);

        menuBar.add(gameMenu);
        menuBar.add(cheatMenu);
        menuBar.add(aiMenu);
        menuBar.add(aboutMenu);

        newGameMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_F2, 0));
        newGameMenuItem.addActionListener(e -> {
            if (game.getGameRule() == WinXpSweeper.GAME_RULE_REAL_WIN_XP) {
                try { initGame(new WinXpSweeper(true)); }
                catch (WinXpSweeper.WindowOccludedException ex) {
                    JOptionPane.showMessageDialog(faceCanvas, ex.toString(), "winmine.exe 未启动或被遮挡", JOptionPane.ERROR_MESSAGE);
                }
            }
            else initGame();
        });
        beginnerMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_1, CTRL_MASK));
        beginnerMenuItem.addActionListener(e -> {
            beginnerMenuItem.setSelected(true);
            intermediateMenuItem.setSelected(false);
            advancedMenuItem.setSelected(false);
            customMenuItem.setSelected(false);
            xpMenuItem.setSelected(false);
            initGame(MineSweeper.DIFFICULTY_BEGINNER);
        });
        intermediateMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_2, CTRL_MASK));
        intermediateMenuItem.addActionListener(e -> {
            beginnerMenuItem.setSelected(false);
            intermediateMenuItem.setSelected(true);
            advancedMenuItem.setSelected(false);
            customMenuItem.setSelected(false);
            xpMenuItem.setSelected(false);
            initGame(MineSweeper.DIFFICULTY_INTERMEDIATE);
        });
        advancedMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_3, CTRL_MASK));
        advancedMenuItem.addActionListener(e -> {
            beginnerMenuItem.setSelected(false);
            intermediateMenuItem.setSelected(false);
            advancedMenuItem.setSelected(true);
            customMenuItem.setSelected(false);
            xpMenuItem.setSelected(false);
            initGame(MineSweeper.DIFFICULTY_EXPERT);
        });
        customMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_4, CTRL_MASK));
        customMenuItem.addActionListener(e -> {
            customMenuItem.setSelected(!customMenuItem.isSelected());
            try {
                String s = row + " " + col + " " + mineCount;
                String[] arr = JOptionPane.showInputDialog(faceCanvas, "自定义棋盘(行 列 雷)", s).split(" ");
                int r = Integer.parseInt(arr[0]);
                int c = Integer.parseInt(arr[1]);
                int m = Integer.parseInt(arr[2]);
                if (r < 1 || c < 1 || m < 0 || m > r * c - (gameRule == MineSweeper.GAME_RULE_WIN_XP ? 1 : 9)) {
                    throw new Exception("数字范围错误，棋盘上容纳不下这么多雷。");
                }
                beginnerMenuItem.setSelected(false);
                intermediateMenuItem.setSelected(false);
                advancedMenuItem.setSelected(false);
                customMenuItem.setSelected(true);
                xpMenuItem.setSelected(false);
                initGame(r, c, m);
            }
            catch (NullPointerException ignored) {
            }
            catch (Exception ex) {
                JOptionPane.showMessageDialog(faceCanvas, ex.toString(), "格式或数字有误", JOptionPane.ERROR_MESSAGE);
            }
        });
        xpMenuItem.addActionListener(e -> {
            xpMenuItem.setSelected(!xpMenuItem.isSelected());
            try {
                MineSweeper game = new WinXpSweeper();
                beginnerMenuItem.setSelected(false);
                intermediateMenuItem.setSelected(false);
                advancedMenuItem.setSelected(false);
                customMenuItem.setSelected(false);
                xpMenuItem.setSelected(true);
                initGame(game);
                setFrameAfterOperation();
            }
            catch (WinXpSweeper.WindowOccludedException ex) {
                JOptionPane.showMessageDialog(faceCanvas, ex.toString(), "winmine.exe 未启动或被遮挡", JOptionPane.ERROR_MESSAGE);
            }
        });
        gameRuleWinXpMenuItem.addActionListener(e -> {
            gameRule = MineSweeper.GAME_RULE_WIN_XP;
            gameRuleWinXpMenuItem.setSelected(true);
            gameRuleWin7MenuItem.setSelected(false);
            if (game.getStep() == 0) initGame();
        });
        gameRuleWin7MenuItem.addActionListener(e -> {
            gameRuleWin7MenuItem.setSelected(!gameRuleWin7MenuItem.isSelected());
            if (mineCount > row * col - 9) {
                JOptionPane.showMessageDialog(faceCanvas, "Win7 规则下当前自定义棋盘上的雷最多为 " + (row * col - 9) + "。",
                        "数字有误", JOptionPane.ERROR_MESSAGE);
                return;
            }
            gameRule = MineSweeper.GAME_RULE_WIN_7;
            gameRuleWinXpMenuItem.setSelected(false);
            gameRuleWin7MenuItem.setSelected(true);
            if (game.getStep() == 0) initGame();
        });
        allowQuestionMenuItem.addActionListener(e -> MineSweeper.setAllowQuestionMark(allowQuestionMenuItem.isSelected()));
        cellLengthMenuItem.addActionListener(e -> {
            try {
                cellLength = Integer.parseInt(JOptionPane.showInputDialog(faceCanvas, "格子大小设置", String.valueOf(cellLength)));
                setFrame();
                canvas.requestRepaintAll(false);
            }
            catch (NumberFormatException ignored) {}
            catch (Exception ex) {
                JOptionPane.showMessageDialog(faceCanvas, "格式错误，请输入合法的整数。", "错误",JOptionPane.ERROR_MESSAGE);
            }
        });

        cheatMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_J, CTRL_MASK));
        cheatMenuItem.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(faceCanvas, "开启/关闭作弊会新开一局，确认继续吗？", "作弊",JOptionPane.YES_NO_OPTION);
            if (res != 0) return;
            cheat = cheatMenuItem.isSelected();
            if (!cheat) showMine = false;
            undoMenuItem.setEnabled(cheat);
            mineMenuItem.setEnabled(cheat);
            customMineMenuItem.setEnabled(cheat);
            initGame();
        });
        undoMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_Z, CTRL_MASK));
        undoMenuItem.addActionListener(e -> { game.undo(); setFrameAfterOperation(); });
        mineMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_M, CTRL_MASK));
        mineMenuItem.addActionListener(e -> {
            if (!cheat) showMine = false;
            else {
                showMine = !showMine;
                mineMenuItem.setText((showMine? "关闭" : "启用") + "透视");
                game.setShowMine(showMine);
                canvas.repaint();
            }
        });
        customMineMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_N, CTRL_MASK));
        customMineMenuItem.addActionListener(e -> {
            JFileChooser jfc = new JFileChooser(lastMineBoardDirectory);
            jfc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int openState = jfc.showDialog(faceCanvas, "选择");
            if (openState == JFileChooser.CANCEL_OPTION) return;
            File file = jfc.getSelectedFile();
            lastMineBoardDirectory = file.getParent();
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                List<String> lineList = new ArrayList<>();
                String line = bufferedReader.readLine();
                while (line != null && !line.equals("")) {
                    lineList.add(line.replaceAll(" ", ""));
                    line = bufferedReader.readLine();
                }
                bufferedReader.close();
                fileReader.close();
                boolean[][] mineBoard = new boolean[lineList.size()][lineList.get(0).length()];
                for (int i = 0; i < lineList.size(); ++i) {
                    line = lineList.get(i);
                    for (int j = 0; j < lineList.get(0).length(); ++j) {
                        mineBoard[i][j] = line.charAt(j) == '*';
                    }
                }
                beginnerMenuItem.setSelected(false);
                intermediateMenuItem.setSelected(false);
                advancedMenuItem.setSelected(false);
                customMenuItem.setSelected(true);
                initGame(new MineSweeper(mineBoard));
            } catch (Exception exception) {
                JOptionPane.showMessageDialog(faceCanvas, exception.getMessage(),
                        "文件读取错误", JOptionPane.ERROR_MESSAGE);
            }
        });

        checkBasicMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_Q, CTRL_MASK));
        checkBasicMenuItem.addActionListener(e -> new Thread(() -> {
            int[] res = AutoSweeper.checkAllBasic(game);
            if (res[0] == AutoSweeper.UNKNOWN) return;
            int[][] arr1 = new int[][]{{res[0]}, {res[1], res[2]}};
            int[][] arr0 = new int[][]{{0}, {res[1], res[2]}};
            try {
                for (int i = 0; i < 3; ++ i) {
                    canvas.highlight(arr1); Thread.sleep(150);
                    canvas.highlight(arr0); Thread.sleep(100);
                }
                canvas.highlight(null);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }).start());

        sweepBasicMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_W, CTRL_MASK));
        sweepBasicMenuItem.addActionListener(e -> new Thread(() -> {
            canvas.doNotUpdateTheFuckingCanvasNow(true);
            AutoSweeper.sweepAllBasic(game);
            canvas.doNotUpdateTheFuckingCanvasNow(false);
            setFrameAfterOperation();
        }).start());

        sweepAdvancedMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_E, CTRL_MASK));
        sweepAdvancedMenuItem.addActionListener(e -> new Thread(() -> {
            canvas.doNotUpdateTheFuckingCanvasNow(true);
            AutoSweeper.sweepAllAdvanced(game);
            canvas.doNotUpdateTheFuckingCanvasNow(false);
            setFrameAfterOperation();
        }).start());

        sweepToEndMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_R, CTRL_MASK));
        sweepToEndMenuItem.addActionListener(e -> new Thread(() -> {
            canvas.doNotUpdateTheFuckingCanvasNow(true);
            AutoSweeper.sweepToEnd(game);
            canvas.doNotUpdateTheFuckingCanvasNow(false);
            setFrameAfterOperation();
        }).start());

        aiDebugMenuItem.setAccelerator(KeyStroke.getKeyStroke(VK_T, CTRL_MASK));
        aiDebugMenuItem.addActionListener(e -> new Thread(() -> {
            int[][] cc = AutoSweeper.findAllConnectedComponents(game).getValue();
            double[][] prob = AutoSweeper.calculateAllProbabilities(game);
            canvas.setConnectedComponentsAndProbability(cc, prob);
        }).start());

        versionMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(java.net.URI.create("https://github.com/XieNaoban/Minesweeper"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(faceCanvas, "浏览器打开失败，请手动访问 https://github.com/XieNaoban/Minesweeper 。", "错误",JOptionPane.ERROR_MESSAGE);
            }
        });
        authorMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(java.net.URI.create("https://github.com/XieNaoban"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(faceCanvas, "浏览器打开失败，请手动访问 https://github.com/XieNaoban 。", "错误",JOptionPane.ERROR_MESSAGE);
            }
        });
        this.setJMenuBar(menuBar);

        Color panelForeColor = new Color(146, 26, 33);
        Color panelBackColor = new Color(20, 0, 0);
        Font panelFont = new Font("Consolas",Font.BOLD, 28);
        Container container = this.getContentPane();

        this.mineLabel = new JLabel("0000", SwingConstants.CENTER);
        this.mineLabel.setVerticalAlignment(SwingConstants.TOP);
        this.mineLabel.setOpaque(true);
        this.mineLabel.setBackground(panelBackColor);
        this.mineLabel.setFont(panelFont);
        this.mineLabel.setForeground(panelForeColor);
        container.add(this.mineLabel);

        this.timeLabel = new JLabel("0000", SwingConstants.CENTER);
        this.timeLabel.setVerticalAlignment(SwingConstants.TOP);
        this.timeLabel.setOpaque(true);
        this.timeLabel.setBackground(panelBackColor);
        this.timeLabel.setFont(panelFont);
        this.timeLabel.setForeground(panelForeColor);
        container.add(this.timeLabel);


        this.faceCanvas = new CellCanvas(4, false);
        this.faceCanvas.enableYellowFace();
        this.faceCanvas.addMouseListener(new MouseListener() {
            @Override public void mousePressed(MouseEvent e) {
                faceCanvas.setReverse(true);
            }
            @Override public void mouseReleased(MouseEvent e) {
                faceCanvas.setReverse(false);
                if (e.getX() < 0 || e.getX() > faceCanvas.getWidth()) return;
                if (e.getY() < 0 || e.getY() > faceCanvas.getHeight()) return;
                if (game.getGameRule() == WinXpSweeper.GAME_RULE_REAL_WIN_XP) {
                    try { initGame(new WinXpSweeper(true)); }
                    catch (WinXpSweeper.WindowOccludedException ex) {
                        JOptionPane.showMessageDialog(faceCanvas, ex.toString(), "winmine.exe 未启动或被遮挡", JOptionPane.ERROR_MESSAGE);
                    }
                }
                else initGame();
            }
            @Override public void mouseEntered(MouseEvent e) {}
            @Override public void mouseExited(MouseEvent e) {}
            @Override public void mouseClicked(MouseEvent e) {}
        });
        this.mineLabelCanvas = new CellCanvas(2, true);
        this.timeLabelCanvas = new CellCanvas(2, true);
        this.boardBorderCanvas = new CellCanvas(5, true);
        this.infoBorderCanvas = new CellCanvas(3, true);
        container.add(this.faceCanvas);
        container.add(this.mineLabelCanvas);
        container.add(this.timeLabelCanvas);
        container.add(this.boardBorderCanvas);
        container.add(this.infoBorderCanvas);
    }

    private void initGame() {
        this.initGame(new MineSweeper(this.row, this.col, this.mineCount, this.cheat, this.gameRule));
    }

    private void initGame(int difficulty) {
        this.initGame(new MineSweeper(difficulty, this.cheat, this.gameRule));
    }

    private void initGame(int row, int col, int mineCount) {
        this.initGame(new MineSweeper(row, col, mineCount, this.cheat, this.gameRule));
    }

    private void initGame(MineSweeper newGame) {
        this.game = newGame;
        this.game.setShowMine(this.showMine);
        this.row = newGame.getRow();
        this.col = newGame.getCol();
        this.mineCount = newGame.getMineCount();
//        this.cheat = newGame.getCheat();
//        if (newGame.getGameRule() != MineSweeper.GAME_RULE_UNKNOWN) this.gameRule = newGame.getGameRule();

        this.setFrame();

        if (this.timeThread != null && !this.timeThread.isInterrupted()) this.timeThread.interrupt();
        this.timeThread = new TimeThread(this.timeLabel, this.cheat ? 0x7fffffff : 0);

        if (this.canvas == null) {
            this.canvas = new BoardCanvas();
            this.getContentPane().add(this.canvas, 1);
        }
        else this.canvas.requestRepaintAll(true);
    }

    private void setFrame() {
        int boardWidth = this.col * this.cellLength;
        int boardHeight = this.row * this.cellLength;

        this.getContentPane().setPreferredSize(new Dimension(boardWidth + 20, boardHeight + this.menuBar.getPreferredSize().height + 5 + INFO_HEIGHT));
        this.pack();

        this.infoBorderCanvas.setBounds(5, 5, boardWidth + 10, INFO_HEIGHT - 5);
        this.boardBorderCanvas.setBounds(5, INFO_HEIGHT + 10, boardWidth + 10, boardHeight + 10);

        this.mineLabelCanvas.setBounds(13, 13, 80, 34);
        this.timeLabelCanvas.setBounds(boardWidth - 73, 13, 80, 34);
        this.faceCanvas.setBounds(10 + boardWidth / 2 - 15 - 2, 13, 34, 34);

        this.mineLabel.setBounds(13 + 2, 13 + 2, 76, 30);
        this.timeLabel.setBounds(boardWidth - 71, 13 + 2, 76, 30);
        this.setMineLabel();
        this.faceCanvas.setEmoji(FACE_NORMAL);
    }

    private void setMineLabel() {
        this.mineLabel.setText(String.format("%04d", this.game.getMineLeft()));
    }

    private void setFrameAfterOperation() {
        this.canvas.repaint();
        switch (this.game.getGameState()) {
            case MineSweeper.WIN:
                faceCanvas.setEmoji(FACE_WIN);
                this.timeThread.interrupt();
                break;
            case MineSweeper.LOSE:
                faceCanvas.setEmoji(FACE_LOSE);
                this.timeThread.interrupt();
                break;
            case MineSweeper.PROCESS:
                faceCanvas.setEmoji(FACE_NORMAL);
                break;
        }
        this.setMineLabel();
    }

    private class BoardCanvas extends Canvas implements MouseMotionListener, MouseListener {

        private final Color[] COLOR = {
                new Color(192, 192, 192), new Color(8, 8, 215), new Color(30, 100, 30),
                new Color(171, 25, 26), new Color(10, 5, 96), new Color(84, 9, 20),
                new Color(20, 112, 102), new Color(1, 1, 1), new Color(128, 128, 128)
        };

        private int mouseX, mouseY;
        private boolean mouseLeft, mouseRight, mouseBoth;
        private int[][] lastPlayerBoard;
        private Font font, debugFont;
        private Image buffer;
        private int[][] highlightArr;
        private int[][] connectedComponents;
        private double[][] probability;
        private int step;
        private boolean dontUpdate;

        BoardCanvas() {
            this.mouseX = this.mouseY = -1;
            this.mouseLeft = this.mouseRight = this.mouseBoth = false;
            this.font = new Font("Consolas",Font.BOLD, cellLength);
            this.debugFont = new Font("等线",Font.BOLD, cellLength / 3);
            this.highlightArr = null;
            this.step = 0;
            this.dontUpdate = false;

            this.setBounds(10, 15 + INFO_HEIGHT, col * cellLength, row * cellLength);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        private void drawBoard(Graphics gPanel) {
            if (this.dontUpdate) return;
            if (this.step < game.getStep() && this.probability != null) {
                this.probability = null;
                this.connectedComponents = null;
                this.lastPlayerBoard = null;
            }
            this.step = game.getStep();
            if (this.buffer == null) this.buffer = this.createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) this.buffer.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(this.font);

            if (this.highlightArr != null) {
                int state = 0;
                for (int[] p : this.highlightArr) {
                    if (p.length == 1) { state = p[0]; continue; }
                    int x = p[0], y = p[1];
                    this.drawCell(x, y, state == AutoSweeper.MINE ? MineSweeper.MINE : game.getPlayerBoard(x, y, true),
                            state != AutoSweeper.UNKNOWN, g);
                }
            }
            for (int i = 0; i < row; ++i)  for (int j = 0; j < col; ++j) {
                if (this.lastPlayerBoard == null || this.lastPlayerBoard[i][j] != game.getPlayerBoard(i, j, true))
                    this.drawCell(i, j, game.getPlayerBoard(i, j, true), false, g);
            }
            this.lastPlayerBoard = game.getPlayerBoard(true);

            List<Point> around = new ArrayList<>();
            if (this.mouseBoth && this.mouseLeft && this.mouseRight) {
                around = game.getAround(this.mouseX, this.mouseY);
                around.add(new Point(this.mouseX, this.mouseY));
            }
            if (!this.mouseBoth && this.mouseLeft && game.isPointInRange(this.mouseX, this.mouseY)) {
                around.add(new Point(this.mouseX, this.mouseY));
            }
            for (Point p : around) {
                this.drawCell(p.x, p.y, game.getPlayerBoard(p.x, p.y, true),true, g);
                this.lastPlayerBoard[p.x][p.y] = 0x7fffffff;
            }
            gPanel.drawImage(this.buffer, 0, 0, this);
        }

        private void drawCell(int x, int y, int state, boolean pressed, Graphics2D g) {
            int px = this.idxYToPosX(y);
            int py = this.idxXToPosY(x);

            if (state < 0 || state > 8) {
                if (pressed && (state == MineSweeper.UNCHECKED || state == MineSweeper.GRAY_MINE)) {
                    this.drawPressedVoidCell(px, py, g);
                    if (state == MineSweeper.GRAY_MINE) this.drawMineOfCell(px, py, new Color(166, 166, 166), g);
                }
                else {
                    g.setColor(new Color(253, 253, 253));
                    int[] xpw = {px, px + cellLength - 1, px};
                    int[] ypw = {py, py, py + cellLength - 1};
                    g.fillPolygon(xpw, ypw, 3);

                    g.setColor(new Color(128, 128, 128));
                    int[] xpb = {px + cellLength, px + cellLength, px};
                    int[] ypb = {py + cellLength, py, py + cellLength};
                    g.fillPolygon(xpb, ypb, 3);

                    g.setColor(new Color(192, 192, 192));
                    g.fillRect(px + 4, py + 4, cellLength - 8, cellLength - 8);

                    switch (state) {
                        case MineSweeper.FLAG: this.drawFlagOfCell(px, py, g); break;
                        case MineSweeper.QUESTION: this.drawQuestionOfCell(px, py, g); break;
                        case MineSweeper.MINE: this.drawMineOfCell(px, py, Color.BLACK, g); break;
                        case MineSweeper.NOT_MINE: this.drawMineOfCell(px, py, Color.BLACK, g); this.drawNotOfCell(px, py, g); break;
                        case MineSweeper.RED_MINE: this.drawMineOfCell(px, py, Color.RED, g); break;
                        case MineSweeper.GRAY_MINE: this.drawMineOfCell(px, py, Color.GRAY, g); break;
                    }
                }
                if (this.probability != null && state == MineSweeper.UNCHECKED) this.drawDebug(px, py, g);
            }
            else {
                this.drawPressedVoidCell(px, py, g);
                this.drawNumberOfCell(px, py, state, g);
            }
        }

        private void drawPressedVoidCell(int px, int py, Graphics2D g) {
            g.setColor(new Color(192, 192, 192));
            g.fillRect(px, py, cellLength, cellLength);
            g.setColor(new Color(132, 132, 132));
            g.drawLine(px, py, px + cellLength - 1, py);
            g.drawLine(px, py, px, py + cellLength - 1);
        }
        private void drawNumberOfCell(int px, int py, int state, Graphics2D g) {
            String text = String.valueOf(state);
            FontMetrics fm = g.getFontMetrics(this.font);
            int fontWidth = fm.stringWidth(text);
            int fontHeight = fm.getHeight();
            g.setColor(COLOR[state]);
            g.drawString(text, px + (cellLength - fontWidth) / 2, py + (cellLength + fontHeight / 2) / 2);
        }
        private void drawFlagOfCell(int px, int py, Graphics2D g) {
            String s1 = "┴";
            String s2 = "▸";
            FontMetrics fm = g.getFontMetrics(this.font);
            int fontWidth1 = fm.stringWidth(s1);
            int fontWidth2 = fm.stringWidth(s2);
            int fontHeight = fm.getHeight();
            g.setColor(Color.RED);
            g.drawString(s2, px + (cellLength - fontWidth2 / 2) / 2, py + (int)(cellLength + fontHeight / 4.5) / 2);
            g.setColor(Color.BLACK);
            g.drawString(s1, px + (cellLength - fontWidth1) / 2, py + (cellLength + fontHeight) / 2);
        }
        private void drawQuestionOfCell(int px, int py, Graphics2D g) {
            String s = "¿";
            FontMetrics fm = g.getFontMetrics(this.font);
            int fontWidth = fm.stringWidth(s);
            int fontHeight = fm.getHeight();
            g.setColor(Color.BLACK);
            g.drawString(s, px + (cellLength - fontWidth) / 2, py + (cellLength + fontHeight / 3) / 2);
        }
        private void drawMineOfCell(int px, int py, Color color, Graphics2D g) {
            String s = "●";
            FontMetrics fm = g.getFontMetrics(this.font);
            int fontWidth = fm.stringWidth(s);
            int fontHeight = fm.getHeight();
            g.setColor(color);
            g.drawString(s, px + (cellLength - fontWidth) / 2, py + (cellLength + fontHeight / 2) / 2);
        }
        private void drawNotOfCell(int px, int py, Graphics2D g) {
            String s = "X";
            FontMetrics fm = g.getFontMetrics(this.font);
            int fontWidth = fm.stringWidth(s);
            int fontHeight = fm.getHeight();
            g.setColor(Color.RED);
            g.drawString(s, px + (cellLength - fontWidth) / 2, py + (cellLength + fontHeight / 2) / 2);
        }
        private void drawDebug(int px, int py, Graphics2D g) {
            String s = String.format("%.2f", this.probability[posYToIdxX(py)][posXToIdxY(px)]);
            g.setFont(this.debugFont);
            FontMetrics fm = g.getFontMetrics(this.debugFont);
            int fontWidth = fm.stringWidth(s);
            int fontHeight = fm.getHeight();
            Color color = Color.DARK_GRAY;
            if (this.connectedComponents != null) {
                int id = this.connectedComponents[posYToIdxX(py)][posXToIdxY(px)];
                if (id > 0) color = this.COLOR[(id - 1) % 6 + 1];
            }
            g.setColor(color);
            g.drawString(s, px + (cellLength - fontWidth) / 2, py + (cellLength + fontHeight / 2) / 2);
            g.setFont(this.font);
        }

        private int posYToIdxX(int y) { return Math.min(row - 1, Math.max(0, y / cellLength)); }
        private int posXToIdxY(int x) { return Math.min(col - 1, Math.max(0, x / cellLength)); }
        private int idxYToPosX(int y) { return y * cellLength; }
        private int idxXToPosY(int x) { return x * cellLength; }

        public void requestRepaintAll(boolean newGame) {
            this.setSize(col * cellLength, row * cellLength);
            this.font = new Font("Consolas",Font.BOLD, cellLength);
            this.debugFont = new Font("等线",Font.BOLD, cellLength / 3);
            this.buffer = null;
            this.lastPlayerBoard = null;
            if (newGame) this.step = -1;
            this.repaint();
        }

        public void highlight(int[][] arr) {
            this.highlightArr = arr;
            this.repaint();
        }

        public void setConnectedComponentsAndProbability(int[][] cc, double[][] prob) {
            this.connectedComponents = cc;
            this.probability = prob;
            this.lastPlayerBoard = null;
            this.repaint();
        }

        public void doNotUpdateTheFuckingCanvasNow(boolean dont) {
            this.dontUpdate = dont;
        }

        @Override
        public void update(Graphics g) {
            this.drawBoard(g);
        }
        @Override
        public void paint(Graphics g) {
            if (!this.mouseLeft && !this.mouseRight) this.drawBoard(g);
        }
        @Override
        public void mouseClicked(MouseEvent e) { }
        @Override
        public void mousePressed(MouseEvent e) {
            if (this.dontUpdate) return;
            if (game.getGameState() != MineSweeper.PROCESS) return;
            faceCanvas.setEmoji(FACE_PRESS);
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) this.mouseLeft = true;
            else if (e.getButton() == MouseEvent.BUTTON3) this.mouseRight = true;
            else if (e.getButton() == MouseEvent.BUTTON2) this.mouseLeft = this.mouseRight = this.mouseBoth = true;
            if (this.mouseLeft && this.mouseRight) this.mouseBoth = true;
            this.repaint();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            if (this.dontUpdate) return;
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) {
                this.mouseLeft = false;
                if (this.mouseBoth) {
                    if (this.mouseRight) game.check(this.mouseX, this.mouseY);
                }
                else if (!this.mouseRight) {
                    game.dig(this.mouseX, this.mouseY);
                    if (!timeThread.isAlive() && game.getGameState() == MineSweeper.PROCESS && !cheat)
                        timeThread.start();
                }
            }
            else if (e.getButton() == MouseEvent.BUTTON3) {
                this.mouseRight = false;
                if (this.mouseBoth) {
                    if (this.mouseLeft) game.check(this.mouseX, this.mouseY);
                }
                else if (!this.mouseLeft) {
                    game.mark(this.mouseX, this.mouseY);
                    setMineLabel();
                    if (!timeThread.isAlive() && game.getGameState() == MineSweeper.PROCESS && !cheat)
                        timeThread.start();
                }
            }
            else if (e.getButton() == MouseEvent.BUTTON2) {
                this.mouseLeft = this.mouseRight = this.mouseBoth = false;
                game.check(this.mouseX, this.mouseY);
            }
            if (!this.mouseLeft && !this.mouseRight) this.mouseBoth = false;
            setFrameAfterOperation();
        }
        @Override
        public void mouseEntered(MouseEvent e) { }
        @Override
        public void mouseExited(MouseEvent e) { }
        @Override
        public void mouseDragged(MouseEvent e) {
            if (this.dontUpdate) return;
            int lastMouseX = this.mouseX;
            int lastMouseY = this.mouseY;
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if ((this.mouseLeft || this.mouseRight)
                    && (this.mouseX !=lastMouseX || this.mouseY !=lastMouseY)) this.repaint();
        }
        @Override
        public void mouseMoved(MouseEvent e) { }
    }

    private static class CellCanvas extends Canvas {
        private boolean reverse;
        private final int margin;
        private boolean yellowFace;
        private String emoji;

        CellCanvas(int margin, boolean reverse) {
            this.reverse = reverse;
            this.margin = margin;
            this.yellowFace = false;
            this.emoji = FACE_NORMAL;
        }

        void enableYellowFace() { this.yellowFace = true; }

        void setReverse(boolean reverse) {
            this.reverse = reverse;
            this.repaint();
        }

        void setEmoji(String emoji) {
            this.emoji = emoji;
            this.repaint();
        }

        private void drawCanvas(Graphics gCanvas) {
            Image buffer = this.createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) buffer.getGraphics();
            int w = this.getWidth();
            int h = this.getHeight();
            Color c1 = new Color(253, 253, 253);
            Color c2 = new Color(128, 128, 128);
            Color c3 = new Color(192, 192, 192);
            if (this.reverse) {
                Color t = c1; c1 = c2; c2 = t;
            }
            g.setColor(c1);
            int[] xp1 = {0, 0, w, w - 4, 4};
            int[] yp1 = {h, 0, 0, 4, h - 4};
            g.fillPolygon(xp1, yp1, 5);

            g.setColor(c2);
            int[] xp2 = {0, w, w, w - 4, 4};
            int[] yp2 = {h, h, 0, 4, h - 4};
            g.fillPolygon(xp2, yp2, 5);

            g.setColor(c3);
            g.fillRect(margin, margin, w - margin * 2, h - margin * 2);

            if (yellowFace) {
                g.setColor(new Color(255, 248, 2));
                g.fillOval(6, 6, 23, 22);
                g.setColor(new Color(0, 0, 0));
                g.setFont(new Font("Dialog",Font.BOLD, 24));
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.drawString(this.reverse ? FACE_RESTART : this.emoji, 5, 26);
            }
            gCanvas.drawImage(buffer, 0, 0, this);
        }

        @Override public void update(Graphics gCanvas) { this.drawCanvas(gCanvas); }

        @Override public void paint(Graphics gCanvas) { this.drawCanvas(gCanvas); }
    }

    private static class TimeThread extends Thread {
        private Date startTime, currentTime;
        private final JLabel targetLabel;

        TimeThread(JLabel label, long initTime) {
            this.targetLabel = label;
            this.setLabel(initTime);
        }

        private void setLabel(long time) {
            if (time > 9999) time = 9999;
            this.targetLabel.setText(String.format("%04d", time));
        }

        @Override
        public void run() {
            this.startTime = this.currentTime = new Date();
            while (!this.isInterrupted()) {
                try {
                    this.currentTime = new Date();
                    long diff = (this.currentTime.getTime() - startTime.getTime());
                    this.setLabel(diff / 1000);
                    Thread.sleep(1000 - (diff % 1000));
                } catch (InterruptedException e) { break; }
            }
        }
    }
}
