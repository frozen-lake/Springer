import java.util.Set;

public class Bishop extends Piece {

    public Bishop(boolean color, int position, Board board){
        super(color, position, board, "Bishop");
    }
    public Bishop(Bishop p, Board board){super(p, board);}
    @Override
    public Set<Move> getMoves() {
        Projection p = new Projection(position, true, board);
        p.projectDiagonal();
        Set<Move> moves = p.moves();
        board.filterLegalMoves(moves);
        return moves;
    }


    public String toString(){
        return this.color ? "B" : "b";
    }
}
