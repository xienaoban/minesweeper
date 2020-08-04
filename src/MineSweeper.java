import java.awt.*;
import java.util.*;
import java.util.List;

public class MineSweeper {
    // 游戏状态
    public static final int PROCESS = 0;
    public static final int WIN     = 1;
    public static final int LOSE    = -1;

    // 三种预设难度
    public static final int DIFFICULTY_BEGINNER     = 1001;
    public static final int DIFFICULTY_INTERMEDIATE = 1002;
    public static final int DIFFICULTY_EXPERT       = 1003;
    public static final int DIFFICULTY_CUSTOM       = 1004;

    // 玩家视图中每个格子的状态
    public static final int UNCHECKED = 11;
    public static final int FLAG      = 101;
    public static final int QUESTION  = 102;
    public static final int MINE      = 111;
    public static final int NOT_MINE  = 112;
    public static final int RED_MINE  = 113;
    public static final int GRAY_MINE = 114;

    // 三种游戏规则 (WinXP: 第一步必不为雷; Win7: 第一步周围九格均必不为雷; Unknown: 无规则, 预设雷区)
    public static final int GAME_RULE_WIN_XP = 20011025;
    public static final int GAME_RULE_WIN_7  = 20091022;
    public static final int GAME_RULE_UNKNOWN  = 20200101;

    // 游戏内部变量
    protected int state;                                  // 游戏状态 (胜/负/进行中)
    protected boolean cheat, showMine;                    // 作弊与否、是否显示地雷 (需要作弊)
    protected int row, col, mineCount;                    // 行、列数、地雷总数
    protected int gameRule;                               // 游戏规则 (WinXP、Win7)
    protected boolean[][] mineBoard;                      // 地雷视图 (true 为雷, false 非雷)
    protected int[][] playerBoard, lastPlayerBoard;       // 当前的玩家视图, 上一步的玩家视图 (用于撤销)
    protected int coveredCellLeft;                        // 剩余的非雷未知格子 (UNCHECKED 且不是雷的格子)
    protected int mineLeft;                               // 剩余的雷 (= 地雷总数 - 小旗数, 所以可为负数)
    protected int step;                                   // 执行了多少步数 (揭开、标旗、标问号等操作均算一步)
    protected static boolean allowQuestionMark = true;    // 右键时是否支持标记问号

    protected MineSweeper() {}

    /**
     * 最简洁的构造函数, 从预设难度创建游戏. 默认作弊关闭、WinXP 版本规则
     * @param difficulty 难度 (三个宏): DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_EXPERT
     */
    public MineSweeper(int difficulty) {
        this(difficulty, false, GAME_RULE_WIN_XP);
    }

    /**
     * 构造函数, 从缺省难度创建游戏. 默认 WinXP 版本规则
     * @param difficulty 难度 (三个宏): DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_EXPERT
     * @param cheat 作弊与否
     */
    public MineSweeper(int difficulty, boolean cheat) {
        this(difficulty, cheat, GAME_RULE_WIN_XP);
    }

    /**
     * 构造函数, 从缺省难度创建游戏. 默认作弊关闭
     * @param difficulty 难度 (三个宏): DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_EXPERT
     * @param gameRule 游戏规则 (两个宏): GAME_RULE_WIN_XP、GAME_RULE_WIN_7
     */
    public MineSweeper(int difficulty, int gameRule) {
        this(difficulty, false, gameRule);
    }

    /**
     * 构造函数, 从缺省难度创建游戏
     * @param difficulty 难度 (三个宏): DIFFICULTY_BEGINNER, DIFFICULTY_INTERMEDIATE, DIFFICULTY_EXPERT
     * @param cheat 作弊与否
     * @param gameRule 游戏规则 (两个宏): GAME_RULE_WIN_XP、GAME_RULE_WIN_7
     */
    public MineSweeper(int difficulty, boolean cheat, int gameRule) {
        switch (difficulty) {
            case DIFFICULTY_BEGINNER:
                this.initGame(9, 9, 10, cheat, null, gameRule); break;
            case DIFFICULTY_INTERMEDIATE:
                this.initGame(16, 16, 40, cheat, null, gameRule); break;
            case DIFFICULTY_EXPERT:
                this.initGame(16, 30, 99, cheat, null, gameRule); break;
        }
    }

    /**
     * 构造函数, 指定宽、高、雷, 默认作弊关闭、WinXP 版本规则
     * @param row 行数
     * @param col 列数
     * @param mineCount 雷数
     */
    public MineSweeper(int row, int col, int mineCount) {
        this(row, col, mineCount, false, GAME_RULE_WIN_XP);
    }

