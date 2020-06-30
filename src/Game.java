import javafx.util.Pair;
import java.util.*;

public class Game {
    // 游戏状态
    public static final int WIN = 1;
    public static final int LOSE = -1;
    public static final int PROCESS = 0;

    // 玩家视图中每个格子的状态
    public static final int UNCHECKED = 11;
    public static final int FLAG = 101;
    public static final int QUESTION = 102;
    public static final int MINE = 111;
    public static final int NOT_MINE = 112;
    public static final int RED_MINE = 113;
    public static final int GRAY_MINE = 114;

    // 游戏内部变量
    private int state;                              // 游戏状态（胜/负/进行中）
    private boolean cheat, showMine, win7Mode;      // 作弊与否、是否显示地雷（需要作弊）、模式（WinXP 或 Win7）
    private int row, col;                           // 行、列数
    private int mineCount;                          // 地雷总数
    private boolean[][] mineBoard;                  // 地雷视图（true 为雷，false 非雷）
    private int[][] playerBoard, lastPlayerBoard;   // 当前的玩家视图，上一步的玩家视图（用于撤销）
    private int clearCellLeft;                      // 剩余的未知格子（UNCHECKED 的格子）
    private int mineLeft;                           // 剩余的雷（= 地雷总数 - 小旗数，所以可为负数）
    private int step;                               // 执行了多少步数（揭开、标旗、标问号等操作均算一步）

    public Game(int row, int col, int mineCount) {
        this.initGame(row, col, mineCount, false, null);
    }

    public Game(int row, int col, int mineCount, boolean cheat) {
        this.initGame(row, col, mineCount, cheat, null);
    }

    public Game(boolean[][] mineBoard) {
        int mineCount = 0;
        for (boolean[] i : mineBoard) for (boolean j : i) {
            if (j) ++mineCount;
        }
        this.initGame(mineBoard.length, mineBoard[0].length, mineCount, true, mineBoard);
    }

