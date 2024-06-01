import java.util.HashSet;
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

        defenders = p.getDefenders();
        attackers = p.getAttackers();

        p.clear();
        attacks = new HashSet<Piece>();
        for(Move move: legalMoves){
            if(move.capture() != null) attacks.add(move.capture());
        }
        p.projectDiagonal(false); p.projectStraight(false);
        defends = p.getDefends();
        p.clear();
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
