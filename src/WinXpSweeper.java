import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class WinXpSweeper extends MineSweeper {
    private static final int OFFSET_X = 3;
    private static final int OFFSET_Y = 46;

    private Robot robot;
    private Rectangle boardPosition;

    public WinXpSweeper() { this.initXpGame(); }

    private void initXpGame() {
        try { this.robot = new Robot(); } catch (AWTException e) {
            throw new RuntimeException("Robot 初始化失败.");
        }
        BufferedImage image = this.captureScreen();

        Point yellowFace = this.findYellowFace(image);
        if (yellowFace == null) throw new WindowOccludedException();
        Rectangle rect = this.boardPosition = this.findWindow(image, yellowFace);
        image = image.getSubimage(rect.x, rect.y, rect.width, rect.height);
        int row = (image.getHeight() - OFFSET_Y - OFFSET_X) / 16;
        int col = (image.getWidth() - 2 * OFFSET_X) / 16;
        int mine = getNumberOfLed(image, 9, 8) * 100 + getNumberOfLed(image, 22, 8) * 10
                + getNumberOfLed(image, 35, 8);
        int [][] board = new int[row][col];
        for (int i = 0; i < row; ++i) for (int j = 0; j < col; ++j) {
            board[i][j] = this.getCell(image, i, j);
            if (board[i][j] == FLAG) ++mine;
        }
        this.initGame(row, col, mine, false, null, GAME_RULE_WIN_XP);
        this.playerBoard = board;

//        this.printPlayerBoardToConsole();
//        System.out.println(this.mineCount);
    }

    private BufferedImage captureBoard() {
        BufferedImage image = this.captureScreen(this.boardPosition);
        if (image.getRGB(0, 0) != -8355712 || image.getRGB(image.getWidth() - 1, 1) != -1
                || image.getRGB(image.getWidth() - 2, 0) != -8355712
                || image.getRGB(image.getWidth() - 1, image.getHeight() - 1) != -1
                || image.getRGB(1, image.getHeight() - 1) != -1
                || image.getRGB(0, image.getHeight() - 2) != -8355712) {
            throw new WindowOccludedException();
        }
        return image;
    }

    private BufferedImage captureScreen() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        return this.captureScreen(new Rectangle(screenSize));
//        robot.mouseMove(0, 0);
//        robot.mousePress(InputEvent.BUTTON1_MASK);
//        robot.mouseRelease(InputEvent.BUTTON1_MASK);
//        robot.mousePress(InputEvent.BUTTON1_MASK);
//        robot.mouseRelease(InputEvent.BUTTON1_MASK);
    }

    private BufferedImage captureScreen(Rectangle size) {
        return this.robot.createScreenCapture(size);
    }


    private Point findYellowFace(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        for (int i = 0; i < width; ++i) for (int j = 0; j < height; ++j) {
            if (image.getRGB(i, j) == -256
                    && image.getRGB(i - 7, j) == -256 && image.getRGB(i, j - 7) == -256
                    && image.getRGB(i + 7, j) == -256 && image.getRGB(i, j + 7) == -256) {
                return new Point(i, j);
            }
        }
        return null;
    }

    private Rectangle findWindow(BufferedImage image, Point yellowFace) {
        int x = yellowFace.x, y = yellowFace.y + 24;
        int x1 = x, y1 = yellowFace.y - 19;
        int x2 = x, y2 = y;
        while (image.getRGB(x1, y) == -8355712) --x1;
        ++x1;
        while (image.getRGB(x2, y) == -8355712) ++x2;
        ++y2;
        while (image.getRGB(x2, y2) == -1) ++y2;
        --y2;
        if (x1 + x2 + 1 >> 1 != x || image.getRGB(x1 + 1, y2) != -1 || image.getRGB(x1, y2 - 1) != -8355712) {
            throw new WindowOccludedException(image.getSubimage(x1, y1, x2 - x1 + 1, y2 - y1 + 1));
        }
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    private void saveImage(BufferedImage image, String filename) {
        File file = new File("C:\\Users\\xie\\Desktop", filename + ".png");
        try {
            ImageIO.write(image, "png", file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private int getNumberOfLed(BufferedImage image, int left, int top) {
        boolean vTop = image.getRGB(left + 5, top + 1) == -65536;
        boolean vMid = image.getRGB(left + 5, top + 10) == -65536;
        boolean vBot = image.getRGB(left + 5, top + 19) == -65536;
        boolean vL1 = image.getRGB(left + 1, top + 5) == -65536;
        boolean vL2 = image.getRGB(left + 1, top + 15) == -65536;
        boolean vR1 = image.getRGB(left + 9, top + 5) == -65536;
        boolean vR2 = image.getRGB(left + 9, top + 15) == -65536;
        if (!vR2) return 2;
        if (vL2) {
            if (!vR1) return 6;
            return vMid ? 8 : 0;
        }
        if (!vR1) return 5;
        if (vBot) return vL1 ? 9: 3;
        if (vMid) return 4;
        if (vTop) return 7;
        return 1;
    }

    private int getCell(BufferedImage image, int x, int y) {
        int cx = y * 16 + OFFSET_X, cy = x * 16 + OFFSET_Y;
        switch (image.getRGB(cx + 7, cy + 8)) {
            case -4144960:
                if (image.getRGB(cx, cy) == -1) {
                    return image.getRGB(cx + 7, cy + 7) == -65536 ? FLAG : UNCHECKED;
                }
                return image.getRGB(cx + 3, cy + 3) == -16777216 ? 7 : 0;
            case -16776961: return 1;
            case -16744448: return 2;
            case -65536: return image.getRGB(cx + 6, cy + 8) == -16777216 ? NOT_MINE : 3;
            case -16777088: return 4;
            case -8388608: return 5;
            case -16744320: return 6;
            case -8355712: return 8;
            case -16777216: return image.getRGB(cx + 1, cy + 1) == -65536 ? RED_MINE : MINE;
            default: //throw new RuntimeException("识别不出来");
                return RED_MINE;
        }
    }

    private int getYellowFaceState(BufferedImage image) {
        if (image.getRGB(image.getWidth() / 2, 21) == -16777216) return LOSE;
        return image.getRGB(image.getWidth() / 2, 16) == -16777216 ? WIN : PROCESS;
    }

    private Point getScreenPosition(int x, int y) {
        return new Point(y * 16 + 7 + OFFSET_X + boardPosition.x,
                x * 16 + 7 + OFFSET_Y + boardPosition.y);
    }

    public static class WindowOccludedException extends RuntimeException {
        public WindowOccludedException() {
            super("扫雷窗口可能被遮挡或移动!");
        }

        public WindowOccludedException(BufferedImage image) {
            super("扫雷窗口可能被遮挡! 问题截图 cap.png 已保存在当前目录.");
            try {
                ImageIO.write(image, "png", new File("cap.png"));
            } catch (IOException e) {
                System.err.println("截图生成失败.");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        new WinXpSweeper();
    }
}
