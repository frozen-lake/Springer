import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

public class Chess {
    public Board board;
    private boolean userColor;

    // Starts a new game of chess.
    public Chess(){
        board = new Board();
        board.sideToMove = true;
        userColor = true;
    }

    public void startGameW(){
        Scanner in = new Scanner(System.in);
        String text = "White to move: ";
        String s = "";
        SpringerAI sp = new SpringerAI(board);

        while(board.winner == null){
            printBoard();
            System.out.println("Springer eval: "+sp.evaluate(null, false));
            if (board.sideToMove && board.kingW.inCheck()) text = "Check! " + text;
            System.out.print(text);
            s = in.nextLine();
            if (s.equals("O-O") || s.equals("o-o")) {
                // Kingside castles
                King playerKing = board.sideToMove ? board.kingW : board.kingB;
                if (!playerKing.canCastleShort()) {
                    text = board.sideToMove ? "Invalid move. White to move: " : "Invalid move. Black to move: ";
                    continue;
                }
                board.makeMove(board.createMove(playerKing.position, playerKing.position + 2));
                text = board.sideToMove ? "White to move: " : "Black to move: ";
                continue;
            } else if (s.equals("O-O-O") || s.equals("o-o-o")) {
                King playerKing = board.sideToMove ? board.kingW : board.kingB;
                if (!playerKing.canCastleLong()) {
                    text = board.sideToMove ? "Invalid move. White to move: " : "Invalid move. Black to move: ";
                    continue;
                }
                board.makeMove(board.createMove(playerKing.position, playerKing.position - 2));
                text = board.sideToMove ? "White to move: " : "Black to move: ";
                continue;
            } else if (s.length() == 5 && s.charAt(2) == ' ') {
                int from = Board.positionToInt(String.valueOf(s.charAt(0)) + String.valueOf(s.charAt(1)));
                int to = Board.positionToInt(String.valueOf(s.charAt(3)) + String.valueOf(s.charAt(4)));
                if (!board.validPosition(from) || !board.validPosition(to) || board.get(from) == null
                        || board.get(from).color != board.sideToMove || !board.canMoveTo(from, to)) {
                    // Invalid move
                    text = board.sideToMove ? "Invalid move. White to move: " : "Invalid move. Black to move: ";
                    continue;
                } else if (board.isPromotion(from, to)) {
                    // Legal promotion, prompt piece
                    Move m;
                    String str;
                    while (true) {
                        System.out.println("Options are Q, R, N, B.");
                        System.out.print("Select promotion piece: ");
                        str = in.nextLine();
                        if (Pawn.promotions.contains(str)) {
                            m = board.createMove(from, to, str);
                            break;
                        }
                    }
                    board.makeMove(m);
                    text = board.sideToMove ? "White to move: " : "Black to move: ";
                    continue;
                } else {
                    // Legal move
                    board.makeMove(board.createMove(from, to));
                    text = board.sideToMove ? "White to move: " : "Black to move: ";
                    continue;
                }
            } else {
                text = board.sideToMove ? "Invalid move. White to move: " : "Invalid move. Black to move: ";
                continue;
            }
        }
    }

