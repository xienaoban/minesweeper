import javafx.util.Pair;

import java.math.BigDecimal;
import java.util.*;

public class AI {
    // AI 给出的三种判断
    public static final int UNKNOWN = 0;
    public static final int MINE = 1;
    public static final int NOT_MINE = -1;

    // 连通分量视图的标记
    public static final int CC_VISITED = -233;
    public static final int CC_UNKNOWN = 0;

    // 二维数组的 [n][m] 表示当 n 个未知格子有 m 个雷时, 有多少种可能的情况
    // 用 long 可能会溢出, 但同时后面会用到浮点除法, 所以又不能用 BigInteger, 于是选用了 BigDecimal
    private static final ArrayList<ArrayList<BigDecimal>> numOfCasesForGivenCellsAndMines;

    static {
        // 初始化 numOfCasesForGivenMinesAndCells
        numOfCasesForGivenCellsAndMines = new ArrayList<>(18 * 32);
        ArrayList<BigDecimal> zero = new ArrayList<>(1);
        zero.add(new BigDecimal(1));
        numOfCasesForGivenCellsAndMines.add(zero);
        getNumOfCasesForGivenCellsAndMines(16 * 30, 99);
    }

    /**
     * 仅通过检测周围8格, 确定已知数字的格子周围剩下的未知格子是否全部为雷
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 周围的 Unchecked 格子全为 (或全不为) 雷、或未知
     */
    public static int checkOneUncoveredCell(Game game, int x, int y) {
        if (game.getPlayerBoard(x, y) > 8) return UNKNOWN;
        int uncheckedOrQuestion = 0, flag = 0;
        List<Pair<Integer, Integer>> around = game.getAround(x, y);
        for (Pair<Integer, Integer> p : around) {
            switch (game.getPlayerBoard(p.getKey(), p.getValue())) {
                case Game.UNCHECKED:
                case Game.QUESTION: ++uncheckedOrQuestion; break;
                case Game.FLAG: ++flag; break;
            }
        }
        if (uncheckedOrQuestion == 0) return UNKNOWN;
        if (game.getPlayerBoard(x, y) == flag) return NOT_MINE;
        if (game.getPlayerBoard(x, y) == flag + uncheckedOrQuestion) return MINE;
        return UNKNOWN;
    }

    /**
     * 使用减法公式, 检测紧挨着的两个已知数字格子两边的未知格子是否可判断
     * @param game 一局游戏
     * @param x1 第一个目标格子 x 坐标
     * @param y1 第一个目标格子 y 坐标
     * @param x2 第二个目标格子 x 坐标
     * @param y2 第二个目标格子 y 坐标
     * @return 返回以 Pair 存储的两个值, key 和 value 分别为「可揭开的格子列表」与「可标雷的格子列表」
     */
    public static Pair<List<Pair<Integer, Integer>>, List<Pair<Integer, Integer>>> checkTwoUncoveredCell(
            Game game, int x1, int y1, int x2, int y2) {
        int num1 = game.getPlayerBoard(x1, y1), num2 = game.getPlayerBoard(x2, y2);
        if (num1 > 8 || num2 > 8) return null;
        int diffX = x2 - x1, diffY = y2 - y1;
        if (Math.abs(diffX) + Math.abs(diffY) != 1) return null;
        List<Pair<Integer, Integer>> around1 = new ArrayList<>(3);
        List<Pair<Integer, Integer>> around2 = new ArrayList<>(3);
        for (int i = -1; i < 2; ++i) {
            int xx1 = x1 - diffX + diffY * i, yy1 = y1 - diffY + diffX * i;
            if (game.isPointInRange(xx1, yy1)) {
                int pp1 = game.getPlayerBoard(xx1, yy1);
                if (pp1 == Game.FLAG) --num1;
                else if (pp1 == Game.UNCHECKED || pp1 == Game.QUESTION) around1.add(new Pair<>(xx1, yy1));
            }

            int xx2 = x2 + diffX + diffY * i, yy2 = y2 + diffY + diffX * i;
            if (game.isPointInRange(xx2, yy2)) {
                int pp2 = game.getPlayerBoard(xx2, yy2);
                if (pp2 == Game.FLAG) --num2;
                else if (pp2 == Game.UNCHECKED || pp2 == Game.QUESTION) around2.add(new Pair<>(xx2, yy2));
            }
        }
        Pair<List<Pair<Integer, Integer>>, List<Pair<Integer, Integer>>> res = null;
        if (num2 - num1 - around2.size() == 0) res = new Pair<>(around1, around2);
        else if (num1 - num2 - around1.size() == 0) res = new Pair<>(around2, around1);
        return res;
    }

