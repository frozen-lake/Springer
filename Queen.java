import java.util.Set;

public class Queen extends Piece {

    public Queen(boolean color, int position, Board board){
        super(color, position, board, "Queen");
    }
    public Queen(Queen p, Board board){super(p, board);}
    public void updateMoves(){
        Projection p = new Projection(position, color, board);
        p.projectDiagonal(true); p.projectStraight(true);
        this.legalMoves  = p.moves();

        board.filterLegalMoves(legalMoves);
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
