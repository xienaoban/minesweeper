import javafx.util.Pair;

import java.util.List;
import java.util.Scanner;

public class Main {
    private static final boolean T = true, F = false;

    // 能够导致 AI 遍历连通分量时卡死的典型案例
    public static final boolean[][] badMineBoardExample = {
        {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, F, F},
        {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, T, F},
        {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F},
        {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F},
        {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F},
        {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, T, F}
    };

    // 一些在 testAI 及其创建的线程中用到的变量
    private static long time;
    private static Game game;

    public static void main(String[] args) {
        if (args.length == 0) new GUI();
        else if (args[0].equals("test")) testAI(args);
        else if (args[0].equals("cli")) CLI();
        else if (args[0].equals("help")) {
            System.out.println("参数     描述");
            System.out.println(" 无       GUI 入口");
            System.out.println(" cli      CLI 入口");
            System.out.println(" test     测试 AI 胜率，详情输入 test --help");
        }
    }

    /**
     * CLI 扫雷入口
     * 没仔细做，算是个对 Game 提供的 API 的简单应用展示
     */
    private static void CLI() {
        Scanner sc = new Scanner(System.in);
        System.out.print("输入 行 列 雷数：");
        Game game = new Game(sc.nextInt(), sc.nextInt(), sc.nextInt());
        while (true) {
            System.out.print("输入 操作码 x y：");
            int op = sc.nextInt();
            int x = sc.nextInt();
            int y = sc.nextInt();
            int sucess = 0;
            switch (op) {
                case 0: sucess = game.uncover(x, y); break;
                case 1: sucess = game.cycFlagAndQuestion(x, y); break;
                case 2: sucess = game.check(x, y); break;
                case 3: AI.sweepToEnd(game); sucess = game.getGameState(); break;
                case 4:
                    Pair<List<Pair<Integer, Integer>>, List<Pair<Integer, Integer>>> res =
                            AI.checkTwoUncoveredCell(game, x, y, x + 1, y);
                    if (res == null) System.out.println("null");
                    else {
                        System.out.println("uncover: " + res.getKey().size());
                        for (Pair<Integer, Integer> p : res.getKey()) {
                            System.out.println("" + p.getKey() + ", " + p.getValue());
                        }
                        System.out.println("setFlag: " + res.getValue().size());
                        for (Pair<Integer, Integer> p : res.getValue()) {
                            System.out.println("" + p.getKey() + ", " + p.getValue());
                        }
                    }
            }
            game.printPlayerBoardToConsole();
            System.out.println(sucess);
            if (sucess != Game.PROCESS) break;
        }
    }

    /**
     * AI 测试
     * 反复运行若干局，计算胜率
     * @param args 执行参数
     */
    private static void testAI(String[] args) {
        int times = 10000, difficulty = Game.DIFFICULTY_EXPERT, gameRule = Game.GAME_RULE_WIN_XP;
        for (int i = 1; i < args.length; i += 2) {
            String nextArg = i + 1 < args.length ? args[i + 1] : "";
            boolean error = false;
            switch (args[i]) {
                case "-t": case "--times":
                    try { times = Integer.parseInt(nextArg); }
                    catch (Exception ignored) { error = true; }
                    break;
                case "-r": case "--rule":
                    if (nextArg.contains("7")) gameRule = Game.GAME_RULE_WIN_7;
                    else if (nextArg.contains("xp") || nextArg.contains("Xp") || nextArg.contains("XP")) {
                        gameRule = Game.GAME_RULE_WIN_XP;
                    }
                    else error = true;
                    break;
                case "-d": case "--difficulty":
                    switch (nextArg) {
                        case "1": case "beg": case "beginner":
                            difficulty = Game.DIFFICULTY_BEGINNER;
                            break;
                        case "2": case "int": case "intermediate":
                            difficulty = Game.DIFFICULTY_INTERMEDIATE;
                            break;
                        case "3": case "exp": case "expert":
                            difficulty = Game.DIFFICULTY_EXPERT;
                            break;
                        default: error = true;
                    }
                    break;
                case "-h": case "--help":
                    System.out.println("--times         -t 测试的局数，默认 10000 次。");
                    System.out.println("--rule          -r 测试的游戏规则，winxp 或 win7，默认 winxp。");
                    System.out.println("--difficulty    -d 测试的游戏难度，beginner（beg，1）、intermediate（int，2）、expert（exp，3）。");
                    return;
                default: error = true;
            }
            if (error) {
                System.out.println("参数格式错误，键入 -h 或 --help 以获得帮助。");
                return;
            }
        }

        int winCnt = 0;
        int[] exploreRateView = new int[11];
        // 如果遇到连通分量特别长导致运算时间很久，每隔2秒输出一次
        Thread th = new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (game == null) break;
                        long diff = System.currentTimeMillis() - time;
                        if (diff > 2000) {
                            System.out.println("当前棋局：");
                            game.printPlayerBoardToConsole();
                            System.out.println("当前连通分量：");
                            AI.printConnectedComponent(AI.findAllConnectedComponent(game).getValue());
                            time = System.currentTimeMillis();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        long startTime = System.currentTimeMillis();
        for (int t = 1; t <= times; ++t) {
            time = System.currentTimeMillis();
//            game = new Game(badMineBoardExample);
            game = new Game(difficulty, gameRule);
            AI.sweepToEnd(game);
            boolean win = game.getGameState() == Game.WIN;
            int exploreRate = 100;
            if (win) ++winCnt;
            else {
                // 计算一下棋盘被探索的程度
                int explored = 0;
                for (int[] row : game.getPlayerBoard()) for (int v : row) {
                    if (v < 9 || v == Game.FLAG) ++explored;
                }
                exploreRate = 100 * explored / (game.getRow() * game.getCol());
            }
            ++exploreRateView[exploreRate / 10];
            System.out.print("第 " + t + " 局：" + (win ? "胜" : "负"));
            System.out.print("    探索程度：" + (exploreRate < 10 ? "  " : (exploreRate < 100 ? " " : "")) + exploreRate + "%");
            System.out.println("    当前胜率：" + String.format("%.4f", (double)winCnt / (double)t * 100) + "%");
        }
        game = null;
        long totalTime = System.currentTimeMillis() - startTime;
        for (int i = 0; i < exploreRateView.length; ++i) {
            exploreRateView[i] = (int)Math.ceil(10.0 * exploreRateView[i] / times);
        }
        System.out.println();
        System.out.print("胜率：" + ((double)winCnt / (double)times * 100) + "%");
        System.out.print("    运行局数：" + times);
        System.out.print("    运行总耗时：" + (totalTime / 1000) + "秒");
        System.out.println("    平均每局耗时：" + (totalTime / times) + "毫秒");
        System.out.println("探索程度统计：");
        System.out.println("⮝ 占比");
        for (int i = 1; i < 10; ++i) {
            System.out.print("|");
            for (int v :exploreRateView) System.out.print(v >= 10 - i ? "  M " : "    ");
            System.out.println();
        }
        System.out.println("+---------------------------------------------> 探索程度");
        System.out.println("   0% 10% 20% 30% 40% 50% 60% 70% 80% 90% 100%");
    }
}
