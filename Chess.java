import java.util.HashSet;
import java.util.LinkedList;
import java.util.Scanner;
import java.util.Set;

public class Chess {
    public Board board;
    private boolean playingColor;
    private boolean userColor;

    // Starts a new game of chess.
    public Chess(){
        board = new Board();
        playingColor = true;
        userColor = true;
    }

    public void startGameW(){
        Scanner in = new Scanner(System.in);
        String text = "White to move: ";
        String s = "";

        while(board.winner == null){
            printBoard();
            if(playingColor && board.kingW.inCheck()) text = "Check! " + text;
            System.out.print(text);
            s = in.nextLine();
            if(s.equals("O-O") || s.equals("o-o")) {
                // Kingside castles
                King playerKing = playingColor ? board.kingW : board.kingB;
                if(!playerKing.canCastleShort()){
                    text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                    continue;
                }
                board.makeMove(board.createMove(playerKing.position, playerKing.position + 2));
                playingColor = !playingColor;
                text = playingColor ? "White to move: " : "Black to move: ";
                continue;

            } else if(s.equals("O-O-O") || s.equals("o-o-o")){
                King playerKing = playingColor ? board.kingW : board.kingB;
                if(!playerKing.canCastleLong()){
                    text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                    continue;
                }
                board.makeMove(board.createMove(playerKing.position, playerKing.position - 2));
                playingColor = !playingColor;
                text = playingColor ? "White to move: " : "Black to move: ";
                continue;
            } else if(s.length() == 5 && s.charAt(2) == ' ') {
                int from = Board.positionToInt(String.valueOf(s.charAt(0))+String.valueOf(s.charAt(1)));
                int to = Board.positionToInt(String.valueOf(s.charAt(3))+String.valueOf(s.charAt(4)));

                if(!board.validPosition(from) || !board.validPosition(to) || board.get(from)==null
                        || board.get(from).color != playingColor || !board.canMoveTo(from, to)){
                    // Invalid move
                    text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                    continue;
                } else if(board.isPromotion(from, to)) {
                    // Legal promotion, prompt piece
                    Move m;
                    String str;
                    while(true){
                        System.out.println("Options are Q, R, N, B.");
                        System.out.print("Select promotion piece: ");
                        str = in.nextLine();
                        if(Pawn.promotions.contains(str)){
                            m = board.createMove(from, to, str);
                            break;
                        }
                    }
                    board.makeMove(m);
                    playingColor = !playingColor;
                    text = playingColor ? "White to move: " : "Black to move: ";
                    continue;
                } else {
                    // Legal move
                    board.makeMove(board.createMove(from, to));
                    playingColor = !playingColor;
                    text = playingColor ? "White to move: " : "Black to move: ";
                    continue;
                }
            } else {
                text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                continue;
            }

        }

    }
    public void printBoard(){
        if(playingColor) board.printBoardW();
        if(!playingColor) board.printBoardB();
    }

    public static void main(String[] args){
        Chess c = new Chess();
        c.startGameW();
    }

}
