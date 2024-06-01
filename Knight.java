
import java.util.HashSet;
import java.util.Set;
public class Knight extends Piece {

    public Knight(boolean color, int position, Board board){
        super(color, position, board, "Knight");
    }
    public Knight(Knight p, Board board){super(p, board);}

    public void updateMoves(){
        Projection p = new Projection(position, color, board);
        p.projectKnight(true);
        legalMoves = p.moves();
        board.filterLegalMoves(legalMoves);

        defenders = p.getDefenders();
        attackers = p.getAttackers();

        p.clear();
        attacks = new HashSet<Piece>();
        for(Move move: legalMoves){
            if(move.capture() != null) attacks.add(move.capture());
        }
        p.projectKnight(false);
        defends = p.getDefends();
        p.clear();
    }
    public String toString(){
        return this.color ? "N" : "n";
    }
}