    /**
     * 仅通过检测周围8格, 确定目标未知格子是否有雷
     * 要先找出周围所有数字格子, 再逐个判断数字格子周围8格, 最终判断范围达到了 5*5 格, 不建议使用该函数
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return AI认为是有、无雷还是未知
     */
    @Deprecated
    public static int checkUncheckedCellBasic(Game game, int x, int y) {
        if (game.getPlayerBoard(x, y) != Game.UNCHECKED
                && game.getPlayerBoard(x, y) != Game.QUESTION) return UNKNOWN;
        List<Pair<Integer, Integer>> around = game.getAround(x, y);
        for (Pair<Integer, Integer> p : around) {
            int px = p.getKey(), py = p.getValue();
            int pState = game.getPlayerBoard(px, py);
            if (pState < 9) {
                int state = checkOneUncoveredCell(game, px, py);
                if (state != UNKNOWN) return state;
            }
        }
        return UNKNOWN;
    }

    /**
     * 扫描全盘, 仅通过单个格子或相邻两个格子, 判断是否存在必为雷或必不为雷的格子
     * @param game 一局游戏
     * @return int数组第一个值代表类型, 第二、第三个值代表坐标 (只返回找到的第一个格子)
     */
    public static int[] checkAllBasic(Game game) {
        for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
            int type = checkOneUncoveredCell(game, x, y);
            if (type != UNKNOWN) {
                for (Pair<Integer, Integer> p : game.getAround(x, y)) {
                    int px = p.getKey(), py = p.getValue();
                    if (game.getPlayerBoard(px, py) != Game.UNCHECKED
                            && game.getPlayerBoard(px, py) != Game.QUESTION) continue;
                    return new int[]{type, px, py};
                }
            }

            for (int i = 0; i < 2; ++i) {
                int x2 = x + i, y2 = y + 1 - i;
                Pair<List<Pair<Integer, Integer>>, List<Pair<Integer, Integer>>> _pair;
                try {
                    _pair = checkTwoUncoveredCell(game, x, y, x2, y2);
                } catch (Game.PointOutOfBoundsException ignored) { continue; }
                if (_pair != null) {
                    for (Pair<Integer, Integer> p : _pair.getKey()) {
                        return new int[]{NOT_MINE, p.getKey(), p.getValue()};
                    }
                    for (Pair<Integer, Integer> p : _pair.getValue()) {
                        return new int[]{MINE, p.getKey(), p.getValue()};
                    }
                }
            }
        }
        return new int[]{UNKNOWN};
    }

    /**
     * 仅通过周围八格信息, 找出所有必为雷或必不为雷的格子
     * @param game 一局游戏
     */
    public static void sweepAllBasic(Game game) {
        boolean swept;
        do {
            swept = false;
            for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
                // 根据单个格子进行判断
                int type = checkOneUncoveredCell(game, x, y);
                if (type != UNKNOWN) {
                    swept = true;
                    for (Pair<Integer, Integer> p : game.getAround(x, y)) {
                        int px = p.getKey(), py = p.getValue();
                        if (game.getPlayerBoard(px, py) != Game.UNCHECKED
                                && game.getPlayerBoard(px, py) != Game.QUESTION) continue;
                        if (type == MINE) game.setFlag(px, py);
                        else if (type == NOT_MINE) game.uncover(px, py);
                        if (game.getGameState() == Game.LOSE) return;
                    }
                }

                // 根据相邻两个格子进行判断 (减法公式)
                for (int i = 0; i < 2; ++i) {
                    int x2 = x + i, y2 = y + 1 - i;
                    if (!game.isPointInRange(x2, y2)) continue;
                    Pair<List<Pair<Integer, Integer>>, List<Pair<Integer, Integer>>> _pair
                            = checkTwoUncoveredCell(game, x, y, x2, y2);
                    if (_pair != null) {
                        if (_pair.getKey().size() + _pair.getValue().size() > 0) swept = true;
                        for (Pair<Integer, Integer> p : _pair.getKey()) {
                            game.uncover(p.getKey(), p.getValue());
                            if (game.getGameState() == Game.LOSE) return;
                        }
                        for (Pair<Integer, Integer> p : _pair.getValue()) {
                            game.setFlag(p.getKey(), p.getValue());
                        }
                    }
                }
            }
        } while (swept);
    }

    /**
     * 进阶扫雷 AI, 将所有概率为 100% 或 0% 的格子扫掉
     * @param game 一局游戏
     * @return 最后一次计算得概率后没有利用, 将其返回以重复利用
     */
    public static double[][] sweepAllAdvanced(Game game) {
        boolean loop = true;
        double[][] prob = null;
        while (loop && game.getGameState() == Game.PROCESS) {
            loop = false;
            sweepAllBasic(game);
            if (game.getGameState() != Game.PROCESS) break;
            prob = calculateAllProbabilities(game);
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                // 只扫爆雷概率为 0 的, 不标爆雷概率为 1 的.
                // 虽然概率计算应该是精确的, 但是还是有极低概率出现计算得概率为 1 但却不是雷 (猜测可能是精度问题).
                // if (prob[i][j] == 1.0 && game.getPlayerBoard(i, j) != Game.FLAG) game.setFlag(i, j);
                if (prob[i][j] == 0.0 && game.getPlayerBoard(i, j) == Game.UNCHECKED) {
                    game.uncover(i, j);
                    loop = true;
                }
            }
        }
        return prob;
    }

    /**
     * 完整地玩一局扫雷
     * 猜雷就是在本方法实现的
     * @param game 一局游戏
     */
    public static void sweepToEnd(Game game) {
        if (game.getStep() == 0) {
            // Win7 的规则下第一步点击距离角落两格的点最好
            if (game.getGameRule() == Game.GAME_RULE_WIN_7) game.uncover(2, 2);
            else game.uncover(0, 0);
        }
        while (game.getGameState() == Game.PROCESS) {
            double[][] prob = sweepAllAdvanced(game);
            if (game.getGameState() != Game.PROCESS) break;
            if (prob == null) prob = calculateAllProbabilities(game);
            int maxX = -1, maxY = -1, corner = -1, intensity = -1;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                int cellState = game.getPlayerBoard(i, j);
                if (cellState != Game.UNCHECKED && cellState != Game.QUESTION) continue;
                if (maxX != -1 && prob[i][j] > prob[maxX][maxY]) continue;
                boolean newMax = false;
                int cor = 0, in = getNumberCellCntAround(game, i, j, 3);
                if (i == 0 || i == game.getRow() - 1) ++cor;
                if (j == 0 || j == game.getCol() - 1) ++cor;
                if (maxX != -1 && prob[i][j] == prob[maxX][maxY]) {
                    // 同概率的话在角落的格子优先探测, 可以将概率从 29% 提升到 33%.
                    // 或者当两者都在 (或都不在) 角落时, 选择周围 24 格数字格子更多的.
                    if ((cor == 2 && cor > corner) || (corner / 2 == cor / 2 && in > intensity)) newMax = true;
                }
                else if (maxX == -1 || prob[i][j] < prob[maxX][maxY]) newMax = true;
                if (!newMax) continue;
                maxX = i;
                maxY = j;
                corner = cor;
                intensity = in;
            }
            // 只找 prob 低的 uncover, 不找 prob 高的 setFlag, 因为 setFlag 不影响游戏状态, 标错了也不知道.
            game.uncover(maxX, maxY);
        }
    }

    /**
     * 检查已知是数字的一个格子, 判断对其周围是否是雷的判定是否合法
     * @param game 一局游戏
     * @param board 棋局. 标记为 MINE 或 FLAG 说明判定为雷; 标记为 NOT_MINE 说明判定为非雷
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 是否合法
     */
    public static boolean isUncoveredCellLegal(Game game, int[][] board, int x, int y) {
        if (board[x][y] > 8) return false;
        List<Pair<Integer, Integer>> list = game.getAround(x, y);
        int mineCnt = 0, notMineCnt = 0, uncheckedCnt = 0;
        for (Pair<Integer, Integer> p : list) {
            switch (board[p.getKey()][p.getValue()]) {
                case Game.FLAG:
                case Game.MINE:
                case Game.RED_MINE:
                case Game.GRAY_MINE:
                    ++mineCnt;
                    break;
                case Game.NOT_MINE:
                    ++notMineCnt;
                    break;
                case Game.UNCHECKED:
                case Game.QUESTION:
                    ++uncheckedCnt;
                    break;
                default: break;
            }
        }
        if (uncheckedCnt == 0) return mineCnt == board[x][y];
        return mineCnt <= board[x][y] && mineCnt + uncheckedCnt >= board[x][y];
    }

    public static BigDecimal getNumOfCasesForGivenCellsAndMines(int cells, int mines) {
        ArrayList<ArrayList<BigDecimal>> _arr = numOfCasesForGivenCellsAndMines;
        for (int i = _arr.size(); i <= cells; ++i) {
            ArrayList<BigDecimal> arrCur = new ArrayList<>(i + 1);
            ArrayList<BigDecimal> arrPre = _arr.get(i - 1);
            _arr.add(arrCur);
            arrCur.add(arrPre.get(0));
            for (int j = 1; j < i ; ++j) {
                arrCur.add(arrPre.get(j - 1).add(arrPre.get(j)));
            }
            arrCur.add(arrPre.get(i - 1));
        }
        return mines >= 0 && mines <= cells ? _arr.get(cells).get(mines) : new BigDecimal(0);
    }

    /**
     * 寻找所有连通分量
     * 一个连通分量表示, 有多种可能性且相互影响的一个区域
     * @param game 一局游戏
     * @return Pair 的 key 储存所有分量的所有点, value 为整个图
     */
    public static Pair<List<List<Pair<Integer, Integer>>>, int[][]> findAllConnectedComponents(Game game) {
        List<List<Pair<Integer, Integer>>> ccList = new ArrayList<>();
        int[][] ccGraph = new int[game.getRow()][game.getCol()];
        int id = 1;
        // 遍历每个点, 找到第一个可能属于一个连通分量的点, 并从该点扩散开来寻找其他属于该分量的点
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if (ccGraph[i][j] != CC_UNKNOWN || game.getPlayerBoard(i, j) > 8) continue;
            // 找到了一个可能的点. 注意该点为已扫出数字的格子 (因为一个数字格子周围的未知格子必属于同一分量)
            // 该队列存储的点是已扫出数字的格子, 这些个数字格子周围的未知格子也必属于同一分量
            Queue<Pair<Integer, Integer>> que = new LinkedList<>();
            que.offer(new Pair<>(i, j));
            List<Pair<Integer, Integer>> points = new ArrayList<>();
            boolean findANewComponent = false;
            // BFS 遍历周围的点, 搜出整个连通分量
            while (!que.isEmpty()) {
                Pair<Integer, Integer> cur = que.poll();
                int cx = cur.getKey(), cy = cur.getValue();
                if (ccGraph[cx][cy] == CC_VISITED) continue;
                ccGraph[cx][cy] = CC_VISITED;
                // 遍历该数字格子周围的所有未知格子, 它们属于同一个分量
                for (Pair<Integer, Integer> p : game.getAround(cx, cy)) {
                    int px = p.getKey(), py = p.getValue();
                    if ((game.getPlayerBoard(px, py) != Game.UNCHECKED && game.getPlayerBoard(px, py) != Game.QUESTION)
                            || ccGraph[px][py] == id) continue;
                    findANewComponent = true;
                    points.add(new Pair<>(px, py));
                    ccGraph[px][py] = id;
                    // 找出「数字格子周围的未知格子」的周围的其余数字格子, 加入队列 (有点绕)
                    for (Pair<Integer, Integer> p2 : game.getAround(px, py)) {
                        if (game.getPlayerBoard(p2.getKey(), p2.getValue()) < 9) que.offer(p2);
                    }
                }
            }
            if (findANewComponent) {
                ccList.add(points);
                ++id;
            }
        }
