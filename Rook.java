import java.util.Set;

public class Rook extends Piece {

    public Rook(boolean color, int position, Board board){
        super(color, position, board, "Rook");
    }
    public Rook(Rook p, Board board){super(p, board);}

    public Set<Move> getMoves(){
        Set<Move> moves = board.straightProjection(position, true);
        board.filterLegalMoves(moves);
        return moves;
    }
    public String toString(){
        return this.color ? "R" : "r";
    }
}
