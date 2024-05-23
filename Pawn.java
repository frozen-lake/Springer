import java.util.Set;
import java.util.HashSet;

public class Pawn extends Piece {

    public Pawn(boolean color, int position, Board board){
        super(color, position, board, "Pawn");
    }
    public Set<Move> getMoves(){
        int forward = color ? position + 8 : position - 8;
        Set<Move> moves = new HashSet<Move>();

        int[] j = {forward + 1, forward - 1};
        for(int i: j){
            if(board.validPosition(i) && board.get(i) != null
                    && board.get(i).color != color) moves.add(board.createMove(position, i));
        }
        if(board.validPosition(forward) && board.get(forward) == null) {
            moves.add(board.createMove(position, forward));
            int doubleForward = color ? forward + 8 : forward - 8;

            if(board.validPosition(doubleForward) && board.get(doubleForward)==null){
                moves.add(board.createMove(position, doubleForward));
            }
        }


        board.filterLegalMoves(moves);
        return moves;
    }
    public String toString(){
        return this.color ? "P" : "p";
    }
}
