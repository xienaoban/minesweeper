import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
        this.setResizable(true);
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        this.getContentPane().setBackground(new Color(200, 200, 200));
        this.cellLength = 24;
        this.initMenu();
        this.initGame(16, 30, 99, false);
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
        gameMenu.add(newGameMenuItem);
        gameMenu.add(beginnerMenuItem);
        gameMenu.add(intermediateMenuItem);
        gameMenu.add(advancedMenuItem);
        gameMenu.add(customMenuItem);

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

        this.setSize(this.col * this.cellLength + 36, this.row * this.cellLength + menuBar.getPreferredSize().height + 59);

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
        private boolean mouseLeft, mouseRight;
        private Font font;

        BoardCanvas() {
            this.mouseX = this.mouseY = -1;
            this.mouseLeft = this.mouseRight = false;
            this.font = new Font("Consolas",Font.BOLD, cellLength);

            this.setBounds(10, 10, col * cellLength, row * cellLength);
            this.addMouseListener(this);
            this.addMouseMotionListener(this);
        }

        private void drawBoard(Graphics2D g) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setFont(this.font);
            for (int i = 0; i < row; ++i)  for (int j = 0; j < col; ++j) {
                this.drawCell(i, j, g);
            }
        }

        private void drawCell(int x, int y, Graphics2D g) {
            int state = game.getPlayerBoard(x, y);
            int px = this.idxYToPosX(y);
            int py = this.idxXToPosY(x);

            if (state < 0 || state > 8) {
                if ((this.mouseLeft || this.mouseRight) && this.mouseX == x && this.mouseY == y) {
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
                        case Chessboard.MINE: this.drawNumberOfCell(px, py, 1, g);this.drawNumberOfCell(px, py, 2, g); break;
                        case Chessboard.NOT_MINE: this.drawNumberOfCell(px, py, 3, g);this.drawNumberOfCell(px, py, 4, g); break;
                        case Chessboard.RED_MINE: this.drawNumberOfCell(px, py, 5, g);this.drawNumberOfCell(px, py, 6, g); break;
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
            g.drawLine(px, py, px, px + cellLength - 1);
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

        private int posYToIdxX(int y) { return y / cellLength; }
        private int posXToIdxY(int x) { return x / cellLength; }
        private int idxYToPosX(int y) { return y * cellLength; }
        private int idxXToPosY(int x) { return x * cellLength; }


        @Override
        public void paint(Graphics g) {
            Image buffer = this.createImage(this.getWidth(), this.getHeight());
            this.drawBoard((Graphics2D) buffer.getGraphics());
            g.drawImage(buffer, 0, 0, this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) this.mouseLeft = true;
            else if (e.getButton() == MouseEvent.BUTTON3) this.mouseRight = true;
            this.repaint();
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            this.mouseX = this.posYToIdxX(e.getY());
            this.mouseY = this.posXToIdxY(e.getX());
            if (e.getButton() == MouseEvent.BUTTON1) {
                this.mouseLeft = false;
                if (this.mouseRight) game.check(this.mouseX, this.mouseY);
                else game.uncover(this.mouseX, this.mouseY);
            }
            else if (e.getButton() == MouseEvent.BUTTON3) {
                this.mouseRight = false;
                if (this.mouseLeft) game.check(this.mouseX, this.mouseY);
                else game.cycFlagAndQuestion(this.mouseX, this.mouseY);
            }
            this.repaint();
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {

        }

        @Override
        public void mouseDragged(MouseEvent e) {

        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }
}
