import java.util.Set;

public class Queen extends Piece {

    public Queen(boolean color, int position, Board board){
        super(color, position, board, "Queen");
    }
    public Queen(Queen p, Board board){super(p, board);}
    public Set<Move> getMoves(){
        Set<Move> moves = board.diagonalProjection(position, true);
        moves.addAll(board.straightProjection(position, true));

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
