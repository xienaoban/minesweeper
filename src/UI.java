import javafx.util.Pair;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class UI extends JFrame {

    private int row, col, mineCount;
    boolean cheat;
    private Chessboard game;

    private int cellLength;
    private BoardCanvas canvas;
    private JMenuBar menuBar;

    public UI() {
        this.setTitle("Minesweeper");
        this.setLayout(null);
        this.setResizable(false);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setBackground(new Color(200, 200, 200));
        this.cellLength = 24;
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
        JMenuItem mineMenuItem          = new JMenuItem("查看地雷");
        JMenuItem luckyMenuItem         = new JMenuItem("欧皇模式");
        JMenuItem unluckyMenuItem       = new JMenuItem("非酋模式");
        cheatMenuItem.setText((cheat? "关闭" : "启用") + "作弊");
        mineMenuItem.setEnabled(cheat);
        luckyMenuItem.setEnabled(cheat);
        unluckyMenuItem.setEnabled(cheat);
        cheatMenu.add(cheatMenuItem);
        cheatMenu.add(mineMenuItem);
        cheatMenu.add(luckyMenuItem);
        cheatMenu.add(unluckyMenuItem);

        JMenuItem aboutMenuItem         = new JMenuItem("作者: 蟹恼板");
        aboutMenu.add(aboutMenuItem);

        menuBar.add(gameMenu);
        menuBar.add(cheatMenu);
        menuBar.add(aiMenu);
        menuBar.add(aboutMenu);

        newGameMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initGame(row, col, mineCount, cheat);
            }
        });
        beginnerMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initGame(9, 9, 10, cheat);
            }
        });
        intermediateMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initGame(16, 16, 40, cheat);
            }
        });
        advancedMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                initGame(16, 30, 99, cheat);
            }
        });
        customMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String s = String.valueOf(row) + " " + String.valueOf(col) + " " + String.valueOf(mineCount);
                    String[] arr = JOptionPane.showInputDialog("自定义棋盘(行 列 雷)", s).split(" ");
                    int r = Integer.parseInt(arr[0]);
                    int c = Integer.parseInt(arr[1]);
                    int m = Integer.parseInt(arr[2]);
                    if (r < 1 || c < 1 || m < 0 || m >= r * c) throw new Exception("你丫是不是数学不好¿");
                    initGame(r, c, m, cheat);
                }
                catch (Exception ex) {
                    JOptionPane.showMessageDialog(null, ex.toString(), "格式或数字有误", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
        cellLengthMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    cellLength = Integer.parseInt(JOptionPane.showInputDialog("格子大小设置", String.valueOf(cellLength)));
                    setSize(col * cellLength + 16 + 10, row * cellLength + menuBar.getPreferredSize().height + 39 + 10);
                    canvas.requestRepaintAll();
                }
                catch (Exception ex) {}
            }
        });

        cheatMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cheat = !cheat;
                cheatMenuItem.setText((cheat? "关闭" : "启用") + "作弊");
                mineMenuItem.setEnabled(cheat);
                luckyMenuItem.setEnabled(cheat);
                unluckyMenuItem.setEnabled(cheat);
            }
        });

        this.setJMenuBar(menuBar);
    }

    private void initGame(int row, int col, int mineCount, boolean cheat) {
        this.row = row;
        this.col = col;
        this.mineCount = mineCount;
        this.cheat = cheat;
        this.game = new Chessboard(this.row, this.col, this.mineCount);

        this.setSize(this.col * this.cellLength + 16 + 10, this.row * this.cellLength + menuBar.getPreferredSize().height + 39 + 10);

        if (canvas != null) this.remove(canvas);
        this.canvas = new BoardCanvas();
        this.add(canvas);
    }

    private class BoardCanvas extends Canvas implements MouseMotionListener, MouseListener {

        private final Color COLOR[] = {
                new Color(192, 192, 192), new Color(8, 8, 215), new Color(30, 100, 30),
                new Color(171, 25, 26), new Color(10, 5, 96), new Color(84, 9, 20),
                new Color(20, 112, 102), new Color(1, 1, 1), new Color(128, 128, 128)
        };

        private int mouseX, mouseY;
        private boolean mouseLeft, mouseRight, mouseBoth;
        private int[][] lastPlayerBoard;
        private Font font;
        private Image buffer;

        BoardCanvas() {
            this.mouseX = this.mouseY = -1;
            this.mouseLeft = this.mouseRight = this.mouseBoth = false;
            this.font = new Font("Consolas",Font.BOLD, cellLength);

            this.setBounds(10, 10, col * cellLength, row * cellLength);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        private void drawBoard(Graphics gPanel) {
            if (this.buffer == null) this.buffer = this.createImage(this.getWidth(), this.getHeight());
            Graphics2D g = (Graphics2D) this.buffer.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(this.font);

            for (int i = 0; i < row; ++i)  for (int j = 0; j < col; ++j) {
                if (this.lastPlayerBoard == null || this.lastPlayerBoard[i][j] != game.getPlayerBoard(i, j))
                    this.drawCell(i, j, false, g);
            }
            this.lastPlayerBoard = game.getPlayerBoard();

            List<Pair<Integer, Integer>> around = new ArrayList<>();
            if (this.mouseLeft && this.mouseRight) {
                around = game.getAround(this.mouseX, this.mouseY);
            }
            if ((this.mouseLeft || this.mouseRight)) {
                around.add(new Pair<>(this.mouseX, this.mouseY));
            }
            for (Pair<Integer, Integer> xy : around) {
                this.drawCell(xy.getKey(), xy.getValue(), true, g);
                this.lastPlayerBoard[xy.getKey()][xy.getValue()] = 0x7fffffff;
            }
            gPanel.drawImage(this.buffer, 0, 0, this);
        }

        private void drawCell(int x, int y, boolean pressed, Graphics2D g) {
            int state = game.getPlayerBoard(x, y);
            int px = this.idxYToPosX(y);
            int py = this.idxXToPosY(x);

            if (state < 0 || state > 8) {
                if (pressed && state == Chessboard.UNCHECKED) {
                    this.drawPressedVoidCell(px, py, g);
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
                        case Chessboard.FLAG: this.drawFlagOfCell(px, py, g); break;
                        case Chessboard.QUESTION: this.drawQuestionOfCell(px, py, g); break;
                        case Chessboard.MINE: this.drawMineOfCell(px, py, Color.BLACK, g); break;
                        case Chessboard.NOT_MINE: this.drawMineOfCell(px, py, Color.BLACK, g); this.drawNotOfCell(px, py, g); break;
                        case Chessboard.RED_MINE: this.drawMineOfCell(px, py, Color.RED, g); break;
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
            g.setColor(Color.BLACK);
            g.drawString(s1, px + (cellLength - fontWidth1) / 2, py + (cellLength + fontHeight) / 2);
            g.setColor(Color.RED);
            g.drawString(s2, px + (cellLength - fontWidth2 / 2) / 2, py + (cellLength + fontHeight / 4) / 2);
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
            this.buffer = null;
            this.lastPlayerBoard = null;
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
                else if (!this.mouseRight) game.uncover(this.mouseX, this.mouseY);
            }
            else if (e.getButton() == MouseEvent.BUTTON3) {
                this.mouseRight = false;
                if (this.mouseBoth) game.check(this.mouseX, this.mouseY);
                else if (!this.mouseLeft) game.cycFlagAndQuestion(this.mouseX, this.mouseY);
            }
            if (!this.mouseLeft && !this.mouseRight) this.mouseBoth = false;
            this.repaint();

            if (game.getChessBoardState() != Chessboard.PROCESS) {
                JOptionPane.showMessageDialog(null, game.getChessBoardState() == Chessboard.SUCCESS ? "牛啤嗷" : "滑稽");
            }
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
}
