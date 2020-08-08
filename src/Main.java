import java.util.Scanner;

public class Main {
    public static final String VERSION = "0.9-ea";
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
    private static int round;
    private static MineSweeper game;

    public static void main(String[] args) {
        if (args.length == 0 || args[0].equals("gui")) new Gui();
        else if (args[0].contains("t")) testAI(args);
        else if (args[0].contains("c")) cli();
        else if (args[0].contains("v")) {
            System.out.println("Java 扫雷 | 作者: 蟹恼板 | 版本: " + VERSION);
        }
        else if (args[0].contains("h")) {
            System.out.println("参数    \t描述");
            System.out.println("------- \t-------");
            System.out.println("(无)    \tGUI 入口");
            System.out.println("gui     \tGUI 入口");
            System.out.println("cli     \tCLI 入口");
            System.out.println("test    \t测试 AI 胜率 (详情输入 test --help)");
            System.out.println("version \t查看版本");
            System.out.println("help    \t查看帮助");
        }
        else System.out.println("参数错误. 输入 help 查看更多信息.");
    }

    /**
     * CLI 扫雷入口
     * 没仔细做, 算是个对 MineSweeper 提供的 API 的简单应用展示
     */
    private static void cli() {
        Scanner sc = new Scanner(System.in);
        System.out.print("输入 行 列 雷数: ");
        MineSweeper game = new MineSweeper(sc.nextInt(), sc.nextInt(), sc.nextInt());
        sc.nextLine();
        while (true) {
            System.out.print("输入操作: ");
            String[] opList = sc.nextLine().split(" ");
            char op = (opList.length > 0 && opList[0].length() == 1) ? opList[0].charAt(0) : 'h';
            int x = -1, y = -1;
            if (op == 'l' || op == 'r' || op == 'c') {
                if (opList.length == 3) {
                    x = Integer.parseInt(opList[1]);
                    y = Integer.parseInt(opList[2]);
                }
                else op = 'h';
            }
            int success;
            switch (op) {
                case 'l': success = game.dig(x, y); break;
                case 'r': success = game.mark(x, y); break;
                case 'c': success = game.check(x, y); break;
                case 'a': AutoSweeper.sweepToEnd(game); success = game.getGameState(); break;
                default:
                    System.out.println("l <x> <y>    模拟左键 (揭开)");
                    System.out.println("r <x> <y>    模拟右键 (标旗, 标问号)");
                    System.out.println("c <x> <y>    模拟左右同时按 (检测周围格子)");
                    System.out.println("a            使用 AI 扫完全部");
                    continue;
            }
            game.printPlayerBoardToConsole();
            if (success != MineSweeper.PROCESS) {
                System.out.println(success == MineSweeper.WIN ? "胜利!" : "失败!");
                break;
            }
        }
    }

