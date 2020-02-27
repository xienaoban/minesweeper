import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        GUI();
    }

    private static void GUI() {
        new UI();
    }

    private static void CLI() {
        Scanner sc = new Scanner(System.in);
        System.out.print("Input row, col and mineCount: ");
        Chessboard game = new Chessboard(sc.nextInt(), sc.nextInt(), sc.nextInt());
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
            if (sucess != Chessboard.PROCESS) break;
        }
    }
}
