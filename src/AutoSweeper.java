import javafx.util.Pair;

import java.awt.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.List;

public class AutoSweeper {
    // AI 给出的三种判断
    public static final int UNKNOWN = 0;
    public static final int MINE = 1;
    public static final int NOT_MINE = -1;

    // 连通分量视图的标记
    public static final int CC_VISITED = -233;
    public static final int CC_UNKNOWN = 0;

    // 下一步可确定的平均格子算法最大支持的计算量 (待测格子数小于等于该数字则投入该算法运行)
    // 该策略只是一种估算, 估算的格子越多偏差也可能越大. 所以该数字不是越大越好, 12 ~ 18 之间或许比较合理.
    private static final int MAX_NEXT_SITUATION_NUM = 15;

    // 时间复杂度爆炸的胜率算法最大支持的计算量 (待测格子数小于等于该数字则投入该算法运行)
    // 该策略精确计算点击每个格子的胜率, 所以该数字越大胜率越高. 但该数字指数级影响 AI 的总耗时.
    private static final int MAX_WIN_RATE_NUM = 12;

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
     * 仅通过检测周围 8 格, 确定已知数字的格子周围剩下的未知格子是否全部为雷
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 周围的 Unchecked 格子全为 (或全不为) 雷、或未知
     */
    public static int checkOneUncoveredCell(MineSweeper game, int x, int y) {
        if (game.getPlayerBoard(x, y) > 8) return UNKNOWN;
        int uncheckedOrQuestion = 0, flag = 0;
        List<Point> around = game.getAround(x, y);
        for (Point p : around) {
            switch (game.getPlayerBoard(p.x, p.y)) {
                case MineSweeper.UNCHECKED:
                case MineSweeper.QUESTION: ++uncheckedOrQuestion; break;
                case MineSweeper.FLAG: ++flag; break;
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
    public static Pair<List<Point>, List<Point>> checkTwoUncoveredCell(
            MineSweeper game, int x1, int y1, int x2, int y2) {
        int num1 = game.getPlayerBoard(x1, y1), num2 = game.getPlayerBoard(x2, y2);
        if (num1 > 8 || num2 > 8) return null;
        int diffX = x2 - x1, diffY = y2 - y1;
        if (Math.abs(diffX) + Math.abs(diffY) != 1) return null;
        List<Point> around1 = new ArrayList<>(3);
        List<Point> around2 = new ArrayList<>(3);
        for (int i = -1; i < 2; ++i) {
            int xx1 = x1 - diffX + diffY * i, yy1 = y1 - diffY + diffX * i;
            if (game.isPointInRange(xx1, yy1)) {
                int pp1 = game.getPlayerBoard(xx1, yy1);
                if (pp1 == MineSweeper.FLAG) --num1;
                else if (pp1 == MineSweeper.UNCHECKED || pp1 == MineSweeper.QUESTION) around1.add(new Point(xx1, yy1));
            }

            int xx2 = x2 + diffX + diffY * i, yy2 = y2 + diffY + diffX * i;
            if (game.isPointInRange(xx2, yy2)) {
                int pp2 = game.getPlayerBoard(xx2, yy2);
                if (pp2 == MineSweeper.FLAG) --num2;
                else if (pp2 == MineSweeper.UNCHECKED || pp2 == MineSweeper.QUESTION) around2.add(new Point(xx2, yy2));
            }
        }
        Pair<List<Point>, List<Point>> res = null;
        if (num2 - num1 - around2.size() == 0) res = new Pair<>(around1, around2);
        else if (num1 - num2 - around1.size() == 0) res = new Pair<>(around2, around1);
        return res;
    }

    /**
     * 初阶扫雷 AI, 根据游戏定义与减法公式, 找出所有必为雷或必不为雷的格子
     * @param game 一局游戏
     */
    public static void sweepAllBasedOnDefinition(MineSweeper game) {
        boolean swept;
        do {
            swept = false;
            for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
                // 根据单个格子进行判断
                int type = checkOneUncoveredCell(game, x, y);
                if (type != UNKNOWN) {
                    swept = true;
                    for (Point p : game.getAround(x, y)) {
                        if (game.getPlayerBoard(p.x, p.y) != MineSweeper.UNCHECKED
                                && game.getPlayerBoard(p.x, p.y) != MineSweeper.QUESTION) continue;
                        if (type == MINE) game.quickFlag(p.x, p.y);
                        else if (type == NOT_MINE) game.quickDig(p.x, p.y);
                        if (game.getGameState() == MineSweeper.LOSE) return;
                    }
                }

                // 根据相邻两个格子进行判断 (减法公式)
                for (int i = 0; i < 2; ++i) {
                    int x2 = x + i, y2 = y + 1 - i;
                    if (!game.isPointInRange(x2, y2)) continue;
                    Pair<List<Point>, List<Point>> _pair
                            = checkTwoUncoveredCell(game, x, y, x2, y2);
                    if (_pair != null) {
                        if (_pair.getKey().size() + _pair.getValue().size() > 0) swept = true;
                        for (Point p : _pair.getKey()) {
                            game.quickDig(p.x, p.y);
                            if (game.getGameState() == MineSweeper.LOSE) return;
                        }
                        for (Point p : _pair.getValue()) {
                            game.quickFlag(p.x, p.y);
                        }
                    }
                }
            }
            game.lazyUpdate();
        } while (swept);
    }

    /**
     * 进阶扫雷 AI, 基于概率, 将所有概率为 100% 或 0% 的格子扫掉
     * @param game 一局游戏
     * @return 最后一次计算得概率后没有利用, 将其返回以重复利用
     */
    public static ProbResult sweepAllBasedOnProbability(MineSweeper game) {
        boolean loop = true;
        ProbResult probResult = null;
        while (loop && game.getGameState() == MineSweeper.PROCESS) {
            loop = false;
            sweepAllBasedOnDefinition(game);
            if (game.getGameState() != MineSweeper.PROCESS) break;
            probResult = calculateAllProbabilities(game);
            double[][] prob = probResult.probGraph;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                // 只扫爆雷概率为 0 的, 不标爆雷概率为 1 的.
                // 虽然概率计算应该是精确的, 但是还是有极低概率出现计算得概率为 1 但却不是雷 (猜测可能是精度问题).
                // if (prob[i][j] == 1.0 && game.getPlayerBoard(i, j) != MineSweeper.FLAG) game.setFlag(i, j);
                if (prob[i][j] == 0.0 && game.getPlayerBoard(i, j) == MineSweeper.UNCHECKED) {
                    game.quickDig(i, j);
                    loop = true;
                }
            }
            game.lazyUpdate();
        }
        return probResult;
    }

    /**
     * 高阶扫雷 AI, 复杂度爆炸的胜率算法
     * 「算力允许的情况下」计算全局 (或孤立区域) 的胜率 (或局部胜率) 并把该区域全部扫完.
     * 因为计算胜率会把所有可能的子局面的胜率都算出来, 所以可以直接对照计算得的所有子局面, 把这一整个区域扫完.
     * @param game 一局游戏
     * @param probInfo 之前计算得的连通分量、爆雷概率等信息
     * @return 算法有没有执行
     */
    public static boolean sweepAllBasedOnWinRate(MineSweeper game, ProbResult probInfo) {
        return sweepIsolatedAreasBasedOnWinRate(game, probInfo) || sweepAllAreasBasedOnWinRate(game);
    }

    /**
     * 完整地玩一局扫雷, 玩到胜利或失败为止
     * 基于 sweepAllBasedOnDefinition -> sweepAllBasedOnProbability -> sweepAllBasedOnWinRate 顺序,
     * 根据定义与减法公式扫除一部分, 基于概率扫除一部分, 直到没有百分百确定的格子. 然后根据胜率算法 (算力允许范围内) 和非雷概率,
     * 扫除不百分百确定的雷.
     * @param game 一局游戏
     */
    public static void sweepToEnd(MineSweeper game) {
        if (game.getStep() == 0) {
            // Win7 的规则下第一步点击距离角落两格的点最好
            if (game.getGameRule() == MineSweeper.GAME_RULE_WIN_7) game.quickDig(2, 2);
            else game.quickDig(0, 0);
            game.lazyUpdate();
        }

        // 这段代码很长, 但逻辑还算清晰, 就不拆成多个函数了
        while (game.getGameState() == MineSweeper.PROCESS) {
            // 先把百分百有把握的格子扫了
            ProbResult probResult = sweepAllBasedOnProbability(game);
            if (game.getGameState() != MineSweeper.PROCESS) break;
            if (probResult == null) probResult = calculateAllProbabilities(game);
            double[][] prob = probResult.probGraph;

            // 至此, 局面上已经没有百分百「是雷」/「不是雷」的格子了, 于是需要一些策略来扫一个格子.
            // 下面有两大种策略:
            //     1. 基于「所有后续局面」的全局 / 局部胜率 (复杂度爆炸, 仅在残局使用).
            //     2. 基于「仅针对当前局面」的爆雷概率 (速度相对更快);

            // 如果在算力允许的范围内, 扫除一整个残局或多个孤立区域
            sweepAllBasedOnWinRate(game, probResult);
            if (game.getGameState() != MineSweeper.PROCESS) break;

            // 根据计算得的每个格子的爆雷概率, 找出爆雷概率最小的格子 (如果最小概率的格子有多个, 还有一些小策略, 详情见 for 内注释)
            // 这里变量命名略有混乱, 解释一下: prob 返回的是爆雷概率, maxX、maxY 标记的是最大非雷概率 (即 1 - 爆雷概率).
            int maxX = -1, maxY = -1, corner = -1, intensity = -1;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                int cellState = game.getPlayerBoard(i, j);
                if (cellState != MineSweeper.UNCHECKED && cellState != MineSweeper.QUESTION) continue;
                if (maxX != -1 && prob[i][j] > prob[maxX][maxY]) continue;
                boolean newMax = false;
                int cor = 0, in = getNumberCellCntAround(game, i, j, 3);
                if (i == 0 || i == game.getRow() - 1) ++cor;
                if (j == 0 || j == game.getCol() - 1) ++cor;
                if (maxX != -1 && prob[i][j] == prob[maxX][maxY]) {
                    // 同概率的话在角落的格子优先探测, 可以将概率从 29% 提升到 33%.
                    // 或者当两者都在 (或都不在) 角落时, 选择周围 24 格数字格子更多的.
                    if ((cor == 2 && cor > corner) || (in > intensity)) newMax = true;
                }
                else if (maxX == -1 || prob[i][j] < prob[maxX][maxY]) newMax = true;
                if (!newMax) continue;
                maxX = i;
                maxY = j;
                corner = cor;
                intensity = in;
            }

            // 若有多个爆雷概率最小的格子且 **复杂度允许**, 则对这些格子计算「选择该格子后, 可百分百确定的其他格子的期望数量」
            List<Point> candidates = new ArrayList<>(MAX_NEXT_SITUATION_NUM + 1);
            candidates.add(new Point(maxX, maxY));
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                if (i == maxX && j == maxY) continue;
                if (prob[i][j] != prob[maxX][maxY]) continue;
                candidates.add(new Point(i, j));
                if (candidates.size() > MAX_NEXT_SITUATION_NUM) break;
            }
            if (candidates.size() <= MAX_NEXT_SITUATION_NUM) {
                int[][] board = game.getPlayerBoard();
                int[][] ccGraph = findAllConnectedComponents(game).getValue();
                double maxAvgSafe = 0;
                int _maxX = maxX, _maxY = maxY;
                boolean adoptThisStrategy = true;
                for (Point p : candidates) {
                    double num = calculateAvgNumOfSafeCells(game, p.x, p.y, board, ccGraph);
                    if (num < 0) {
                        adoptThisStrategy = false;
                        break;
                    }
                    if (num > maxAvgSafe) {
                        maxAvgSafe = num;
                        _maxX = p.x;
                        _maxY = p.y;
                    }
                }
                if (adoptThisStrategy) {
                    maxX = _maxX;
                    maxY = _maxY;
                }
            }

            // 只找 prob 低的 uncover, 不找 prob 高的 setFlag, 因为 setFlag 不影响游戏状态, 标错了也不知道.
            game.quickDig(maxX, maxY);
            game.lazyUpdate();
        }
    }

    /**
     * 寻找所有连通分量
     * 一个连通分量表示, 有多种可能性且相互影响的一个区域
     * @param game 一局游戏
     * @return Pair 的 key 储存所有分量的所有点, value 为整个图
     */
    public static Pair<List<List<Point>>, int[][]> findAllConnectedComponents(MineSweeper game) {
        List<List<Point>> ccList = new ArrayList<>();
        int[][] ccGraph = new int[game.getRow()][game.getCol()];
        int id = 1;
        // 遍历每个点, 找到第一个可能属于一个连通分量的点, 并从该点扩散开来寻找其他属于该分量的点
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if (ccGraph[i][j] != CC_UNKNOWN || game.getPlayerBoard(i, j) > 8) continue;
            // 找到了一个可能的点. 注意该点为已扫出数字的格子 (因为一个数字格子周围的未知格子必属于同一分量)
            // 该队列存储的点是已扫出数字的格子, 这些个数字格子周围的未知格子也必属于同一分量
            Queue<Point> que = new LinkedList<>();
            que.offer(new Point(i, j));
            List<Point> points = new ArrayList<>();
            boolean findANewComponent = false;
            // BFS 遍历周围的点, 搜出整个连通分量
            while (!que.isEmpty()) {
                Point cur = que.poll();
                if (ccGraph[cur.x][cur.y] == CC_VISITED) continue;
                ccGraph[cur.x][cur.y] = CC_VISITED;
                // 遍历该数字格子周围的所有未知格子, 它们属于同一个分量
                for (Point p : game.getAround(cur.x, cur.y)) {
                    if ((game.getPlayerBoard(p.x, p.y) != MineSweeper.UNCHECKED && game.getPlayerBoard(p.x, p.y) != MineSweeper.QUESTION)
                            || ccGraph[p.x][p.y] == id) continue;
                    findANewComponent = true;
                    points.add(new Point(p.x, p.y));
                    ccGraph[p.x][p.y] = id;
                    // 找出「数字格子周围的未知格子」的周围的其余数字格子, 加入队列 (有点绕)
                    for (Point p2 : game.getAround(p.x, p.y)) {
                        if (game.getPlayerBoard(p2.x, p2.y) < 9) que.offer(p2);
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
     * 注意: 返回的是格子为雷的概率, 则非雷概率为 1 - p.
     * @param game 一局游戏
     * @return 每个格子的有雷概率
     */
    public static ProbResult calculateAllProbabilities(MineSweeper game) {
        double[][] probGraph = new double[game.getRow()][game.getCol()];
        Pair<List<List<Point>>, int[][]> _ccPair = findAllConnectedComponents(game);
        List<List<Point>> ccList = _ccPair.getKey();
        int[][] ccGraph = _ccPair.getValue();
        List<Map<Integer, int[]>> ccPermList = new ArrayList<>(ccList.size());

        // 计算每个连通分量的每个点的有雷概率
        for (List<Point> points : ccList) {
            Map<Integer, int[]> perm = new HashMap<>(16);
            backtrackAllPossiblePermutations(game, game.getPlayerBoard(), points,
                    perm, 0, 0); // 如果 permutationCnt 为 0, 说明玩家设的旗有错, 会异常
            ccPermList.add(perm);
        }
        double avgPermMineCnt = calculateProbabilitiesOfAllConnectedComponents(game, ccList, ccPermList, probGraph);

        // 所有未知孤立格子 (即周围没有已知数字格子的格子) 统一计算概率为 "平均剩余雷数/未知孤立格子数"
        int unknownCellCnt = game.getUncheckedCellLeft();
        for (List<Point> points : ccList) unknownCellCnt -= points.size();
        if (unknownCellCnt > 0) {
            double unknownCellProb = ((double) game.getMineLeft() - avgPermMineCnt) / (double) (unknownCellCnt);
            if (Math.abs(unknownCellProb) < 1e-5) unknownCellProb = 0.0;
            else if (Math.abs(unknownCellProb - 1.0) < 1e-5) unknownCellProb = 1.0;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                if (ccGraph[i][j] == CC_UNKNOWN) {
                    if (game.getPlayerBoard(i, j) == MineSweeper.UNCHECKED || game.getPlayerBoard(i, j) == MineSweeper.QUESTION) {
                        probGraph[i][j] = unknownCellProb;
                    }
                    else if (game.getPlayerBoard(i, j) == MineSweeper.FLAG) probGraph[i][j] = 1.0;
                }
            }
        }
//        printProbability(probGraph);
        return new ProbResult(_ccPair.getKey(), _ccPair.getValue(), ccPermList, probGraph);
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
    private static int backtrackAllPossiblePermutations(MineSweeper game, int[][] board, List<Point> points,
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
                Point p = points.get(i);
                if (board[p.x][p.y] == MineSweeper.MINE) ++count[i];
            }
            ++count[points.size()]; // 排列个数总数
            return 1;
        }

        // 分别递归考虑当前格子是雷、不是雷的情况
        Point cur = points.get(curIndex);
        int res = 0;
        board[cur.x][cur.y] = MineSweeper.MINE;
        if (curMine < game.getMineLeft() && isUncheckedCellLegal(game, board, cur.x, cur.y)) {
            res += backtrackAllPossiblePermutations(game, board, points, ccPerm, curIndex + 1, curMine + 1);
        }
        board[cur.x][cur.y] = MineSweeper.NOT_MINE;
        if (isUncheckedCellLegal(game, board, cur.x, cur.y)) {
            res += backtrackAllPossiblePermutations(game, board, points, ccPerm, curIndex + 1, curMine);
        }
        board[cur.x][cur.y] = MineSweeper.UNCHECKED;
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
    private static double calculateProbabilitiesOfAllConnectedComponents(MineSweeper game,
                                                                         List<List<Point>> ccList,
                                                                         List<Map<Integer, int[]>> ccPermList,
                                                                         double[][] probGraph) {
        int notInCC = game.getUncheckedCellLeft();
        for (List<Point> cc : ccList) {
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
            List<Point> ccPoints = ccList.get(i); // 该分量的所有格子坐标
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
                    Point p = ccPoints.get(j);
                    double prob = new BigDecimal(cur.getValue()[j]).multiply(curPermCnt)
                            .divide(allPermCnt, 6, BigDecimal.ROUND_HALF_UP).doubleValue();
                    probGraph[p.x][p.y] += prob;
                    avgPermMineCnt += prob;
                }
            }

        }
        return avgPermMineCnt;
    }

    /**
     * 如果假定一个未知格子为数字格子, 那么当该格子为 0~8 的数字时, 当前局面有哪些格子可以因此也被确定是安全的 (必不为雷).
     * 计算 0~8 所有情况下平均能确定多少个雷. 该策略效果没那么明显 (低于 1%), 且涉及连通分量的遍历计算, 十分耗时.
     * 所以优化了一下, 如果待计算的连通分量长度大于 16, 就返回 -1, 以表罢工. 最终结果是提升了 0.5% ~ 0.9%, 平均每局耗时增加了 2ms.
     * @param game 一局游戏
     * @param x 待测格子 x 坐标 (请确保该格子为未知格子或问号格子)
     * @param y 待测格子 y 坐标 (请确保该格子为未知格子或问号格子)
     * @param board 玩家面板
     * @param ccGraph 连通分量图
     * @return 确定待测格子后平均能确定多少其他未知格子
     */
    private static double calculateAvgNumOfSafeCells(MineSweeper game, int x, int y, int[][] board, int[][] ccGraph) {
        // 假设当前格子不是雷, 计算该格子所涉及的连通分量
        List<Point> newCcPoints = new ArrayList<>();
        Set<Integer> ccSet = new HashSet<>(8);
        boolean[][] vis = new boolean[game.getRow()][game.getCol()];
        vis[x][y] = true;
        for (Point p : game.getAround(x, y)) { // 周围一圈的未知格子都属于该分量
            if (board[p.x][p.y] == MineSweeper.UNCHECKED || board[p.x][p.y] == MineSweeper.QUESTION) {
                vis[p.x][p.y] = true;
                newCcPoints.add(p);
                if (ccGraph[p.x][p.y] > 0) ccSet.add(ccGraph[p.x][p.y]);
            }
        }
        for (int i = 0; i < newCcPoints.size(); ++i) { // 周围一圈格子所涉及的连通分量也都属于该新分量
            Point p = newCcPoints.get(i);
            for (Point pa : game.getAround(p.x, p.y)) {
                if (!vis[pa.x][pa.y] && ccSet.contains(ccGraph[pa.x][pa.y])) {
                    vis[pa.x][pa.y] = true;
                    newCcPoints.add(pa);
                }
            }
        }
        // 连通分量太长的不算, 不然太耗时
        if (newCcPoints.size() > 16) return -1;

        // 根据新连通分量, 计算: 当目标格子为 0~8 时, 后续可以确定多少个格子必为非雷
        // (只统计必非雷的格子, 没统计必为雷的格子, 因为标雷对棋局没什么实质性帮助)
        int alwaysSafe = 0, allPermCnt = 0;
        for (int num = 0; num <= 8; ++num) {
            board[x][y] = num;
            Map<Integer, int[]> perm = new HashMap<>(16);
            backtrackAllPossiblePermutations(game, board, newCcPoints, perm, 0, 0);
            int[] allCount = new int[newCcPoints.size() + 1];
            for (int[] v : perm.values()) {
                for (int i = 0; i < allCount.length; ++i) {
                    allCount[i] += v[i];
                }
            }
            final int permCnt = allCount[newCcPoints.size()];
            if (permCnt == 0) continue;
            allPermCnt += permCnt;
            for (int mineCnt : allCount) {
                if (mineCnt == 0) alwaysSafe += permCnt;
            }
        }
        board[x][y] = MineSweeper.UNCHECKED;

        // 计算: 当当前格不为雷时, 则挖开该格子后, 局面上平均有多少格未知格子因此被确定不是雷
        return (double) alwaysSafe / allPermCnt;
    }

    /**
     * 给整个残局计算全局胜率并扫除 (算力不允许则返回 false)
     * @param game 一局游戏
     * @return 有没有执行
     */
    private static boolean sweepAllAreasBasedOnWinRate(MineSweeper game) {
        if (game.getUncheckedCellLeft() > MAX_WIN_RATE_NUM) return false;
        return sweepGivenAreaBasedOnWinRate(game, game.getPlayerBoard(), getAllUncheckedPoints(game), game.getMineLeft());
    }

    /**
     * 给所有孤立区域计算局部胜率并扫除 (算力不允许则返回 false)
     * @param game 一局游戏
     * @param probInfo 概率信息
     * @return 有没有执行
     */
    private static boolean sweepIsolatedAreasBasedOnWinRate(MineSweeper game, ProbResult probInfo) {
        boolean worked = false;
        for (Pair<Pair<List<Point>, Integer>, int[][]> area : findAllIsolatedAreas(game, probInfo)) {
            List<Point> toCheck = area.getKey().getKey();
            int maxMineCnt = area.getKey().getValue();
            int[][] board = area.getValue();
            worked = sweepGivenAreaBasedOnWinRate(game, board, toCheck, maxMineCnt) || worked;
            if (game.getGameState() != MineSweeper.PROCESS) break;
        }
        return worked;
    }

    /**
     * 给给定区域计算胜率并扫除 (算力不允许则返回 false)
     * @param game 一局游戏
     * @param board 当前的棋局
     * @param toCheck 指定区域的所有未知格子
     * @param maxMineCnt 该区域雷数
     * @return 有没有执行
     */
    private static boolean sweepGivenAreaBasedOnWinRate(MineSweeper game, int[][] board, List<Point> toCheck, int maxMineCnt) {
        Map<String, Pair<Pair<Integer, Double>, double[]>> vis = calculateAllWinRates(game, board, toCheck, maxMineCnt);
        if (vis == null) return false;
        while (game.getGameState() == MineSweeper.PROCESS) {
            String uri = uriOfBoard(game.getPlayerBoard(), toCheck);
            Pair<Pair<Integer, Double>, double[]> winRateInfo = vis.get(uri);
            double[] rateList = winRateInfo.getValue();
            double maxRate = winRateInfo.getKey().getValue();
            int i;
            for (i = 0; i < toCheck.size(); ++i) {
                if (rateList[i] != maxRate) continue;
                game.dig(toCheck.get(i).x, toCheck.get(i).y);
                break;
            }
            if (i == toCheck.size()) break;
        }
        return true;
    }

    /**
     * 找到所有孤立区域
     * 孤立区域的定义: 区域内都是未知格子且属于同一个连通分量, 且区域内部雷数数量的可能性唯一.
     * @param game 一局游戏
     * @param probInfo 棋盘连通分量与概率信息
     * @return 所有孤立区域. List 的元素为 ((孤立区域的格子, 孤立区域的雷数), 把非孤立区域部分都填满后的棋盘) )
     */
    private static List<Pair<Pair<List<Point>, Integer>, int[][]>> findAllIsolatedAreas(MineSweeper game, ProbResult probInfo) {
        List<Pair<Pair<List<Point>, Integer>, int[][]>> res = new ArrayList<>();
        for (int index = 0; index < probInfo.ccList.size(); ++index) {
            List<Point> points = probInfo.ccList.get(index);
            Map<Integer, int[]> perms = probInfo.ccPermList.get(index);
            if (points.size() > MAX_WIN_RATE_NUM) continue;

            // 判断一块区域是不是孤立的 (即区域内可能存在的雷数是确定的)
            if (perms.size() != 1) continue;
            boolean isIsolated = true;
            for (Point p : points) {
                for (Point pa : game.getAround(p.x, p.y)) {
                    int cellState = game.getPlayerBoard(pa.x, pa.y);
                    if ((cellState == MineSweeper.UNCHECKED || cellState == MineSweeper.QUESTION)
                            && probInfo.ccGraph[pa.x][pa.y] != probInfo.ccGraph[p.x][p.y]) {
                        isIsolated = false;
                        break;
                    }
                }
                if (!isIsolated) break;
            }
            if (!isIsolated) continue;

            // 获取该孤立区域的雷数
            int mineCnt = 0;
            for (int cnt : perms.keySet()) mineCnt = cnt; // 雷数 (即 perms 的 key) 只有一种可能, 即 for 只会执行一次

            // 把非待测区域的地方都填满 (一开始写方法的时候没考虑周到, 导致现在调用那些方法得曲线救国. 但是又懒得重构了)
            int[][] testBoard = new int[game.getRow()][game.getCol()], board = game.getPlayerBoard();
            for (Point p : points) {
                if (board[p.x][p.y] == MineSweeper.UNCHECKED || board[p.x][p.y] == MineSweeper.QUESTION) {
                    testBoard[p.x][p.y] = MineSweeper.NOT_MINE;
                }
            }
            int flagToAdd = game.getMineLeft() - mineCnt;
            for (int i = 0; i < testBoard.length; ++i) for (int j = 0; j < testBoard[0].length; ++j) {
                if (testBoard[i][j] == MineSweeper.NOT_MINE) testBoard[i][j] = MineSweeper.UNCHECKED;
                else if (board[i][j] == MineSweeper.UNCHECKED || board[i][j] == MineSweeper.QUESTION) {
                    if (flagToAdd > 0) {
                        --flagToAdd;
                        testBoard[i][j] = MineSweeper.FLAG;
                    } else testBoard[i][j] = 8;
                } else testBoard[i][j] = board[i][j];
            }
            res.add(new Pair<>(new Pair<>(points, mineCnt), testBoard));
        }
        return res;
    }

    /**
     * 计算 toCheck 列表里所有点的胜率
     * @param game 一局游戏
     * @param board 当前棋局
     * @param toCheck 待测的未知格子列表
     * @param maxMineCnt 这些格子内的雷数
     * @return 当前局与所有子局的胜率信息
     */
    private static Map<String, Pair<Pair<Integer, Double>, double[]>>
    calculateAllWinRates(MineSweeper game, int[][] board, List<Point> toCheck, int maxMineCnt) {
        if (toCheck.size() > MAX_WIN_RATE_NUM) return null;
        Map<String, Pair<Pair<Integer, Double>, double[]>> vis = new HashMap<>(500000);
        if (calculateBoardWinRate(game, board, toCheck, maxMineCnt, vis) == null) return null;
        return vis;
    }

    /**
     * 计算一个局面的胜率 (即所有格子的最大胜率)
     * @param game 一局游戏
     * @param board 当前局面
     * @param toCheck 待测的未知格子列表
     * @param maxMineCnt 这些格子内的雷数
     * @param vis 所有已计算的子局面的胜率信息
     * @return 可能的排列数 与 局面的胜率
     */
    private static Pair<Integer, Double> calculateBoardWinRate(MineSweeper game, int[][] board,
                                                               List<Point> toCheck, int maxMineCnt,
                                                               Map<String, Pair<Pair<Integer, Double>, double[]>> vis) {
        // 获得局面 uri, 如果已经计算过则直接返回
        String uri = uriOfBoard(board, toCheck);
        Pair<Pair<Integer, Double>, double[]> tuple = vis.get(uri);
        if (tuple != null) return tuple.getKey();

        double[] winRates = new double[toCheck.size()];
        List<Point> _unchecked = new ArrayList<>(toCheck.size());
        for (Point p : toCheck) {
            if (board[p.x][p.y] == MineSweeper.UNCHECKED || board[p.x][p.y] == MineSweeper.QUESTION) {
                _unchecked.add(p);
            }
        }
        int permCnt = calculatePermCnt(game, board).intValue();
        // DFS 的边界: 剩下的未知格子都是雷或当前局面不合法
        if (_unchecked.size() == maxMineCnt || permCnt == 0) {
            vis.put(uri, new Pair<>(new Pair<>(permCnt, (double) permCnt), winRates));
            return new Pair<>(permCnt, (double) permCnt);
        }

        // 局面的胜率等于所有格子中胜率最大的那个
        int maxIndex = -1;
        for (int index = 0; index < toCheck.size(); ++index) {
            int x = toCheck.get(index).x, y = toCheck.get(index).y;
            if (board[x][y] != MineSweeper.UNCHECKED && board[x][y] != MineSweeper.QUESTION) continue;
            double cellWinRate = calculateCellWinRate(game, board, toCheck, maxMineCnt, index, permCnt, vis);
            winRates[index] = cellWinRate;

            if (maxIndex == -1 || winRates[maxIndex] < cellWinRate) {
                maxIndex = index;
            }
        }
        vis.put(uri, new Pair<>(new Pair<>(permCnt, winRates[maxIndex]), winRates));
        return new Pair<>(permCnt, winRates[maxIndex]);
    }

    /**
     * 计算一个格子在当前局面的胜率 (即当它为 0 ~ 8 时的子局面胜率的综合)
     * @param game 一局游戏
     * @param board 当前局面
     * @param toCheck 待测格子
     * @param maxMineCnt 局面最大雷数
     * @param checkIndex 当前计算的格子在 toCheck 中的下标
     * @param permCnt 当前局面的所有可能排列数量
     * @param vis 所有已计算的子局面的胜率信息
     * @return 格子胜率
     */
    private static double calculateCellWinRate(MineSweeper game, int[][] board, List<Point> toCheck, int maxMineCnt,
                                               int checkIndex, int permCnt,
                                               Map<String, Pair<Pair<Integer, Double>, double[]>> vis) {
        int x = toCheck.get(checkIndex).x, y = toCheck.get(checkIndex).y;
        double cellWinRate = 0.0;
        board[x][y] = MineSweeper.NOT_MINE;
        if (isUncheckedCellLegal(game, board, x, y)) {
            for (int cellState = 0; cellState <= 8; ++cellState) {
                board[x][y] = cellState;
                if (!isUncoveredCellLegal(game, board, x, y)) continue;
                Pair<Integer, Double> _pair = calculateBoardWinRate(game, board, toCheck, maxMineCnt, vis);
                assert _pair != null;
                cellWinRate += _pair.getValue() * _pair.getKey() / permCnt;
            }
        }
        board[x][y] = MineSweeper.UNCHECKED;
        return cellWinRate;
    }

    /**
     * 综合考虑两个连通分量 (或包含多个分量的分量集合), 计算在不同雷数情况下有多少种可能性
     * 对于一个分量 P, S_P 代表分量 P 可能的雷数的集合, 且当分量雷数为 i (∈S_P) 时有 N_P_i 种情况 (同时 i 必小于等于剩余总雷数).
     * 则对于两个分量 (或分量集合) P1、P2, 雷数集合分别为 S1、S2, 当雷有 n 个时, 有 ΣN1_i * N2_j (i∈S1, j∈S2, i + j = n) 种情况.
     * @param perm1 连通分量 1
     * @param perm2 连通分量 2
     * @param maxMine 最多有多少个雷 (一般为剩余雷数)
     * @param objType 第二个参数 perm2 的 value 的类型, 我这里用到了两种数据结构: 一种就是整形, 记录情况个数;
     *                另一种是数组, 其最后一个元素记录了情况个数.
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
     * 计算当前局面所有的可能组合数
     * @param game 一局游戏
     * @param board 当前局面
     * @return 所有的可能组合数
     */
    private static BigDecimal calculatePermCnt(MineSweeper game, int[][] board) {
        DebugSweeper g2 = new DebugSweeper(game, board);
        Pair<List<List<Point>>, int[][]> _pair = findAllConnectedComponents(g2);
        Map<Integer, Integer> perms = new HashMap<>();
        perms.put(0, 1); // 0 个分量时有一种情况
        for (List<Point> list : _pair.getKey()) {
            Map<Integer, int[]> perm = new HashMap<>(16);
            backtrackAllPossiblePermutations(g2, board, list, perm, 0, 0);
            perms = mergeTwoPermutations(perms, perm, g2.getMineLeft(), true);
        }
        BigDecimal permCnt = new BigDecimal(0);
        int unknown = g2.getUncheckedCellLeft();
        for (List<Point> list : _pair.getKey()) unknown -= list.size();
        for (Map.Entry<Integer, Integer> e : perms.entrySet()) {
            permCnt = permCnt.add(getNumOfCasesForGivenCellsAndMines(unknown, g2.getMineLeft() - e.getKey())
                    .multiply(new BigDecimal(e.getValue())));
        }
        return permCnt;
    }

    /**
     * 获得所有未知的格子
     * @param game 一局游戏
     * @return 所有格子
     */
    private static List<Point> getAllUncheckedPoints(MineSweeper game) {
        List<Point> toCheck = new ArrayList<>(MAX_WIN_RATE_NUM);
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if (game.getPlayerBoard(i, j) != MineSweeper.UNCHECKED
                    && game.getPlayerBoard(i, j) != MineSweeper.QUESTION) continue;
            toCheck.add(new Point(i, j));
        }
        return  toCheck;
    }

    /**
     * 给定 n 个未知的格子与 m 个雷, 返回所有可能的组合的个数
     * f(n, m) = f(n - 1, m) + f(n - 1, m - 1)
     * @param cells 未知格子数
     * @param mines 雷的个数
     * @return 可能的情况 (值可能超过 long 的范围, 且后续涉及浮点除法, 故采用 BigDecimal)
     */
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
     * 获取以某点为中心、半径 radius 以内的所有数字格子的个数
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @param radius 半径 (最终范围为边长 2r-1 的一个正方形)
     * @return 数字格子个数
     */
    private static int getNumberCellCntAround(MineSweeper game, int x, int y, int radius) {
        int res = 0;
        for (int i = x - radius + 1; i < x + radius; ++i) for (int j = y - radius + 1; j < y + radius; ++j) {
            if (i < 0 || i >= game.getRow() || j < 0 || j >= game.getCol()) continue;
            if (i == x && j == y) continue;
            if (game.getPlayerBoard(x, y) < 9) ++res;
        }
        return res;
    }

    /**
     * 检查已知是数字的一个格子, 判断对其周围是否是雷的判定是否合法
     * 数字格子周围的格子有三种状态: 数字 (或被假设为非雷的未知格子)、雷 (旗子或被假设为雷的未知格子)、未知 (还未被判定),
     * 以此判定当前棋面是否可能存在.
     * @param game 一局游戏
     * @param board 棋局. 标记为 MINE 或 FLAG 说明判定为雷; 标记为 NOT_MINE 说明判定为非雷
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 是否合法
     */
    private static boolean isUncoveredCellLegal(MineSweeper game, int[][] board, int x, int y) {
        if (board[x][y] > 8) return false;
        List<Point> list = game.getAround(x, y);
        int mineCnt = 0, uncheckedCnt = 0;
        for (Point p : list) {
            switch (board[p.x][p.y]) {
                case MineSweeper.FLAG:
                case MineSweeper.MINE:
                case MineSweeper.RED_MINE:
                case MineSweeper.GRAY_MINE:
                    ++mineCnt;
                    break;
                case MineSweeper.UNCHECKED:
                case MineSweeper.QUESTION:
                    ++uncheckedCnt;
                    break;
                default: break;
            }
        }
        if (uncheckedCnt == 0) return mineCnt == board[x][y];
        return mineCnt <= board[x][y] && mineCnt + uncheckedCnt >= board[x][y];
    }

    /**
     * 检查一个非数字的格子, 根据周围的数字格子判断该格子在被判定为是雷/非雷的情况下是否合法
     * @param game 一局游戏
     * @param board 棋局. 标记为 MINE 或 FLAG 说明判定为雷; 标记为 NOT_MINE 说明判定为非雷
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return 是否合法
     */
    private static boolean isUncheckedCellLegal(MineSweeper game, int[][] board, int x, int y) {
        if (board[x][y] < 9) return false;
        for (Point p : game.getAround(x, y)) {
            if (board[p.x][p.y] < 9 && !isUncoveredCellLegal(game, board, p.x, p.y)) return false;
        }
        return true;
    }

    /**
     * 用一个 String 来作为一个局面的唯一标识, 可用于作为字典的 key
     * @param board 局面
     * @param toCheck 生成标识所涉及的所有点
     * @return 局面的唯一标识
     */
    private static String uriOfBoard(int[][] board, List<Point> toCheck) {
        StringBuilder builder = new StringBuilder();
        for (Point p : toCheck) {
            switch (board[p.x][p.y]) {
                case MineSweeper.UNCHECKED: case MineSweeper.QUESTION:
                    builder.append('U'); break;
                default: builder.append((char)('0' + board[p.x][p.y]));
            }
        }
        return builder.toString();
    }

    // 以下定义了一些杂七杂八的东西----------------------------------------------------------------------

    /**
     * 工具类, 不允许实例化
     */
    private AutoSweeper() {}

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

    /**
     * 仅通过检测周围8格, 确定目标未知格子是否有雷
     * 要先找出周围所有数字格子, 再逐个判断数字格子周围8格, 最终判断范围达到了 5*5 格, 不建议使用该函数
     * @param game 一局游戏
     * @param x 目标格子 x 坐标
     * @param y 目标格子 y 坐标
     * @return AI认为是有、无雷还是未知
     */
    @Deprecated
    public static int checkUncheckedCellBasic(MineSweeper game, int x, int y) {
        if (game.getPlayerBoard(x, y) != MineSweeper.UNCHECKED
                && game.getPlayerBoard(x, y) != MineSweeper.QUESTION) return UNKNOWN;
        List<Point> around = game.getAround(x, y);
        for (Point p : around) {
            int pState = game.getPlayerBoard(p.x, p.y);
            if (pState < 9) {
                int state = checkOneUncoveredCell(game, p.x, p.y);
                if (state != UNKNOWN) return state;
            }
        }
        return UNKNOWN;
    }

    /**
     * 扫描全盘, 根据游戏定义与减法公式, 判断是否存在必为雷或必不为雷的格子 (主要是 GUI 展示时用)
     * @param game 一局游戏
     * @return 如果找到了则 int 数组长度为 3, 第一个值代表类型, 第二、第三个值代表坐标 (只返回找到的第一个格子);
     *         否则 int 数组长度为 1, 返回 { UNKNOWN }.
     */
    public static int[] checkAllBasedOnDefinition(MineSweeper game) {
        for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
            int type = checkOneUncoveredCell(game, x, y);
            if (type != UNKNOWN) {
                for (Point p : game.getAround(x, y)) {
                    if (game.getPlayerBoard(p.x, p.y) != MineSweeper.UNCHECKED
                            && game.getPlayerBoard(p.x, p.y) != MineSweeper.QUESTION) continue;
                    return new int[]{type, p.x, p.y};
                }
            }

            for (int i = 0; i < 2; ++i) {
                int x2 = x + i, y2 = y + 1 - i;
                Pair<List<Point>, List<Point>> _pair;
                try {
                    _pair = checkTwoUncoveredCell(game, x, y, x2, y2);
                } catch (MineSweeper.PointOutOfBoundsException ignored) { continue; }
                if (_pair != null) {
                    for (Point p : _pair.getKey()) {
                        return new int[]{NOT_MINE, p.x, p.y};
                    }
                    for (Point p : _pair.getValue()) {
                        return new int[]{MINE, p.x, p.y};
                    }
                }
            }
        }
        return new int[]{UNKNOWN};
    }

    /**
     * 计算并获得当前每个孤立区域的格子 (或所有格子) 的胜率
     * 如果超出算力范围则不计算, 格子胜率设为 NaN. 如果算的是全局胜率, 则 areaGraph 返回 null;
     * 反之如果计算的是局部胜率, areaGraph 标记不同孤立区域.
     * @param game 一局游戏
     * @return 所有格子的胜率 (如果计算耗时超标则全部返回 NaN)
     */
    public static Pair<int[][], double[][]> getWinRateGraph(MineSweeper game) {
        double[][] rateGraph = new double[game.getRow()][game.getCol()];
        for (int i = 0; i < rateGraph.length; ++i) for (int j = 0; j < rateGraph[0].length; ++j) {
            rateGraph[i][j] = Double.NaN;
        }
        int[][] areaGraph = new int[game.getRow()][game.getCol()];
        int areaIndex = 0;
        int notIsolatedCellCnt = game.getUncheckedCellLeft();
        ProbResult pr = calculateAllProbabilities(game);
        for (Pair<Pair<List<Point>, Integer>, int[][]> area : findAllIsolatedAreas(game, pr)) {
            List<Point> toCheck = area.getKey().getKey();
            int maxMineCnt = area.getKey().getValue();
            int[][] board = area.getValue();
            Map<String, Pair<Pair<Integer, Double>, double[]>> vis
                    = calculateAllWinRates(game, board, toCheck, maxMineCnt);
            if (vis == null) continue;
            notIsolatedCellCnt -= toCheck.size();
            double[] rates = vis.get(uriOfBoard(board, toCheck)).getValue();
            ++areaIndex;
            for (int i = 0; i < toCheck.size(); ++i) {
                rateGraph[toCheck.get(i).x][toCheck.get(i).y] = rates[i];
                areaGraph[toCheck.get(i).x][toCheck.get(i).y] = areaIndex;
            }
        }
        if (areaIndex == 1 && notIsolatedCellCnt == 0) {
            // 如果该孤立区域即全部未知区域, 则把背景换成金色 (仅仅是为了展示好看易于理解)
            areaGraph = null;
        }
        else if (areaIndex == 0) {
            List<Point> toCheck = getAllUncheckedPoints(game);
            Map<String, Pair<Pair<Integer, Double>, double[]>> vis
                    = calculateAllWinRates(game, game.getPlayerBoard(), toCheck, game.getMineLeft());
            if (vis != null) {
                areaGraph = null;
                double[] winRates = vis.get(uriOfBoard(game.getPlayerBoard(), toCheck)).getValue();
                for (int i = 0; i < winRates.length; ++i) {
                    rateGraph[toCheck.get(i).x][toCheck.get(i).y] = winRates[i];
                }
            }
        }
        return new Pair<>(areaGraph, rateGraph);
    }

    /**
     * 自定义一局
     */
    private static class DebugSweeper extends MineSweeper {
        public DebugSweeper(MineSweeper game, int[][] board) {
            super(game.getRow(), game.getCol(), game.getMineCount(), true, game.getGameRule());
            this.playerBoard = board;
            this.coveredCellLeft = this.row * this.col - this.mineCount;
            this.mineLeft = this.mineCount;
            for (int i = 0; i < this.row; ++i) for (int j = 0; j < this.col; ++j) {
                if (this.playerBoard[i][j] <= 8) --coveredCellLeft;
                else if (this.playerBoard[i][j] == FLAG) --mineLeft;
            }
            this.step = 2;
        }

        public DebugSweeper(MineSweeper game) {
            super(game.getRow(), game.getCol(), game.getMineCount(), true, game.getGameRule());
            this.playerBoard = game.getPlayerBoard();
            this.coveredCellLeft = game.getUncheckedCellLeft() - game.getMineLeft();
            this.mineLeft = game.getMineLeft();
            this.step = 2;
        }
    }

    /**
     * 计算概率时后续可能用到的返回值太多了, 遂单独列个类
     */
    public static class ProbResult {
        public List<List<Point>> ccList;
        public int[][] ccGraph;
        public List<Map<Integer, int[]>> ccPermList;
        public double[][] probGraph;

        public ProbResult() {}
        public ProbResult(List<List<Point>> ccList, int[][] ccGraph, List<Map<Integer, int[]>> ccPermList, double[][] probGraph) {
            this.ccList = ccList;
            this.ccGraph = ccGraph;
            this.ccPermList = ccPermList;
            this.probGraph = probGraph;
        }
    }
}
