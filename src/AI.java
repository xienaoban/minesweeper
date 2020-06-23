import javafx.util.Pair;

import java.util.*;

public class AI {
    public static final int UNKNOWN = 0;
    public static final int MINE = 1;
    public static final int NOT_MINE = -1;

    public static final int CC_VISITED = -233;
    public static final int CC_UNKNOWN = 0;

    /**
     * 仅通过检测周围8格，确定已知数字的格子周围剩下的未知格子是否全部为雷
     * @param game
     * @param x
     * @param y
     * @return 周围的Unchecked格子全为（或全不为）雷、或未知
     */
    public static int checkUncoveredCellBasic(Game game, int x, int y) {
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
     * 仅通过检测周围8格，确定目标未知格子是否有雷
     * @param game
     * @param x
     * @param y
     * @return AI认为是有、无雷还是未知
     */
    public static int checkUncheckedCellBasic(Game game, int x, int y) {
        if (game.getPlayerBoard(x, y) != Game.UNCHECKED
                && game.getPlayerBoard(x, y) != Game.QUESTION) return UNKNOWN;
        List<Pair<Integer, Integer>> around = game.getAround(x, y);
        for (Pair<Integer, Integer> p : around) {
            int px = p.getKey(), py = p.getValue();
            int pState = game.getPlayerBoard(px, py);
            if (pState >= 0 && pState <= 8) {
                if (checkUncoveredCellBasic(game, px, py) == MINE) return MINE;
                if (checkUncoveredCellBasic(game, px, py) == NOT_MINE) return NOT_MINE;
            }
        }
        return UNKNOWN;
    }

    /**
     * 扫描全盘，仅通过周围八格信息，判断是否存在必为雷或必不为雷的格子
     * @param game
     * @return int数组第一个值代表类型，第二、第三个值代表坐标
     */
    public static int[] checkAllBasic(Game game) {
        for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
            int type = checkUncheckedCellBasic(game, x, y);
            if (type == UNKNOWN) continue;
            return new int[]{type, x, y};
        }
        return new int[]{UNKNOWN};
    }

    /**
     * 仅通过周围八格信息，找出所有必为雷或必不为雷的格子
     * @param game
     */
    public static void sweepAllBasic(Game game) {
        boolean swept;
        do {
            swept = false;
            for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
                int type = checkUncoveredCellBasic(game, x, y);
                if (type == UNKNOWN) continue;
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
        } while (swept);
    }

    /**
     * 检查已知是数字的一个格子，判断对其周围是否是雷的判定是否合法
     * @param game
     * @param board 棋局。标记为 MINE 或 FLAG 说明判定为雷；标记为 NOT_MINE 说明判定为非雷
     * @param x
     * @param y
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

    /**
     * 检查一个非数字的格子，根据周围的数字格子判断该格子在被判定为是雷/非雷的情况下是否合法
     * @param game
     * @param board 棋局。标记为 MINE 或 FLAG 说明判定为雷；标记为 NOT_MINE 说明判定为非雷
     * @param x
     * @param y
     * @return 是否合法
     */
    public static boolean isUncheckedCellLegal(Game game, int[][] board, int x, int y) {
        if (board[x][y] < 9) return false;
        for (Pair<Integer, Integer> p : game.getAround(x, y)) {
            int px = p.getKey(), py = p.getValue();
            if (board[px][py] < 9 && !isUncoveredCellLegal(game, board, px, py)) return false;
        }
        return true;
    }

