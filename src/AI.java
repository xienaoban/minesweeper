import javafx.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class AI {
    public static final int UNKNOWN = 0;
    public static final int MINE = 1;
    public static final int NOT_MINE = -1;

    /**
     * 仅通过检测周围8格，确定已知数字的格子周围剩下的未知格子是否全部为雷
     * @param game
     * @param x
     * @param y
     * @return 周围的Unchecked格子全为（或全不为）雷、或未知
     */
    public static int checkUncoveredCellBasically(Game game, int x, int y) {
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
    public static int checkUncheckedCellBasically(Game game, int x, int y) {
        if (game.getPlayerBoard(x, y) != Game.UNCHECKED
                && game.getPlayerBoard(x, y) != Game.QUESTION) return UNKNOWN;
        List<Pair<Integer, Integer>> around = game.getAround(x, y);
        for (Pair<Integer, Integer> p : around) {
            int px = p.getKey(), py = p.getValue();
            int pState = game.getPlayerBoard(px, py);
            if (pState >= 0 && pState <= 8) {
                if (checkUncoveredCellBasically(game, px, py) == MINE) return MINE;
                if (checkUncoveredCellBasically(game, px, py) == NOT_MINE) return NOT_MINE;
            }
        }
        return UNKNOWN;
    }

    /**
     * 扫描全盘，仅通过周围八格信息，判断是否存在必为雷或必不为雷的格子
     * @param game
     * @return int数组第一个值代表类型，第二、第三个值代表坐标
     */
    public static int[] checkAllBasically(Game game) {
        for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
            int type = checkUncheckedCellBasically(game, x, y);
            if (type == UNKNOWN) continue;
            return new int[]{type, x, y};
        }
        return new int[]{UNKNOWN};
    }

    /**
     * 仅通过周围八格信息，找出所有必为雷或必不为雷的格子
     * @param game
     */
    public static void sweepAllBasically(Game game) {
        boolean swept;
        do {
            swept = false;
            for (int x = 0; x < game.getRow(); ++x) for (int y = 0; y < game.getCol(); ++y) {
                int type = checkUncoveredCellBasically(game, x, y);
                if (type == UNKNOWN) continue;
                swept = true;
                for (Pair<Integer, Integer> p : game.getAround(x, y)) {
                    int px = p.getKey(), py = p.getValue();
                    if (game.getPlayerBoard(px, py) != Game.UNCHECKED
                            && game.getPlayerBoard(px, py) != Game.QUESTION) continue;
                    if (type == MINE) game.setFlag(px, py);
                    else if (type == NOT_MINE) game.uncover(px, py);
                    if (game.getGameState() == Game.FAIL) return;
                }
            }
        } while (swept);
    }

    public static int[][] findAllConnectedComponents(Game game) {
        final int _VISITED = -233, _UNKNOWN = 0;
        int[][] components = new int[game.getRow()][game.getCol()];
        int id = 1;
        for (int i = 0; i < game.getRow(); ++i) for (int j = 0; j < game.getCol(); ++j) {
            if (components[i][j] != _UNKNOWN || game.getPlayerBoard(i, j) > 8) continue;
            Queue<Pair<Integer, Integer>> que = new LinkedList<>();
            que.offer(new Pair<>(i, j));
            boolean findANewComponent = false;
            while (!que.isEmpty()) {
                Pair<Integer, Integer> cur = que.poll();
                int cx = cur.getKey(), cy = cur.getValue();
                if (components[cx][cy] == _VISITED) continue;
                components[cx][cy] = _VISITED;
                for (Pair<Integer, Integer> p : game.getAround(cx, cy)) {
                    int px = p.getKey(), py = p.getValue();
                    if (game.getPlayerBoard(px, py) != Game.UNCHECKED) continue;
                    findANewComponent = true;
                    components[px][py] = id;
                    for (Pair<Integer, Integer> p2 : game.getAround(px, py)) {
                        if (game.getPlayerBoard(p2.getKey(), p2.getValue()) < 9) que.offer(p2);
                    }
                }
            }
            if (findANewComponent) ++id;
        }
        return components;
    }
}