    /**
     * 构造函数, 默认关闭作弊
     * @param row 行数
     * @param col 列数
     * @param mineCount 雷数
     * @param gameRule 游戏规则版本
     */
    public MineSweeper(int row, int col, int mineCount, int gameRule) {
        this(row, col, mineCount, false, gameRule);
    }

    /**
     * 构造函数, 默认 WinXP 版本规则
     * @param row 行数
     * @param col 列数
     * @param mineCount 雷数
     * @param cheat 作弊与否
     */
    public MineSweeper(int row, int col, int mineCount, boolean cheat) {
        this(row, col, mineCount, cheat, GAME_RULE_WIN_XP);
    }

    /**
     * 最全的构造函数, 全部自己指定
     * @param row 行数
     * @param col 列数
     * @param mineCount 雷数
     * @param cheat 作弊与否
     * @param gameRule 游戏规则版本
     */
    public MineSweeper(int row, int col, int mineCount, boolean cheat, int gameRule) {
        this.initGame(row, col, mineCount, cheat, null, gameRule);
    }

    /**
     * 手动指定雷的位置, 将会默认开启作弊
     * 地雷视图包含了行、列、雷信息, 无需指定. 游戏规则在此无效, 第一步就可能触雷.
     * @param mineBoard 地雷视图
     */
    public MineSweeper(boolean[][] mineBoard) {
        int mineCount = 0;
        for (boolean[] i : mineBoard) for (boolean j : i) {
            if (j) ++mineCount;
        }
        this.initGame(mineBoard.length, mineBoard[0].length, mineCount, true, mineBoard, GAME_RULE_UNKNOWN);
    }

