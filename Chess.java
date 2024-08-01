import java.util.*;

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

            System.out.println("Springer eval: "+sp.getEval());
            //sp.debugPieces();

            if (board.sideToMove && board.kingW.inCheck()) text = "Check! " + text;
            System.out.print(text);
            s = in.nextLine();
            if(s.equals("pass")){
                board.makeMove(board.generateNullMove());
                text = board.sideToMove ? "White to move: " : "Black to move: ";
                continue;
            }
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
        //String[] moves = {"e2 e4", "d2 d4", "e4 d5", "b1 c3", "d1 h5", "h5 d1", "f1 c4", "g1 e2", "d5 d6", "c4 d5", "d6 e7", "e7 d8", "e2 f4"};
        //String[] moves = {"e2 e4", "d2 d4", "g2 g3", "c1 e3", "d1 d4", "d4 e5", "e5 g5", "g5 d5", "b2 b4", "b1 d2", "e1 f1", "g1 f3", "f3 e5", "f1 g1", "a1 c1", "c1 f1", "h2 h4", "g1 g2", "h1 h3", "g2 h3", "h3 g4", "f2 f4", "g4 g5", "g3 g4", "f4 f5", "g5 h5"};
        String[] moves = {"e2 e4", "g1 f3", "f1 b5", "O-O", "b5 a4", "a4 b3", "a2 a3", "d2 d3", "f3 g5", "g1 h1", "g5 f3", "d1 e2", "f1 e2", "f1 f2", "e2 f2", "b3 e6", "f2 e2", "f3 d4", "e4 e5", "e2 e5"};

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
        printBoard();
        System.out.println("Winner is " + (board.winner?"white":"black"));
        System.out.println("==="+board.sideToMove + board.get(38) + board.get(38).getMoves());
    }
    public void printBoard(){
        if(board.sideToMove) board.printBoardW();
        if(!board.sideToMove) board.printBoardB();
    }


    public static void main(String[] args){
        Chess c = new Chess();
        //c.startGameW();
        c.startGameSpringerAI();
        //c.startGameSpringerTest();
    }

}