    /**
     * AI 测试
     * 反复运行若干局, 计算胜率
     * @param args 执行参数
     */
    private static void testAI(String[] args) {
        // 从命令行获取参数
        int times = 10000, difficulty = MineSweeper.DIFFICULTY_EXPERT, gameRule = MineSweeper.GAME_RULE_WIN_XP;
        for (int i = 1; i < args.length; i += 2) {
            String nextArg = i + 1 < args.length ? args[i + 1] : "";
            boolean error = false;
            switch (args[i]) {
                case "-t": case "--times":
                    try { times = Integer.parseInt(nextArg); }
                    catch (Exception ignored) { error = true; }
                    break;
                case "-r": case "--rule":
                    if (nextArg.contains("7")) gameRule = MineSweeper.GAME_RULE_WIN_7;
                    else if (nextArg.contains("xp")) {
                        gameRule = MineSweeper.GAME_RULE_WIN_XP;
                    }
                    else if (nextArg.contains("XP")) {
                        gameRule = WinXpSweeper.GAME_RULE_REAL_WIN_XP;
                    }
                    else error = true;
                    break;
                case "-d": case "--difficulty":
                    switch (nextArg) {
                        case "1": case "beg": case "beginner":
                            difficulty = MineSweeper.DIFFICULTY_BEGINNER;
                            break;
                        case "2": case "int": case "intermediate":
                            difficulty = MineSweeper.DIFFICULTY_INTERMEDIATE;
                            break;
                        case "3": case "exp": case "expert":
                            difficulty = MineSweeper.DIFFICULTY_EXPERT;
                            break;
                        default: error = true;
                    }
                    break;
                case "-h": case "--help":
                    System.out.println("--times        -t  测试的局数, 默认 10000 次.");
                    System.out.println("--rule         -r  测试的游戏规则, xp 或 7 或 XP, 默认 xp.");
                    System.out.println("                   xp 为模拟 WinXP 扫雷的规则, 运行速度快;");
                    System.out.println("                   XP 直接截屏并鼠标操作 winmine.exe, 运行速度慢;");
                    System.out.println("                   7  为模拟 Win7 扫雷的规则, 运行速度快.");
                    System.out.println("--difficulty   -d  测试的游戏难度, 初级 (beg, 1), 中级 (int, 2), 高级 (exp, 3).");
                    System.out.println("                   当 rule 为 XP 时该参数不起作用.");
                    return;
                default: error = true;
            }
            if (error) {
                System.out.println("参数格式错误, 键入 test --help 以获得帮助.");
                return;
            }
        }
        if (gameRule == WinXpSweeper.GAME_RULE_REAL_WIN_XP) {
            WinXpSweeper tmp = new WinXpSweeper();
            if (tmp.getRow() == 16 && tmp.getCol() == 30 && tmp.getMineCount() == 99) {
                difficulty = WinXpSweeper.DIFFICULTY_EXPERT;
            }
            else if (tmp.getRow() == 16 && tmp.getCol() == 16 && tmp.getMineCount() == 40) {
                difficulty = WinXpSweeper.DIFFICULTY_INTERMEDIATE;
            }
            else if (tmp.getRow() == 9 && tmp.getCol() == 9 && tmp.getMineCount() == 10) {
                difficulty = WinXpSweeper.DIFFICULTY_BEGINNER;
            }
            else difficulty = WinXpSweeper.DIFFICULTY_CUSTOM;
        }

        // 输出参数
        System.out.printf("执行次数: %d   游戏规则: %s   难度: %s", times,
                gameRule == MineSweeper.GAME_RULE_WIN_XP ? "WinXP" : (
                    gameRule == MineSweeper.GAME_RULE_WIN_7 ? "Win7" : "WinXP (winmine.exe)"
                ),
                difficulty == WinXpSweeper.DIFFICULTY_CUSTOM ? "??" : (
                    difficulty == MineSweeper.DIFFICULTY_BEGINNER ? "初级" : (
                        difficulty == MineSweeper.DIFFICULTY_INTERMEDIATE ? "中级" : "高级"
                    )
                )
        );
        System.out.println();

        //开始运算
        int winCnt = 0;
        int[] exploreRateView = new int[11];
        // 如果遇到连通分量特别长导致运算时间很久, 每隔2秒输出一次
        Thread th = new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (game == null) break;
                        long diff = System.currentTimeMillis() - time;
                        if (diff > 5000) {
                            System.out.println();
                            System.out.println("第 " + round + " 局耗时超预期, 可能是因为连通分量太长. 当前步数: "
                                    + game.getStep() + ". 当前连通分量: ");
                            AutoSweeper.printConnectedComponent(AutoSweeper.findAllConnectedComponents(game).getValue());
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
        long printTime = startTime;
        long winTime = 0;
        for (round = 1; round <= times; ++round) {
            long roundStartTime = time = System.currentTimeMillis();
            try {
                if (gameRule == WinXpSweeper.GAME_RULE_REAL_WIN_XP) {
                    game = new WinXpSweeper(true);
                }
                else  game = new MineSweeper(difficulty, gameRule);
//                game = new MineSweeper(badMineBoardExample);
                AutoSweeper.sweepToEnd(game);
            }
            catch (Exception e) {
                e.printStackTrace();
                times = round;
                break;
            }
            long roundEndTime = System.currentTimeMillis();
            boolean win = game.getGameState() == MineSweeper.WIN;
            int exploreRate = 100;
            if (win) {
                ++winCnt;
                winTime += roundEndTime - roundStartTime;
            }
            else {
                // 计算一下棋盘被探索的程度
                int explored = 0;
                for (int[] row : game.getPlayerBoard()) for (int v : row) {
                    if (v < 9 || v == MineSweeper.FLAG) ++explored;
                }
                exploreRate = 100 * explored / (game.getRow() * game.getCol());
            }
            ++exploreRateView[exploreRate / 10];
            if (roundEndTime - printTime > 120) {
                printTime = roundEndTime;
                System.out.printf(
                        "第 %d 局: %s   探索程度: %s   当前胜率: %.4f%%   平均胜局耗时: %d毫秒   平均每局耗时: %d毫秒    \r",
                        round, (win ? "胜" : "负"),
                        (exploreRate < 10 ? "  " : (exploreRate < 100 ? " " : "")) + exploreRate + "%",
                        (double) winCnt / (double) round * 100, winTime / Math.max(winCnt, 1),
                        (roundEndTime - startTime) / round);
            }
        }
        game = null;
        long totalTime = System.currentTimeMillis() - startTime;
        for (int i = 0; i < exploreRateView.length; ++i) {
            exploreRateView[i] = (int)Math.ceil(10.0 * exploreRateView[i] / times);
        }
        System.out.print("                                                         \r");
        System.out.printf("运行局数: %d   胜率: %.2f%%   运行总耗时: %d秒   平均胜局耗时: %d毫秒   平均每局耗时: %d毫秒        ",
                times, (double)winCnt / (double)times * 100, totalTime / 1000, winTime / Math.max(winCnt, 1), totalTime / times);
        System.out.println();
        System.out.println("探索程度统计: ");
        System.out.println("A 占比");
        for (int i = 1; i < 10; ++i) {
            System.out.print("|");
            for (int v :exploreRateView) System.out.print(v >= 10 - i ? "  M " : "    ");
            System.out.println();
        }
        System.out.println("+---------------------------------------------> 探索程度");
        System.out.println("   0% 10% 20% 30% 40% 50% 60% 70% 80% 90% 100%");
    }
}