    private void initGame(int row, int col, int mineCount, boolean cheat, boolean[][] mineBoard) {
        this.state = PROCESS;
        this.row = row;
        this.col = col;
        this.mineCount = mineCount;
        this.cheat = cheat;
        this.showMine = false;
        this.mineBoard = mineBoard;
        this.playerBoard = new int[this.row][this.col];
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j)
            this.playerBoard[i][j] = UNCHECKED;
        this.lastPlayerBoard = null;
        this.clearCellLeft = this.row * this.col - this.mineCount;
        this.mineLeft = this.mineCount;
        this.step = 0;
        this.win7Mode = true;
    }

    private void initRandomMineBoard(int x, int y) {
        this.mineBoard = new boolean[this.row][this.col];
        final int radius = this.win7Mode ? 2 : 1;
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < this.row * this.col; ++i) list.add(i);
        Collections.shuffle(list);
        int mine = this.mineCount;
        for (int p : list) {
            int px = p / this.col, py = p % this.col;
            if (Math.abs(px - x) < radius && Math.abs(py - y) < radius) continue;
            this.mineBoard[px][py] = true;
            if (--mine <= 0) break;
        }
    }

    private int calculateValue(int x, int y) {
        int cnt = 0;
        if (x - 1 >= 0) {
            if (y - 1 >= 0 && this.mineBoard[x - 1][y - 1]) ++cnt;
            if (this.mineBoard[x - 1][y]) ++cnt;
            if (y + 1 < this.col && this.mineBoard[x - 1][y + 1]) ++cnt;
        }
        if (y - 1 >= 0 && this.mineBoard[x][y - 1]) ++cnt;
        if (y + 1 < this.col && this.mineBoard[x][y + 1]) ++cnt;
        if (x + 1 < this.row) {
            if (y - 1 >= 0 && this.mineBoard[x + 1][y - 1]) ++cnt;
            if (this.mineBoard[x + 1][y]) ++cnt;
            if (y + 1 < this.col && this.mineBoard[x + 1][y + 1]) ++cnt;
        }
        return cnt;
    }

    private int endAndPublishMineBoard(int state) {
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
            if (this.playerBoard[i][j] == FLAG && !this.mineBoard[i][j]) this.playerBoard[i][j] = NOT_MINE;
            else if (this.playerBoard[i][j] == UNCHECKED && this.mineBoard[i][j]) this.playerBoard[i][j] = MINE;
        }
        return this.state = state;
    }

    private void recordLastPlayerBoard() {
        this.lastPlayerBoard = this.getPlayerBoard();
    }

    public void undo() {
        if (!this.cheat || this.lastPlayerBoard == null) return;
        this.playerBoard = this.lastPlayerBoard;
        this.lastPlayerBoard = null;
        this.state = PROCESS;
        this.clearCellLeft = this.row * this.col - this.mineCount;
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
            if (this.playerBoard[i][j] < 9) --this.clearCellLeft;
        }
    }

    public int uncover(int x, int y) {
        if (this.state != PROCESS || !this.isXYLegal(x, y) || this.playerBoard[x][y] != UNCHECKED) return this.state;
        this.recordLastPlayerBoard();
        ++this.step;

        if (this.mineBoard == null) this.initRandomMineBoard(x, y);
        if (this.mineBoard[x][y]) {
            this.playerBoard[x][y] = RED_MINE;
            return this.endAndPublishMineBoard(LOSE);
        }

        Queue<Pair<Integer, Integer>> queue = new LinkedList<>();
        queue.offer(new Pair<>(x, y));
        while (!queue.isEmpty()) {
            Pair<Integer, Integer> point = queue.poll();
            x = point.getKey();
            y = point.getValue();
            if (this.playerBoard[x][y] != UNCHECKED) continue;
            this.playerBoard[x][y] = this.calculateValue(x, y);
            --this.clearCellLeft;
            if (this.playerBoard[x][y] != 0) continue;

            for (Pair<Integer, Integer> p : this.getAround(x, y)) {
                int px = p.getKey();
                int py = p.getValue();
                if (this.playerBoard[px][py] == UNCHECKED) queue.offer(new Pair<>(px, py));
            }
        }
        if (this.clearCellLeft == 0) this.endAndPublishMineBoard(WIN);
        return this.state;
    }

    public int setFlag(int x, int y) {
        if (this.state != PROCESS) return this.state;
        if (this.isXYLegal(x, y) && (this.playerBoard[x][y] == UNCHECKED || this.playerBoard[x][y] == QUESTION)) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = FLAG;
            --this.mineLeft;
        }
        return this.state;
    }

    public int unsetFlag(int x, int y) {
        if (this.state != PROCESS) return this.state;
        if (this.isXYLegal(x, y) && this.playerBoard[x][y] == FLAG) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = UNCHECKED;
            ++this.mineLeft;
        }
        return this.state;
    }

    public int setQuestion(int x, int y) {
        if (this.state != PROCESS) return this.state;
        if (this.isXYLegal(x, y) && (this.playerBoard[x][y] == UNCHECKED || this.playerBoard[x][y] == FLAG)) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = QUESTION;
        }
        return this.state;
    }

    public int unsetQuestion(int x, int y) {
        if (this.state != PROCESS) return this.state;
        if (this.isXYLegal(x, y) && this.playerBoard[x][y] == QUESTION) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = UNCHECKED;
        }
        return this.state;
    }

    public int check(int x, int y) {
        if (this.state != PROCESS) return this.state;
        if (!this.isXYLegal(x, y) || this.playerBoard[x][y] > 8) return this.state;

        List<Pair<Integer, Integer>> around = this.getAround(x, y);

        int flagCount = 0;
        for (Pair<Integer, Integer> point : around) {
            if (this.playerBoard[point.getKey()][point.getValue()] == FLAG) ++flagCount;
        }
        if (flagCount != this.playerBoard[x][y]) return this.state;
        this.recordLastPlayerBoard();
        ++this.step;

        boolean fail = false;
        for (Pair<Integer, Integer> point : around) {
            int px = point.getKey();
            int py = point.getValue();
            if (this.playerBoard[px][py] == UNCHECKED && this.mineBoard[px][py]) {
                fail = true;
                this.playerBoard[px][py] = RED_MINE;
            }
            else if (this.playerBoard[px][py] == FLAG && !this.mineBoard[px][py]) {
                fail = true;
                this.playerBoard[px][py] = NOT_MINE;
            }
        }
        if (fail) return this.endAndPublishMineBoard(LOSE);

        for (Pair<Integer, Integer> point : around) this.uncover(point.getKey(), point.getValue());
        return this.state;
    }

    public int cycFlagAndQuestion(int x, int y) {
        if (this.state != PROCESS || !this.isXYLegal(x, y)) return this.state;
        switch (this.playerBoard[x][y]) {
            case UNCHECKED: this.setFlag(x, y); break;
            case FLAG: this.unsetFlag(x, y); this.setQuestion(x, y); break;
            case QUESTION: this.unsetQuestion(x, y); break;
        }
        return this.state;
    }

    public int getGameState() { return this.state; }
    public int getRow() { return this.row; }
    public int getCol() { return this.col; }
    public int getMineCount() { return this.mineCount; }
    public int getMineLeft() { return this.mineLeft; }
    public int getStep() {return this.step; }

    public int getPlayerBoard(int x, int y) { return this.getPlayerBoard(x, y, false); }
    public int getPlayerBoard(int x, int y, boolean showMineIfAllowed) {
        if (showMineIfAllowed && this.cheat && this.showMine && this.getMineBoard(x, y) && this.playerBoard[x][y] == UNCHECKED) return GRAY_MINE;
        return this.playerBoard[x][y];
    }

    public int[][] getPlayerBoard() { return this.getPlayerBoard(false); }
    public int[][] getPlayerBoard(boolean showMineIfAllowed) {
        int[][] board = new int[this.row][this.col];
        for (int i = 0; i < this.row; ++i)
            if (this.col >= 0) System.arraycopy(this.playerBoard[i], 0, board[i], 0, this.col);
        if (showMineIfAllowed && this.cheat && this.showMine) {
            for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
                if (this.getMineBoard(i, j) && board[i][j] == UNCHECKED) board[i][j] = GRAY_MINE;
            }
        }
        return board;
    }

    public boolean getMineBoard(int x, int y) {
        if (this.cheat && this.mineBoard != null) return this.mineBoard[x][y];
        return false;
    }

    public boolean[][] getMineBoard() {
        if (!this.cheat || this.mineBoard == null) return null;
        boolean[][] board = new boolean[this.row][this.col];
        for (int i = 0; i < this.row; ++i)
            if (this.col >= 0) System.arraycopy(this.mineBoard[i], 0, board[i], 0, this.col);
        return board;
    }

    public List<Pair<Integer, Integer>> getAround(int x, int y) {
        List<Pair<Integer, Integer>> around = new ArrayList<>();
        if (!isXYLegal(x, y)) return around;

        boolean up    = x - 1 >= 0;
        boolean down  = x + 1 < this.row;
        boolean left  = y - 1 >= 0;
        boolean right = y + 1 < this.col;
        if (up && left)     around.add(new Pair<>(x - 1, y - 1));
        if (up)             around.add(new Pair<>(x - 1, y    ));
        if (up && right)    around.add(new Pair<>(x - 1, y + 1));
        if (left)           around.add(new Pair<>(x    , y - 1));
        if (right)          around.add(new Pair<>(x    , y + 1));
        if (down && left)   around.add(new Pair<>(x + 1, y - 1));
        if (down)           around.add(new Pair<>(x + 1, y    ));
        if (down && right)  around.add(new Pair<>(x + 1, y + 1));
        return around;
    }

    public boolean isXYLegal(int x, int y) {
        return x >= 0 && x < this.row && y >= 0 && y < this.col;
    }

    public void setShowMine(boolean flag) { if (cheat) this.showMine = flag; }
    public boolean getShowMine() { return this.showMine; }

    public void printMineBoardToConsole() {
        if (!this.cheat || this.mineBoard == null) System.out.println("null");
        else {
            for (int i = 0; i < this.row; ++i) {
                for (int j = 0; j < this.col; ++j) {
                    if (this.mineBoard[i][j]) System.out.print(" * ");
                    else if (this.playerBoard[i][j] == UNCHECKED) System.out.print("   ");
                    else if (this.playerBoard[i][j] == FLAG) System.out.print(" F ");
                    else if (this.playerBoard[i][j] == QUESTION) System.out.print(" ? ");
                    else if (this.playerBoard[i][j] == MINE) System.out.print(" * ");
                    else if (this.playerBoard[i][j] == NOT_MINE) System.out.print(" X ");
                    else if (this.playerBoard[i][j] == RED_MINE) System.out.print(" O ");
                    else System.out.print(" " + this.playerBoard[i][j] + " ");
                }
                System.out.println();
            }
        }
    }

    public void printPlayerBoardToConsole() {
        if (this.mineBoard == null) System.out.println("null");
        else {
            for (int i = 0; i < this.col; ++i) System.out.print("---");
            System.out.println();
            for (int[] i : this.playerBoard) {
                for (int j : i) {
                    switch (j) {
                        case UNCHECKED: System.out.print("   "); break;
                        case FLAG:      System.out.print(" F "); break;
                        case QUESTION:  System.out.print(" ? "); break;
                        case MINE:      System.out.print(" * "); break;
                        case NOT_MINE:  System.out.print(" X "); break;
                        case RED_MINE:  System.out.print(" O "); break;
                        default: System.out.print(" " + j + " "); break;
                    }
                }
                System.out.println();
            }
            for (int i = 0; i < this.col; ++i) System.out.print("---");
            System.out.println();
        }
    }
}
