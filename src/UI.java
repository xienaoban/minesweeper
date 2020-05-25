import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UI extends JFrame {

    private static final String FACE_NORMAL = "\uD83D\uDE42";
    private static final String FACE_PRESS = "\uD83D\uDE2E";
    private static final String FACE_WIN = "\uD83D\uDE0E";
    private static final String FACE_LOSE = "\uD83D\uDE2B";
    private static final String FACE_RESTART = "\uD83D\uDE32";

    private static final int INFO_HEIGHT = 55;

    private int row, col, mineCount;
    private boolean cheat, showMine;
    private Game game;
    private AI ai;

    private int cellLength;
    private BoardCanvas canvas;
    private TimeThread timeThread;
    private JMenuBar menuBar;
    private JLabel mineLabel;
    private JLabel timeLabel;
    private CellCanvas faceCanvas, mineLabelCanvas, timeLabelCanvas,
            boardBorderCanvas, infoBorderCanvas;

    public UI() {
        this.setTitle("Minesweeper");
        this.setLayout(new BorderLayout());
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setLayout(null);
        this.getContentPane().setBackground(new Color(192, 192, 192));
        this.cellLength = 28;
        this.showMine = false;
        this.initMenu();
        this.initGame(9, 9, 10, false);
        this.setVisible(true);
    }

    private void initMenu() {
        menuBar = new JMenuBar();
        JMenu gameMenu  = new JMenu("游戏");
        JMenu cheatMenu = new JMenu("作弊");
        JMenu aiMenu    = new JMenu("AI");
        JMenu aboutMenu = new JMenu("关于");

        JMenuItem newGameMenuItem       = new JMenuItem("新局");
        JMenuItem beginnerMenuItem      = new JMenuItem("初级");
        JMenuItem intermediateMenuItem  = new JMenuItem("中级");
        JMenuItem advancedMenuItem      = new JMenuItem("高级");
        JMenuItem customMenuItem        = new JMenuItem("自定义");
        JMenuItem cellLengthMenuItem      = new JMenuItem("格子大小");
        gameMenu.add(newGameMenuItem);
        gameMenu.add(beginnerMenuItem);
        gameMenu.add(intermediateMenuItem);
        gameMenu.add(advancedMenuItem);
        gameMenu.add(customMenuItem);
        gameMenu.add(cellLengthMenuItem);

        JMenuItem cheatMenuItem         = new JMenuItem("启用作弊");
        JMenuItem undoMenuItem          = new JMenuItem("撤销操作");
        JMenuItem mineMenuItem          = new JMenuItem("启用透视");
        JMenuItem luckyMenuItem         = new JMenuItem("欧皇模式");
        JMenuItem unluckyMenuItem       = new JMenuItem("非酋模式");
        cheatMenuItem.setText((cheat? "关闭" : "启用") + "作弊");
        undoMenuItem.setEnabled(cheat);
        mineMenuItem.setEnabled(cheat);
        luckyMenuItem.setEnabled(cheat);
        unluckyMenuItem.setEnabled(cheat);
        cheatMenu.add(cheatMenuItem);
        cheatMenu.add(undoMenuItem);
        cheatMenu.add(mineMenuItem);
        cheatMenu.add(luckyMenuItem);
        cheatMenu.add(unluckyMenuItem);


        JMenuItem checkBasicallyMenuItem = new JMenuItem("提示一格");
        JMenuItem sweepBasicallyMenuItem = new JMenuItem("简易扫扫");
        aiMenu.add(checkBasicallyMenuItem);
        aiMenu.add(sweepBasicallyMenuItem);

        JMenuItem aboutMenuItem         = new JMenuItem("作者: 蟹恼板");
        aboutMenu.add(aboutMenuItem);

        menuBar.add(gameMenu);
        menuBar.add(cheatMenu);
        menuBar.add(aiMenu);
        menuBar.add(aboutMenu);

        newGameMenuItem.addActionListener(e -> initGame(row, col, mineCount, cheat));
        beginnerMenuItem.addActionListener(e -> initGame(9, 9, 10, cheat));
        intermediateMenuItem.addActionListener(e -> initGame(16, 16, 40, cheat));
        advancedMenuItem.addActionListener(e -> initGame(16, 30, 99, cheat));
        customMenuItem.addActionListener(e -> {
            try {
                String s = row + " " + col + " " + mineCount;
                String[] arr = JOptionPane.showInputDialog("自定义棋盘(行 列 雷)", s).split(" ");
                int r = Integer.parseInt(arr[0]);
                int c = Integer.parseInt(arr[1]);
                int m = Integer.parseInt(arr[2]);
                if (r < 1 || c < 1 || m < 0 || m >= r * c) throw new Exception("你丫是不是数学不好¿");
                initGame(r, c, m, cheat);
            }
            catch (NullPointerException ignored) {}
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.toString(), "格式或数字有误", JOptionPane.ERROR_MESSAGE);
            }
        });
        cellLengthMenuItem.addActionListener(e -> {
            try {
                cellLength = Integer.parseInt(JOptionPane.showInputDialog("格子大小设置", String.valueOf(cellLength)));
                setFrame();
                canvas.requestRepaintAll();
            }
            catch (NumberFormatException ignored) {}
            catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "格式错误，请输入合法的整数。", "错误",JOptionPane.ERROR_MESSAGE);
            }
        });

        cheatMenuItem.addActionListener(e -> {
            int res = JOptionPane.showConfirmDialog(null, "开启/关闭作弊会启用新局，确认继续吗？", "作弊",JOptionPane.YES_NO_OPTION);
            if (res != 0) return;
            cheat = !cheat;
            if (!cheat) showMine = false;
            cheatMenuItem.setText((cheat? "关闭" : "启用") + "作弊");
            undoMenuItem.setEnabled(cheat);
            mineMenuItem.setEnabled(cheat);
            luckyMenuItem.setEnabled(cheat);
            unluckyMenuItem.setEnabled(cheat);
            initGame(row, col, mineCount, cheat);
        });
        undoMenuItem.addActionListener(e -> { game.undo(); setFrameAfterOperation(); });
        mineMenuItem.addActionListener(e -> {
            if (!cheat) showMine = false;
            else {
                showMine = !showMine;
                mineMenuItem.setText((showMine? "关闭" : "启用") + "透视");
                game.setShowMine(showMine);
                canvas.repaint();
            }
        });

        checkBasicallyMenuItem.addActionListener(e -> {
            int[] res = AI.checkAllBasically(game);
            if (res[0] != AI.UNKNOWN) {
                new Thread() {
                    public void run() {
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
                    }
                }.start();
            }
        });

        sweepBasicallyMenuItem.addActionListener(e -> {
            AI.sweepAllBasically(game);
            setFrameAfterOperation();
        });

        aboutMenuItem.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(java.net.URI.create("https://github.com/XieNaoban"));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, "浏览器打开失败，请手动访问 https://github.com/XieNaoban 。", "错误",JOptionPane.ERROR_MESSAGE);
            }
        });
        this.setJMenuBar(menuBar);

        Color panelForeColor = new Color(146, 26, 33);
        Color panelBackColor = new Color(20, 0, 0);
        Font panelFont = new Font("Consolas",Font.BOLD, cellLength);
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
                initGame(row, col, mineCount, cheat);
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

    private void initGame(int row, int col, int mineCount, boolean cheat) {
        this.row = row;
        this.col = col;
        this.mineCount = mineCount;
        this.cheat = cheat;
        this.game = new Game(this.row, this.col, this.mineCount, this.cheat);
        this.game.setShowMine(this.showMine);

        this.setFrame();

        if (this.timeThread != null && !this.timeThread.isInterrupted()) this.timeThread.interrupt();
        this.timeThread = new TimeThread(this.timeLabel, this.cheat ? 0x7fffffff : 0);

        if (this.canvas != null) this.remove(this.canvas);
        this.canvas = new BoardCanvas();
        this.getContentPane().add(this.canvas, 1);
    }

    private void setFrame() {
        int boardWidth = this.col * this.cellLength;
        int boardHeight = this.row * this.cellLength;

        this.getContentPane().setPreferredSize(new Dimension(boardWidth + 20, boardHeight + this.menuBar.getPreferredSize().height + 5 + INFO_HEIGHT));
        this.pack();

        this.infoBorderCanvas.setBounds(5, 5, boardWidth + 10, INFO_HEIGHT - 5);
        this.boardBorderCanvas.setBounds(5, INFO_HEIGHT + 10, boardWidth + 10, boardHeight + 10);

        this.timeLabelCanvas.setBounds(13, 13, 74, 34);
        this.mineLabelCanvas.setBounds(boardWidth - 60 - 4 - 3, 13, 74, 34);
        this.faceCanvas.setBounds(10 + boardWidth / 2 - 15 - 2, 13, 34, 34);

        this.timeLabel.setBounds(13 + 2, 13 + 2, 70, 30);
        this.mineLabel.setBounds(boardWidth - 60 - 2 - 3, 13 + 2, 70, 30);
        this.setMineLabel();
        this.faceCanvas.setEmoji(FACE_NORMAL);
    }

    private void setMineLabel() {
        this.mineLabel.setText(String.format("%04d", this.game.getMineLeft()));
    }

    private void setFrameAfterOperation() {
        this.canvas.repaint();
        switch (this.game.getGameState()) {
            case Game.SUCCESS:
                faceCanvas.setEmoji(FACE_WIN);
                this.timeThread.interrupt();
                break;
            case Game.FAIL:
                faceCanvas.setEmoji(FACE_LOSE);
                this.timeThread.interrupt();
                break;
            case Game.PROCESS:
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
        private Font font;
        private Image buffer;
        private int[][] highlightArr;

        BoardCanvas() {
            this.mouseX = this.mouseY = -1;
            this.mouseLeft = this.mouseRight = this.mouseBoth = false;
            this.font = new Font("Consolas",Font.BOLD, cellLength);
            this.highlightArr = null;

            this.setBounds(10, 15 + INFO_HEIGHT, col * cellLength, row * cellLength);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        private void drawBoard(Graphics gPanel) {
            if (this.buffer == null) this.buffer = this.createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) this.buffer.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(this.font);

            if (this.highlightArr != null) {
                int state = 0;
                for (int[] p : this.highlightArr) {
                    if (p.length == 1) { state = p[0]; continue; }
                    int x = p[0], y = p[1];
                    this.drawCell(x, y, state == AI.MINE ? Game.MINE :  game.getPlayerBoard(x, y, true), state != AI.UNKNOWN, g);
                }
            }
            for (int i = 0; i < row; ++i)  for (int j = 0; j < col; ++j) {
                if (this.lastPlayerBoard == null || this.lastPlayerBoard[i][j] != game.getPlayerBoard(i, j, true))
                    this.drawCell(i, j, game.getPlayerBoard(i, j, true), false, g);
            }
            this.lastPlayerBoard = game.getPlayerBoard(true);

            List<Pair<Integer, Integer>> around = new ArrayList<>();
            if (this.mouseLeft && this.mouseRight) {
                around = game.getAround(this.mouseX, this.mouseY);
            }
            if ((this.mouseLeft || this.mouseRight) && game.isXYLegal(this.mouseX, this.mouseY)) {
                around.add(new Pair<>(this.mouseX, this.mouseY));
            }
            for (Pair<Integer, Integer> xy : around) {
                int x = xy.getKey(), y = xy.getValue();
                this.drawCell(x, y, game.getPlayerBoard(x, y, true),true, g);
                this.lastPlayerBoard[xy.getKey()][xy.getValue()] = 0x7fffffff;
            }
            gPanel.drawImage(this.buffer, 0, 0, this);
        }

        private void drawCell(int x, int y, int state, boolean pressed, Graphics2D g) {
            int px = this.idxYToPosX(y);
            int py = this.idxXToPosY(x);

            if (state < 0 || state > 8) {
                if (pressed && (state == Game.UNCHECKED || state == Game.GRAY_MINE)) {
                    this.drawPressedVoidCell(px, py, g);
                    if (state == Game.GRAY_MINE) this.drawMineOfCell(px, py, new Color(166, 166, 166), g);
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
                        case Game.FLAG: this.drawFlagOfCell(px, py, g); break;
                        case Game.QUESTION: this.drawQuestionOfCell(px, py, g); break;
                        case Game.MINE: this.drawMineOfCell(px, py, Color.BLACK, g); break;
                        case Game.NOT_MINE: this.drawMineOfCell(px, py, Color.BLACK, g); this.drawNotOfCell(px, py, g); break;
                        case Game.RED_MINE: this.drawMineOfCell(px, py, Color.RED, g); break;
                        case Game.GRAY_MINE: this.drawMineOfCell(px, py, Color.GRAY, g); break;
                    }
                }
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
            g.setColor(new Color(234, 128, 21));
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

        private int posYToIdxX(int y) { return y / cellLength; }
        private int posXToIdxY(int x) { return x / cellLength; }
        private int idxYToPosX(int y) { return y * cellLength; }
        private int idxXToPosY(int x) { return x * cellLength; }

        public void requestRepaintAll() {
            this.setSize(col * cellLength, row * cellLength);
            this.font = new Font("Consolas",Font.BOLD, cellLength);
            this.buffer = null;
            this.lastPlayerBoard = null;
            this.repaint();
        }

        public void highlight(int[][] arr) {
            this.highlightArr = arr;
            this.repaint();
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
            if (game.getGameState() != Game.PROCESS) return;
            faceCanvas.setEmoji(FACE_PRESS);
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) this.mouseLeft = true;
            else if (e.getButton() == MouseEvent.BUTTON3) this.mouseRight = true;
            if (this.mouseLeft && this.mouseRight) this.mouseBoth = true;
            this.repaint();
        }
        @Override
        public void mouseReleased(MouseEvent e) {
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) {
                this.mouseLeft = false;
                if (this.mouseBoth) game.check(this.mouseX, this.mouseY);
                else if (!this.mouseRight) {
                    game.uncover(this.mouseX, this.mouseY);
                    if (!timeThread.isAlive() && game.getGameState() == Game.PROCESS && !cheat)
                        timeThread.start();
                }
            }
            else if (e.getButton() == MouseEvent.BUTTON3) {
                this.mouseRight = false;
                if (this.mouseBoth) game.check(this.mouseX, this.mouseY);
                else if (!this.mouseLeft) {
                    game.cycFlagAndQuestion(this.mouseX, this.mouseY);
                    setMineLabel();
                    if (!timeThread.isAlive() && game.getGameState() == Game.PROCESS && !cheat)
                        timeThread.start();
                }
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

    private class CellCanvas extends Canvas {
        private boolean reverse;
        private int margin;
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

    private class TimeThread extends Thread {
        private Date startTime, currentTime;
        private JLabel targetLabel;

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
