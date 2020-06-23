import java.util.Date;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        testAI(10000);
//        new GUI();
//        CLI();
    }

    private static void CLI() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Input row, col and mineCount: ");
        Game game = new Game(sc.nextInt(), sc.nextInt(), sc.nextInt());
        while (true) {
            System.out.print("Input operation, row and col: ");
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

    private static Date time;
    private static Game game;
    private static void testAI(int times) {
        int winCnt = 0;
        Thread th = new Thread() {
            public void run() {
                try {
                    while (true) {
                        Thread.sleep(1000);
                        if (time == null || game == null) break;
                        long diff = new Date().getTime() - time.getTime();
                        if (diff > 6000) {
                            game.printPlayerBoardToConsole();
                            AI.printConnectedComponent(AI.findAllConnectedComponent(game).getValue());
                            time = new Date();
                        }
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        th.start();
        for (int t = 1; t <= times; ++t) {
            time = new Date();
//            game = new Game(9, 9, 10);
//            game = new Game(16, 16, 40);
            game = new Game(16, 30, 99);
//            final boolean O = true, x = false;
//            game = new Game(new boolean[][] {
//                    {x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, x, x},
//                    {x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, O, x},
//                    {x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x},
//                    {x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, x, x},
//                    {x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x, O, x},
//                    {x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, x, x, O, x, O, x}
//            });
            AI.sweepToEnd(game);
            boolean win = game.getGameState() == Game.WIN;
            if (win) ++winCnt;
            System.out.print("Round " + t + ": " + (win ? "Win. " : "Lose."));
            System.out.println("    Current win rate: " + ((double)winCnt / (double)t));
        }
        time = null;
        game = null;
    }
}
