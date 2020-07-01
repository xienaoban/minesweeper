import java.util.Date;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        new GUI();
//        testAI(10000);
//        CLI();
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
                case 1: sucess = game.setFlag(x, y); break;
                case 2: sucess = game.unsetFlag(x, y); break;
                case 3: sucess = game.setQuestion(x, y); break;
                case 4: sucess = game.unsetQuestion(x, y); break;
                case 5: sucess = game.check(x, y); break;
            }
            game.printPlayerBoardToConsole();
            System.out.println(sucess);
            if (sucess != Game.PROCESS) break;
        }
    }

    // 一些在 testAI 及其创建的线程中用到的变量
    private static long time;
    private static Game game;

    /**
     * AI 测试
     * 反复运行若干局，计算胜率
     * @param times 执行次数
     */
    private static void testAI(int times) {
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
                            game.printPlayerBoardToConsole();
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
//            game = new Game(9, 9, 10);
//            game = new Game(16, 16, 40);
            game = new Game(16, 30, 99);
//            final boolean T = true, F = false;
//            game = new Game(new boolean[][] { // 超大连通分量导致概率计算巨卡的案例
//                    {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, F, F},
//                    {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, T, F},
//                    {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F},
//                    {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F},
//                    {F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F, T, F},
//                    {F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, F, F, T, F, T, F}
//            });
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
            System.out.println("    当前胜率：" + String.format("%.6f", (double)winCnt / (double)t));
        }
        game = null;
        long totalTime = System.currentTimeMillis() - startTime;
        for (int i = 0; i < exploreRateView.length; ++i) {
            exploreRateView[i] = (int)Math.ceil(10.0 * exploreRateView[i] / times);
        }
        System.out.println();
        System.out.print("运行总耗时：" + (totalTime / 1000) + "秒");
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