    public void startGameSpringerAI(){
        Scanner in = new Scanner(System.in);
        String c = userColor ? "White" : "Black";
        String text = c + " to move: ";
        String s = "";
        SpringerAI sp = new SpringerAI(board);
        while(board.winner == null){
            if(board.sideToMove == userColor) {
                printBoard();
                if (board.sideToMove && board.kingW.inCheck()) text = "Check! " + c + " to move: ";
                System.out.print(text);
                s = in.nextLine();
                if (s.equals("O-O") || s.equals("o-o")) {
                    // Kingside castles
                    King playerKing = board.sideToMove ? board.kingW : board.kingB;
                    if (!playerKing.canCastleShort()) {
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    }
                    board.makeMove(board.createMove(playerKing.position, playerKing.position + 2));
                    text = c + " to move: ";
                    continue;
                } else if (s.equals("O-O-O") || s.equals("o-o-o")) {
                    King playerKing = board.sideToMove ? board.kingW : board.kingB;
                    if (!playerKing.canCastleLong()) {
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    }
                    board.makeMove(board.createMove(playerKing.position, playerKing.position - 2));
                    text = text = c + " to move: ";
                    continue;
                } else if (s.length() == 5 && s.charAt(2) == ' ') {
                    int from = Board.positionToInt(String.valueOf(s.charAt(0)) + String.valueOf(s.charAt(1)));
                    int to = Board.positionToInt(String.valueOf(s.charAt(3)) + String.valueOf(s.charAt(4)));
                    if (!board.validPosition(from) || !board.validPosition(to) || board.get(from) == null
                            || board.get(from).color != board.sideToMove || !board.canMoveTo(from, to)) {
                        // Invalid move
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    } else if (board.isPromotion(from, to)) {
                        // Legal promotion, prompt piece
                        Move m;
                        String str;
                        while (true) {
                            System.out.println("Options are Q, R, N, B.");
                            System.out.print("Select promotion piece: ");
                            str = in.nextLine();
                            if (Pawn.promotions.contains(str)) {
                                m = board.createMove(from, to, str);
                                break;
                            }
                        }
                        board.makeMove(m);
                        text = c + " to move: ";
                        continue;
                    } else {
                        // Legal move
                        board.makeMove(board.createMove(from, to));
                        text = c + " to move: ";
                        continue;
                    }
                } else {
                    text = "Invalid move. " + c + " to move: ";
                    continue;
                }
            } else {
                System.out.println("AI thinking...");
                sp.takeTurn();
            }
        }
        printBoard();
        System.out.println("Winner is " + (board.winner?"white":"black"));
    }
    public void startGameSpringerTest(){
        String[] moves = {"e2 e4", "d2 d4", "e4 d5", "b1 c3", "d1 h5", "h5 d1", "f1 c4", "g1 e2", "d5 d6", "c4 d5", "d6 e7", "e7 d8", "e2 f4"};
        int i = -1;
        String c = userColor ? "White" : "Black";
        String text = c + " to move: ";
        String s = "";
        SpringerAI sp = new SpringerAI(board);
        while(board.winner == null){
            if(board.sideToMove == userColor) {
                i++;
                if (board.sideToMove && board.kingW.inCheck()) text = "Check! " + c + " to move: ";
                System.out.print(text);
                s = moves[i];System.out.println(moves[i]);
                if (s.equals("O-O") || s.equals("o-o")) {
                    // Kingside castles
                    King playerKing = board.sideToMove ? board.kingW : board.kingB;
                    if (!playerKing.canCastleShort()) {
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    }
                    board.makeMove(board.createMove(playerKing.position, playerKing.position + 2));
                    text = c + " to move: ";
                    continue;
                } else if (s.equals("O-O-O") || s.equals("o-o-o")) {
                    King playerKing = board.sideToMove ? board.kingW : board.kingB;
                    if (!playerKing.canCastleLong()) {
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    }
                    board.makeMove(board.createMove(playerKing.position, playerKing.position - 2));
                    text = text = c + " to move: ";
                    continue;
                } else if (s.length() == 5 && s.charAt(2) == ' ') {
                    int from = Board.positionToInt(String.valueOf(s.charAt(0)) + String.valueOf(s.charAt(1)));
                    int to = Board.positionToInt(String.valueOf(s.charAt(3)) + String.valueOf(s.charAt(4)));
                    if (!board.validPosition(from) || !board.validPosition(to) || board.get(from) == null
                            || board.get(from).color != board.sideToMove || !board.canMoveTo(from, to)) {
                        // Invalid move
                        text = "Invalid move. " + c + " to move: ";
                        continue;
                    } else if (board.isPromotion(from, to)) {
                        // Legal promotion, prompt piece
                        Move m;
                        String str;
                        while (true) {
                            System.out.println("Options are Q, R, N, B.");
                            System.out.print("Select promotion piece: ");
                            str = "Q";
                            if (Pawn.promotions.contains(str)) {
                                m = board.createMove(from, to, str);
                                break;
                            }
                        }
                        board.makeMove(m);
                        text = c + " to move: ";
                        continue;
                    } else {
                        // Legal move
                        board.makeMove(board.createMove(from, to));
                        text = c + " to move: ";
                        continue;
                    }
                } else {
                    text = "Invalid move. " + c + " to move: ";
                    continue;
                }
            } else {
                printBoard();
                System.out.println("AI thinking...");
                sp.takeTurn();
            }
        }
    }
    public void printBoard(){
        if(board.sideToMove) board.printBoardW();
        if(!board.sideToMove) board.printBoardB();
    }

    public static void main(String[] args){
        Chess c = new Chess();
        c.startGameSpringerAI();
        //c.startGameSpringerTest();
    }

}