    /**
     * 寻找所有连通分量
     * 一个连通分量表示，有多种可能性且相互影响的一个区域
     * @param game
     * @return Pair 的 key 储存所有分量的所有点，value 为整个图
     */
    public static Pair<List<List<Pair<Integer, Integer>>>, int[][]> findAllConnectedComponent(Game game) {
        List<List<Pair<Integer, Integer>>> ccList = new ArrayList<>();
        int[][] ccGraph = new int[game.getRow()][game.getCol()];
        int id = 1;
        // 遍历每个点，找到第一个可能属于一个连通分量的点，并从该点扩散开来寻找其他属于该分量的点
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if (ccGraph[i][j] != CC_UNKNOWN || game.getPlayerBoard(i, j) > 8) continue;
            // 找到了一个可能的点。注意该点为已扫出数字的格子（因为一个数字格子周围的未知格子必属于同一分量）
            // 该队列存储的点是已扫出数字的格子，这些个数字格子周围的未知格子也必属于同一分量
            Queue<Pair<Integer, Integer>> que = new LinkedList<>();
            que.offer(new Pair<>(i, j));
            List<Pair<Integer, Integer>> points = new ArrayList<>();
            boolean findANewComponent = false;
            // BFS 遍历周围的点，搜出整个连通分量
            while (!que.isEmpty()) {
                Pair<Integer, Integer> cur = que.poll();
                int cx = cur.getKey(), cy = cur.getValue();
                if (ccGraph[cx][cy] == CC_VISITED) continue;
                ccGraph[cx][cy] = CC_VISITED;
                // 遍历该数字格子周围的所有未知格子，它们属于同一个分量
                for (Pair<Integer, Integer> p : game.getAround(cx, cy)) {
                    int px = p.getKey(), py = p.getValue();
                    if ((game.getPlayerBoard(px, py) != Game.UNCHECKED && game.getPlayerBoard(px, py) != Game.QUESTION)
                            || ccGraph[px][py] == id) continue;
                    findANewComponent = true;
                    points.add(new Pair<>(px, py));
                    ccGraph[px][py] = id;
                    // 找出「数字格子周围的未知格子」的周围的其余数字格子，加入队列（有点绕）
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
     * 计算每个格子有雷的概率（默认当前每个旗子设的都是对的，懒得自检了）
     * @param game
     * @return 每个格子的有雷概率
     */
    public static double[][] calculateProbability(Game game) {
        double[][] prob = new double[game.getRow()][game.getCol()];
        Pair<List<List<Pair<Integer, Integer>>>, int[][]> _ccPair = findAllConnectedComponent(game);
        List<List<Pair<Integer, Integer>>> ccList = _ccPair.getKey();
        int[][] ccGraph = _ccPair.getValue();
        double avgPermMineCnt = 0; // 所有连通分量的排列中雷的平均数

        // 计算每个连通分量的每个点的有雷概率
        for (List<Pair<Integer, Integer>> points : ccList) {
            int permutationCnt = backtrackProbability(game, game.getPlayerBoard(), prob, points,
                    game.getMineLeft(), 0); // 如果 permutationCnt 为 0，说明玩家设的旗有错，记得接收异常
            for (Pair<Integer, Integer> p : points) {
                int px = p.getKey(), py = p.getValue();
                prob[px][py] /= permutationCnt;
                avgPermMineCnt += prob[px][py];
            }
        }

        // 所有未知孤立格子（即周围没有已知数字格子的格子）统一计算概率为“平均剩余雷数/未知孤立格子数”
        int unknownCellCnt = 0;
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if ((game.getPlayerBoard(i, j) == Game.UNCHECKED || game.getPlayerBoard(i, j) == Game.QUESTION)
                    && ccGraph[i][j] == CC_UNKNOWN) ++unknownCellCnt;
        }
        if (unknownCellCnt > 0) {
            double unknownCellProb = ((double) game.getMineLeft() - avgPermMineCnt) / (double) (unknownCellCnt);
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                if (ccGraph[i][j] == CC_UNKNOWN) {
                    if (game.getPlayerBoard(i, j) == Game.UNCHECKED || game.getPlayerBoard(i, j) == Game.QUESTION) {
                        prob[i][j] = unknownCellProb;
                    }
                    else if (game.getPlayerBoard(i, j) == Game.FLAG) prob[i][j] = 1.0;
                }
            }
        }
//        printProbability(prob);
        return prob;
    }

    /**
     * 进阶扫雷 AI，将所有概率为 100% 或 0% 的格子扫掉
     * @param game
     */
    public static void sweepAllAdvanced(Game game) {
        boolean loop = true;
        while (loop && game.getGameState() == Game.PROCESS) {
            loop = false;
            sweepAllBasic(game);
            if (game.getGameState() != Game.PROCESS) break;
            double[][] prob = calculateProbability(game);
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                // 由于我没有在不同连通分量之间建立联系，所以即便算出来为雷概率为 100% 也不一定准，也可能把旗标到了
                // 非雷的格子里，导致最后整个图都标满了但游戏还未结束，所以就不在此标雷了
//                if (prob[i][j] == 1 && game.getPlayerBoard(i, j) != Game.FLAG) game.setFlag(i, j);
                if (prob[i][j] == 0 && game.getPlayerBoard(i, j) == Game.UNCHECKED) {
                    game.uncover(i, j);
                    loop = true;
                }
            }
        }
    }

