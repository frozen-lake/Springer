import java.util.Set;

public class Queen extends Piece {

    public Queen(boolean color, int position, Board board){
        super(color, position, board, "Queen");
    }
    public Set<Move> getMoves(){
        Set<Move> moves = board.diagonalProjection(position);
        moves.addAll(board.straightProjection(position));

        board.filterLegalMoves(moves);
        return moves;
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