//        printConnectedComponent(ccGraph);
        return new Pair<>(ccList, ccGraph);
    }

    /**
     * 计算每个格子有雷的概率 (默认当前每个旗子设的都是对的, 懒得自检了)
     * @param game 一局游戏
     * @return 每个格子的有雷概率
     */
    public static double[][] calculateAllProbabilities(Game game) {
        double[][] probGraph = new double[game.getRow()][game.getCol()];
        Pair<List<List<Pair<Integer, Integer>>>, int[][]> _ccPair = findAllConnectedComponents(game);
        List<List<Pair<Integer, Integer>>> ccList = _ccPair.getKey();
        int[][] ccGraph = _ccPair.getValue();
        List<Map<Integer, int[]>> ccPermList = new ArrayList<>(ccList.size());

        // 计算每个连通分量的每个点的有雷概率
        for (List<Pair<Integer, Integer>> points : ccList) {
            Map<Integer, int[]> perm = new HashMap<>(16);
            backtrackAllPossiblePermutations(game, game.getPlayerBoard(), points,
                    perm, 0, 0); // 如果 permutationCnt 为 0, 说明玩家设的旗有错, 会异常
            ccPermList.add(perm);
        }
        double avgPermMineCnt = calculateProbabilitiesOfAllConnectedComponents(game, ccList, ccPermList, probGraph);

        // 所有未知孤立格子 (即周围没有已知数字格子的格子) 统一计算概率为 "平均剩余雷数/未知孤立格子数"
        int unknownCellCnt = 0;
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if ((game.getPlayerBoard(i, j) == Game.UNCHECKED || game.getPlayerBoard(i, j) == Game.QUESTION)
                    && ccGraph[i][j] == CC_UNKNOWN) ++unknownCellCnt;
        }
        if (unknownCellCnt > 0) {
            double unknownCellProb = ((double) game.getMineLeft() - avgPermMineCnt) / (double) (unknownCellCnt);
            if (Math.abs(unknownCellProb) < 1e-5) unknownCellProb = 0.0;
            else if (Math.abs(unknownCellProb - 1.0) < 1e-5) unknownCellProb = 1.0;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                if (ccGraph[i][j] == CC_UNKNOWN) {
                    if (game.getPlayerBoard(i, j) == Game.UNCHECKED || game.getPlayerBoard(i, j) == Game.QUESTION) {
                        probGraph[i][j] = unknownCellProb;
                    }
                    else if (game.getPlayerBoard(i, j) == Game.FLAG) probGraph[i][j] = 1.0;
                }
            }
        }