    /**
     * 完整地玩一局扫雷
     * 猜雷就是在本方法实现的
     * @param game
     */
    public static void sweepToEnd(Game game) {
        while (game.getGameState() == Game.PROCESS) {
            sweepAllAdvanced(game);
            if (game.getGameState() != Game.PROCESS) break;
            double[][] prob = calculateProbability(game);
            int maxX = -1, maxY = -1, corner = -1, intensity = 1;
            for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
                int cellState = game.getPlayerBoard(i, j);
                if (cellState != Game.UNCHECKED && cellState != Game.QUESTION) continue;
                // 同概率的话在角落的格子优先探测，抗疫将概率从 29% 提升到 33%
                if (maxX != -1 && prob[i][j] == prob[maxX][maxY]) {
                    int cor = 0, in = getNumberCellCntAround(game, i, j, 3);
                    if (i == 0 || i == game.getRow() - 1) ++cor;
                    if (j == 0 || j == game.getCol() - 1) ++cor;
                    if ((cor == 2 && cor > corner) || (corner < 2 && in > intensity)) {
                        maxX = i;
                        maxY = j;
                        corner = cor;
                        intensity = in;
                    }
                }
                else if (maxX == -1 || prob[i][j] < prob[maxX][maxY]) {
                    maxX = i;
                    maxY = j;
                }
            }
            // 只找 prob 低的 uncover，不找 prob 高的 setFlag，因为 setFlag 不影响游戏状态，
            // 可能会出现找不到 max_x 的情况。
            game.uncover(maxX, maxY);
        }
    }

    /**
     * 使用回溯法，找出某连通分量中所有的雷的布局可能性
     * @param game
     * @param board 回溯时可被任意修改的棋盘（防止在游戏本体上修改出问题）
     * @param nums 存储所有可能排列下，分量中每个格子是雷的情况个数
     * @param points 一个连通分量所有的点
     * @param cur 当前回溯位置的下标
     * @return 可行排列总数
     */
    private static int backtrackProbability(Game game, int[][] board, double[][] nums,
                                            List<Pair<Integer, Integer>> points, int mineLeft, int cur) {
        // 成功找到一个可能的排列
        if (cur >= points.size()) {
            for (Pair<Integer, Integer> p : points) {
                int px = p.getKey(), py = p.getValue();
                if (board[px][py] == Game.MINE) ++nums[px][py];
            }
            return 1;
        }

        // 分别递归考虑当前格子是雷、不是雷的情况
        int x = points.get(cur).getKey(), y = points.get(cur).getValue();
        int res = 0;
        board[x][y] = Game.MINE;
        if (mineLeft > 0 && isUncheckedCellLegal(game, board, x, y)) {
            res += backtrackProbability(game, board, nums, points, mineLeft - 1, cur + 1);
        }
        board[x][y] = Game.NOT_MINE;
        if (isUncheckedCellLegal(game, board, x, y)) {
            res += backtrackProbability(game, board, nums, points, mineLeft, cur + 1);
        }
        board[x][y] = Game.UNCHECKED;
        return res;
    }

    private static int getNumberCellCntAround(Game game, int x, int y, int radius) {
        int res = 0;
        for (int i = x - radius + 1; i < x + radius; ++i) for (int j = y - radius + 1; j < y + radius; ++j) {
            if (i < 0 || i >= game.getRow() || j < 0 || j >= game.getCol()) continue;
            if (i == x && j == y) continue;
            if (game.getPlayerBoard(x, y) < 9) ++res;
        }
        return res;
    }

    public static void printConnectedComponent(int[][] cc) {
        for (int i = 0; i < cc[0].length; ++i) System.out.print("---");
        System.out.println();
        for (int i = 0; i < cc.length; ++i) {
            for (int j = 0; j < cc[0].length; ++j) {
                String s = String.valueOf(cc[i][j]);
                if (cc[i][j] == -233) s = ".";
                else if (cc[i][j] == 0) s = " ";
                System.out.print(" " + s + " ");
            }
            System.out.println();
        }
        for (int i = 0; i < cc[0].length; ++i) System.out.print("---");
        System.out.println();
    }

    public static void printProbability(double[][] prob) {
        for (int i = 0; i < prob[0].length; ++i) System.out.print("-------");
        System.out.println();
        for (int i = 0; i < prob.length; ++i) {
            for (int j = 0; j < prob[0].length; ++j) System.out.print(String.format(" %.3f ", prob[i][j]));
            System.out.println();
        }
        for (int i = 0; i < prob[0].length; ++i) System.out.print("-------");
        System.out.println();
    }
}
