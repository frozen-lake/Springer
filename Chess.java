import java.util.LinkedList;
import java.util.Scanner;

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

        //board.printBoardW();
        while(true){
            board.printBoardW();
            System.out.print(text);
            s = in.nextLine();
            if(s.equals("O-O") || s.equals("O-O-O")) {
                // castle
            } else if(s.length() == 5 && s.charAt(2) == ' ') {
                int from = Board.positionToInt(String.valueOf(s.charAt(0))+String.valueOf(s.charAt(1)));
                int to = Board.positionToInt(String.valueOf(s.charAt(3))+String.valueOf(s.charAt(4)));

                if(!board.validPosition(from) || !board.validPosition(to) || board.get(from)==null || board.get(from).color != playingColor || !board.canMoveTo(from, to)){
                    text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                    continue;
                } else {
                    playingColor = !playingColor;
                    text = playingColor ? "White to move: " : "Black to move: ";
                    board.makeMove(board.createMove(from, to));
                    continue;
                }
            } else {
                text = playingColor ? "Invalid move. White to move: ": "Invalid move. Black to move: ";
                continue;
            }

            playingColor = !playingColor;
        }

    }

    public static void main(String[] args){
        Chess c = new Chess();
        c.startGameW();
    }

}
