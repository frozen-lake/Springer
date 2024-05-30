import java.util.Set;

public class Queen extends Piece {

    public Queen(boolean color, int position, Board board){
        super(color, position, board, "Queen");
    }
    public Queen(Queen p, Board board){super(p, board);}
    public void updateMoves(){
        Projection p = new Projection(position, true, board);
        p.projectDiagonal(); p.projectStraight();
        Set<Move> moves = p.moves();

        board.filterLegalMoves(moves);
        this.legalMoves = moves;
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