//        printProbability(probGraph);
        return probGraph;
    }

    /**
     * 使用回溯法, 找出某连通分量中所有的雷的布局可能性
     * @param game 一局游戏
     * @param board 回溯时可被任意修改的棋盘 (防止在游戏本体上修改出问题)
     * @param points 一个连通分量所有的点
     * @param ccPerm 当有 key 个雷时的所有情况
     * @param curIndex 当前回溯位置的下标
     * @param curMine 当前有多少雷
     * @return 可行排列总数
     */
    private static int backtrackAllPossiblePermutations(Game game, int[][] board, List<Pair<Integer, Integer>> points,
                                                        Map<Integer, int[]> ccPerm, int curIndex, int curMine) {
        // 成功找到一个可能的排列
        if (curIndex >= points.size()) {
            int[] count; // count 前 points.size() 位保存所有情况中格子有雷的次数, 最后一位保存回溯出多少种情况
            if (ccPerm.containsKey(curMine)) count = ccPerm.get(curMine);
            else {
                count = new int[points.size() + 1];
                ccPerm.put(curMine, count);
            }
            for (int i = 0; i < points.size(); ++i) {
                int px = points.get(i).getKey(), py = points.get(i).getValue();
                if (board[px][py] == Game.MINE) ++count[i];
            }
            ++count[points.size()]; // 排列个数总数
            return 1;
        }

        // 分别递归考虑当前格子是雷、不是雷的情况
        int x = points.get(curIndex).getKey(), y = points.get(curIndex).getValue();
        int res = 0;
        board[x][y] = Game.MINE;
        if (curMine < game.getMineLeft() && isUncheckedCellLegal(game, board, x, y)) {
            res += backtrackAllPossiblePermutations(game, board, points, ccPerm, curIndex + 1, curMine + 1);
        }
        board[x][y] = Game.NOT_MINE;
        if (isUncheckedCellLegal(game, board, x, y)) {
            res += backtrackAllPossiblePermutations(game, board, points, ccPerm, curIndex + 1, curMine);
        }
        board[x][y] = Game.UNCHECKED;
        return res;
    }

    /**
     * 计算所有连通分量中各个格子有雷的精确概率 (但可能有一点点精度误差)
     * @param game 一局游戏
     * @param ccList 所有连通分量的列表
     * @param ccPermList 所有连通分量所有可能性下, 有多少个情况格子为雷
     * @param probGraph 一个空白数组, 用于返回每个格子的概率
     * @return 所有分量平均雷数
     */
    private static double calculateProbabilitiesOfAllConnectedComponents(Game game,
                                                                         List<List<Pair<Integer, Integer>>> ccList,
                                                                         List<Map<Integer, int[]>> ccPermList,
                                                                         double[][] probGraph) {
        final int unchecked = game.getUncheckedCellLeft();
        int notInCC = unchecked;
        for (List<Pair<Integer, Integer>> cc : ccList) {
            notInCC -= cc.size();
        }
        final int maxMineCnt = game.getMineLeft();
        final int minMineCnt = maxMineCnt - notInCC;
        double avgPermMineCnt = 0;

        // stack 按顺序存放: 综合前 0 ~ n-1 个分量, 计算不同雷数时这 n 个分量共有多少种组合情况 (该栈主要是为了遍历时不重复计算)
        Deque<Map<Integer, Integer>> stack = new ArrayDeque<>(ccPermList.size());
        Map<Integer, Integer> outOfRange = new HashMap<>();
        outOfRange.put(0, 1); // 0 个分量时有一种情况
        stack.addFirst(outOfRange);
        for (Map<Integer, int[]> toMerge : ccPermList) {
            Map<Integer, Integer> pre = stack.getFirst();
            Map<Integer, Integer> cur = mergeTwoPermutations(pre, toMerge, maxMineCnt, true);
            stack.addFirst(cur);
        }

        // 计算所有可能组合的数量 (刚好栈头元素 (所有分量的组合情况) 之后用不到, 于是直接 pop 了)
        BigDecimal allPermCnt = new BigDecimal(0);
        for (Map.Entry<Integer, Integer> e : stack.removeFirst().entrySet()) {
            allPermCnt = allPermCnt.add(new BigDecimal(e.getValue())
                    .multiply(getNumOfCasesForGivenCellsAndMines(notInCC, maxMineCnt - e.getKey())));
        }

        // 计算在不同雷数下的所有可能组合, 并与 allPermCnt 得到概率
        Map<Integer, Integer> right = outOfRange; // 与 stack 的 top 对应 (即下面代码里的 left 变量)
        for (int i = ccList.size() - 1; i >= 0; --i) { // 遍历每个连通分量 (遍历方向与 stack 相反)
            List<Pair<Integer, Integer>> ccPoints = ccList.get(i); // 该分量的所有格子坐标
            Map<Integer, int[]> ccPerms = ccPermList.get(i);       // 该连通分量的不同雷数 (key) 情况下的排列 (value)

            Map<Integer, Integer> left = stack.removeFirst();
            // 除该连通分量之外, 其他所有连通分量有多少组合使雷数小于等于 maxMineCnt (key 为雷数 (小于等于 maxMineCnt), value 为组合总个数)
            Map<Integer, Integer> exceptCur = mergeTwoPermutations(left, right, maxMineCnt, false);
            right = mergeTwoPermutations(right, ccPerms, maxMineCnt, true);

            for (Map.Entry<Integer, int[]> cur :  ccPerms.entrySet()) { // 遍历该连通分量的所有可能雷数
                BigDecimal curPermCnt = new BigDecimal(0); // (乘上每个格子为雷的情况个数后为) 当前情形下的可能情况
                for (Map.Entry<Integer, Integer> other : exceptCur.entrySet()) {
                    int mineCnt = other.getKey() + cur.getKey();
                    if (mineCnt < minMineCnt || mineCnt > maxMineCnt) continue;
                    curPermCnt = curPermCnt.add(new BigDecimal(other.getValue())
                            .multiply(getNumOfCasesForGivenCellsAndMines(notInCC, maxMineCnt - mineCnt)));
                }
                for (int j = 0; j < ccPoints.size(); ++j) {
                    int px = ccPoints.get(j).getKey(), py = ccPoints.get(j).getValue();
                    double prob = new BigDecimal(cur.getValue()[j]).multiply(curPermCnt)
                            .divide(allPermCnt, 6, BigDecimal.ROUND_HALF_UP).doubleValue();
                    probGraph[px][py] += prob;
                    avgPermMineCnt += prob;
                }
            }

        }
        return avgPermMineCnt;
    }

    /**
     * 综合考虑两个连通分量 (或包含多个分量的分量集合), 计算在不同雷数情况下有多少种可能性
     * 对于一个分量 P, S_P 代表分量 P 可能的雷数的集合, 且当分量雷数为 i (∈S_P) 时有 N_P_i 种情况 (同时 i 必小于等于剩余总雷数).
     * 则对于两个分量 (或分量集合) P1、P2, 雷数集合分别为 S1、S2, 当雷有 n 个时, 有 ΣN1_i * N2_j (i∈S1, j∈S2, i + j = n) 种情况.
     * @param perm1 连通分量 1
     * @param perm2 连通分量 2
     * @param maxMine 最多有多少个雷 (一般为剩余雷数)
     * @param objType 第二个参数 perm2 的 value 的类型, 我这里用到了两种数据结构: 一种就是整形, 记录情况个数;
     *                另一种是数组, 其最后一个元素记录了有多少种情况.
     * @return 两个分量 (或分量集合) 综合考虑后, 不同雷数情况下有多少种情况
     */
    private static Map<Integer, Integer> mergeTwoPermutations(Map<Integer, Integer> perm1, Map<Integer, ?> perm2,
                                                              int maxMine, boolean objType) {
        Map<Integer, Integer> res = new HashMap<>(32);
        for (Map.Entry<Integer, Integer> e1 : perm1.entrySet()) {
            for (Map.Entry<Integer, ?> e2 : perm2.entrySet()) {
                int newKey = e1.getKey() + e2.getKey();
                if (newKey > maxMine) continue;
                int newValue = e1.getValue();
                if (objType) {
                    int[] temp = (int[]) e2.getValue();
                    newValue *= temp[temp.length - 1];
                }
                else newValue *= (Integer) e2.getValue();
                newValue += res.getOrDefault(newKey, 0);
                res.put(newKey, newValue);
            }
        }
        return res;
    }

    /**
     * 获取以某点为中心、半径 radius 以内的所有数字格子的个数
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @param radius 半径 (最终范围为边长 2r-1 的一个正方形)
     * @return 数字格子个数
     */
    private static int getNumberCellCntAround(Game game, int x, int y, int radius) {
        int res = 0;
        for (int i = x - radius + 1; i < x + radius; ++i) for (int j = y - radius + 1; j < y + radius; ++j) {
            if (i < 0 || i >= game.getRow() || j < 0 || j >= game.getCol()) continue;
            if (i == x && j == y) continue;
            if (game.getPlayerBoard(x, y) < 9) ++res;
        }
        return res;
    }

    /**
     * 检查一个非数字的格子, 根据周围的数字格子判断该格子在被判定为是雷/非雷的情况下是否合法
     * @param game 一局游戏
     * @param board 棋局. 标记为 MINE 或 FLAG 说明判定为雷; 标记为 NOT_MINE 说明判定为非雷
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 是否合法
     */
    private static boolean isUncheckedCellLegal(Game game, int[][] board, int x, int y) {
        if (board[x][y] < 9) return false;
        for (Pair<Integer, Integer> p : game.getAround(x, y)) {
            int px = p.getKey(), py = p.getValue();
            if (board[px][py] < 9 && !isUncoveredCellLegal(game, board, px, py)) return false;
        }
        return true;
    }

    // 以下定义了一些杂七杂八的东西

    /**
     * 工具类, 不允许实例化
     */
    private AI() {}

    /**
     * 控制台输出连通分量视图
     * @param cc 方法 findAllConnectedComponent 的返回值
     */
    public static void printConnectedComponent(int[][] cc) {
        for (int i = 0; i < cc[0].length; ++i) System.out.print("---");
        System.out.println();
        for (int[] row : cc) {
            for (int v : row) {
                String s = String.valueOf(v);
                if (v == CC_VISITED) s = ".";
                else if (v == CC_UNKNOWN) s = " ";
                System.out.print(" " + s + " ");
            }
            System.out.println();
        }
        for (int i = 0; i < cc[0].length; ++i) System.out.print("---");
        System.out.println();
    }

    /**
     * 控制台输出概率视图
     * @param prob 方法 calculateProbability 的返回值
     */
    public static void printProbability(double[][] prob) {
        for (int i = 0; i < prob[0].length; ++i) System.out.print("-------");
        System.out.println();
        for (double[] row : prob) {
            for (double v : row) System.out.print(String.format(" %.3f ", v));
            System.out.println();
        }
        for (int i = 0; i < prob[0].length; ++i) System.out.print("-------");
        System.out.println();
    }
}