    /**
     * 所有构造函数调用的初始化方法
     * @param row 行数
     * @param col 列数
     * @param mineCount 雷数
     * @param cheat 作弊与否
     * @param mineBoard 地雷视图
     * @param gameRule 游戏规则版本
     */
    protected void initGame(int row, int col, int mineCount, boolean cheat, boolean[][] mineBoard, int gameRule) {
        this.state = PROCESS;
        this.row = row;
        this.col = col;
        this.mineCount = mineCount;
        this.gameRule = gameRule;
        this.cheat = cheat;
        this.showMine = false;
        this.mineBoard = mineBoard;
        this.playerBoard = new int[this.row][this.col];
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j)
            this.playerBoard[i][j] = UNCHECKED;
        this.lastPlayerBoard = null;
        this.coveredCellLeft = this.row * this.col - this.mineCount;
        this.mineLeft = this.mineCount;
        this.step = 0;
    }

    /**
     * 初始化地雷视图
     * 如果没指定地雷视图, 本方法将在点下左键第一步时触发 (右键不会触发).
     * WinXP 规则下要求 mineCount < row * col - 1;
     * Win7 规则下要求 mineCount < row * col - 9.
     * 但是我这里并没有做自检, 因为还没想好不符合规则时应该怎么优雅地报错.
     * @param x 第一步左击的 x 坐标
     * @param y 第一步左击的 y 坐标
     */
    private void initRandomMineBoard(int x, int y) {
        this.mineBoard = new boolean[this.row][this.col];
        final int radius = this.gameRule == GAME_RULE_WIN_7 ? 2 : 1;
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

    /**
     * 计算目标格子周围一圈格子 (介于 0 ~ 8 格之间) 的雷数
     * 在揭开棋盘时被调用.
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     * @return 目标格子周围一圈的雷数
     */
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

    /**
     * 将某个未被揭开的格子标记为地雷 (即鼠标右键的插旗)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     */
    private void setFlag(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state != PROCESS) return;
        if (this.playerBoard[x][y] == UNCHECKED || this.playerBoard[x][y] == QUESTION) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = FLAG;
            --this.mineLeft;
        }
    }

    /**
     * 将某个被标旗的格子取消标记 (即鼠标右键的插旗)
     * 同时剩余雷数会加一
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     */
    private void unsetFlag(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state == PROCESS && this.playerBoard[x][y] == FLAG) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = UNCHECKED;
            ++this.mineLeft;
        }
    }

    /**
     * 将一个未知或被标旗的格子设为问号格子  (即两次鼠标右键)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     */
    private void setQuestion(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state != PROCESS) return;
        if (this.playerBoard[x][y] == UNCHECKED || this.playerBoard[x][y] == FLAG) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = QUESTION;
        }
    }

    /**
     * 将一个问号格子设为未知格子
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     */
    private void unsetQuestion(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state == PROCESS && this.playerBoard[x][y] == QUESTION) {
            this.recordLastPlayerBoard();
            ++this.step;
            this.playerBoard[x][y] = UNCHECKED;
        }
    }

    /**
     * 游戏胜利或结束后, 更新游戏状态并公布所有的雷
     * @param state 最终游戏状态
     * @return 最终游戏状态
     */
    private int endAndPublishMineBoard(int state) {
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
            if (this.playerBoard[i][j] == FLAG && !this.mineBoard[i][j]) this.playerBoard[i][j] = NOT_MINE;
            else if ((this.playerBoard[i][j] == UNCHECKED || this.playerBoard[i][j] == QUESTION)
                    && this.mineBoard[i][j]) this.playerBoard[i][j] = MINE;
        }
        return this.state = state;
    }

    /**
     * 记录上一步, 便于之后撤销
     * 如你所见, 撤销只支持撤销 1 步, 不支持连续撤销 2 步及以上.
     * 毕竟除非触雷了, 平时也没什么好撤销的.
     */
    private void recordLastPlayerBoard() { this.lastPlayerBoard = this.getPlayerBoard(); }

    /**
     * 撤销回上一步 (只有开了作弊才允许撤销)
     * 如你所见, 撤销只支持撤销 1 步, 不支持连续撤销 2 步及以上.
     * 毕竟除非触雷了, 平时也没什么好撤销的.
     */
    public void undo() {
        if (!this.cheat || this.lastPlayerBoard == null) return;
        this.playerBoard = this.lastPlayerBoard;
        this.lastPlayerBoard = null;
        this.state = PROCESS;
        this.coveredCellLeft = this.row * this.col - this.mineCount;
        this.mineLeft = this.mineCount;
        for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
            if (this.playerBoard[i][j] < 9) --this.coveredCellLeft;
            else if (this.playerBoard[i][j] == FLAG) --mineLeft;
        }
    }

    /**
     * 挖掘某个未知的格子 (即鼠标左键)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     * @return 执行该操作后的游戏状态
     */
    public int dig(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state != PROCESS
                || (this.playerBoard[x][y] != UNCHECKED && this.playerBoard[x][y] != QUESTION)) return this.state;
        this.recordLastPlayerBoard();
        ++this.step;

        if (this.mineBoard == null) this.initRandomMineBoard(x, y);
        if (this.mineBoard[x][y]) {
            this.playerBoard[x][y] = RED_MINE;
            return this.endAndPublishMineBoard(LOSE);
        }

        Queue<Point> queue = new LinkedList<>();
        queue.offer(new Point(x, y));
        while (!queue.isEmpty()) {
            Point p = queue.poll();
            if (this.playerBoard[p.x][p.y] != UNCHECKED && this.playerBoard[p.x][p.y] != QUESTION) continue;
            this.playerBoard[p.x][p.y] = this.calculateValue(p.x, p.y);
            --this.coveredCellLeft;
            if (this.playerBoard[p.x][p.y] != 0) continue;

            for (Point pa : this.getAround(p.x, p.y)) {
                if (this.playerBoard[pa.x][pa.y] == UNCHECKED || this.playerBoard[pa.x][pa.y] == QUESTION) {
                    queue.offer(new Point(pa.x, pa.y));
                }
            }
        }
        if (this.coveredCellLeft == 0) this.endAndPublishMineBoard(WIN);
        return this.state;
    }

    /**
     * 检查一个已被揭开的格子周围 8 格并自动揭开 (即鼠标左右键同时点击)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     * @return 执行该操作后的游戏状态
     */
    public int check(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state != PROCESS || this.playerBoard[x][y] > 8) return this.state;

        List<Point> around = this.getAround(x, y);

        int flagCount = 0;
        for (Point point : around) {
            if (this.playerBoard[point.x][point.y] == FLAG) ++flagCount;
        }
        if (flagCount != this.playerBoard[x][y]) return this.state;
        this.recordLastPlayerBoard();
        ++this.step;

        boolean fail = false;
        for (Point p : around) {
            if ((this.playerBoard[p.x][p.y] == UNCHECKED || this.playerBoard[p.x][p.y] == QUESTION)
                    && this.mineBoard[p.x][p.y]) {
                fail = true;
                this.playerBoard[p.x][p.y] = RED_MINE;
            }
            else if (this.playerBoard[p.x][p.y] == FLAG && !this.mineBoard[p.x][p.y]) {
                fail = true;
                this.playerBoard[p.x][p.y] = NOT_MINE;
            }
        }
        if (fail) return this.endAndPublishMineBoard(LOSE);

        for (Point point : around) this.dig(point.x, point.y);
        return this.state;
    }

    /**
     * 循环标旗、标问号、取消所有标记 (即鼠标右键)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     * @return 执行该操作后的游戏状态
     */
    public int mark(int x, int y) {
        this.pointRangeCheck(x, y);
        if (this.state != PROCESS) return this.state;
        switch (this.playerBoard[x][y]) {
            case UNCHECKED: this.setFlag(x, y); break;
            case FLAG: this.unsetFlag(x, y); if (allowQuestionMark) this.setQuestion(x, y); break;
            case QUESTION: this.unsetQuestion(x, y); break;
        }
        return this.state;
    }

    /**
     * 为 AI 特供的高速挖掘接口. 将不必要的更新挪到 lazyUpdate() 统一处理. 在本类中无作用, 主要是为 WinXpSweeper 提供的.
     * @param x x
     * @param y x
     */
    public void quickDig(int x, int y) { this.dig(x, y); }

    /**
     * 为 AI 特供的高速插旗接口. 将不必要的更新挪到 lazyUpdate() 统一处理. 在本类中无作用, 主要是为 WinXpSweeper 提供的.
     * @param x x
     * @param y x
     */
    public void quickFlag(int x, int y) { this.setFlag(x, y); }

    /**
     * 为 AI 特供的高速接口. 懒惰加载, 统一处理棋盘的更新. 在本类中无作用, 主要是为 WinXpSweeper 提供的.
     */
    public void lazyUpdate() {}

    // 一些游戏信息变量的 get 方法 (如果要返回对象, 会返回一个副本以防止篡改. 同时有些变量在非作弊状态下不允许获取)
    public int getGameState() { return this.state; }
    public int getRow() { return this.row; }
    public int getCol() { return this.col; }
    public int getMineCount() { return this.mineCount; }
    public int getMineLeft() { return this.mineLeft; }
    public int getUncheckedCellLeft() { return this.coveredCellLeft + this.mineLeft; }
    public int getStep() { return this.step; }
    public int getGameRule() { return this.gameRule; }
    public boolean getCheat() { return this.cheat; }

    public int getPlayerBoard(int x, int y) { return this.getPlayerBoard(x, y, false); }
    public int getPlayerBoard(int x, int y, boolean showMineIfAllowed) {
        this.pointRangeCheck(x, y);
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

    /**
     * 获得某格子周围一圈所有格子 (由于有边界的存在, 周围格子在 0 ~ 8 格之间)
     * (也不知道我当时怎么想的, 两个 for 遍历一下周围 9 个格子不就好了, 整这么麻烦)
     * @param x 目标格子的 x 坐标
     * @param y 目标格子的 y 坐标
     * @return 周围一圈的所有格子
     */
    public List<Point> getAround(int x, int y) {
        this.pointRangeCheck(x, y);
        List<Point> around = new ArrayList<>();
        boolean up    = x - 1 >= 0;
        boolean down  = x + 1 < this.row;
        boolean left  = y - 1 >= 0;
        boolean right = y + 1 < this.col;
        if (up && left)    around.add(new Point(x - 1, y - 1));
        if (up)            around.add(new Point(x - 1, y    ));
        if (up && right)   around.add(new Point(x - 1, y + 1));
        if (left)          around.add(new Point(x    , y - 1));
        if (right)         around.add(new Point(x    , y + 1));
        if (down && left)  around.add(new Point(x + 1, y - 1));
        if (down)          around.add(new Point(x + 1, y    ));
        if (down && right) around.add(new Point(x + 1, y + 1));
        return around;
    }

    /**
     * 计算格子坐标是否是否越界
     * @param x 待判定的 x 坐标
     * @param y 待判定的 y 坐标
     * @return 是否在界内
     */
    public boolean isPointInRange(int x, int y) {
        return x >= 0 && x < this.row && y >= 0 && y < this.col;
    }

    /**
     * 检查格子坐标是否是否越界 (越界则抛出异常)
     * @param x 待判定的 x 坐标
     * @param y 待判定的 y 坐标
     */
    public void pointRangeCheck(int x, int y) {
        if (!this.isPointInRange(x, y)) throw new PointOutOfBoundsException(x, y, this.row, this.col);
    }

    /**
     * 设置是否在玩家视图上显示地雷 (需要作弊. 有雷的未知格子会被标记为 GRAY_MINE)
     * @param flag 显示与否
     */
    public void setShowMine(boolean flag) { if (cheat) this.showMine = flag; }
    public boolean getShowMine() { return this.showMine; }

    public static void setAllowQuestionMark(boolean flag) { allowQuestionMark = flag; }
    public static boolean getAllowQuestionMark() { return allowQuestionMark; }

    /**
     * 控制台输出地雷视图
     */
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

    /**
     * 控制台输出玩家视图
     */
    public void printPlayerBoardToConsole() {
        if (this.cheat && this.mineBoard == null) System.out.println("null");
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

    public static class PointOutOfBoundsException extends RuntimeException {
        public PointOutOfBoundsException(int x, int y, int row, int col) {
            super("Point (" + x + ", " + y + ") is out of range (0, 0, "
                    + row + ", " + col + ").");
        }
    }
}
