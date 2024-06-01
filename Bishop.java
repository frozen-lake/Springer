import java.util.HashSet;
import java.util.Set;

public class Bishop extends Piece {

    public Bishop(boolean color, int position, Board board){
        super(color, position, board, "Bishop");
    }
    public Bishop(Bishop p, Board board){super(p, board);}
    @Override
    public void updateMoves() {
        Projection p = new Projection(position, color, board);
        p.projectDiagonal(true);
        this.legalMoves = p.moves();
        board.filterLegalMoves(legalMoves);

        defenders = p.getDefenders();
        attackers = p.getAttackers();

        p.clear();
        attacks = new HashSet<Piece>();
        for(Move move: legalMoves){
            if(move.capture() != null) attacks.add(move.capture());
        }
        p.projectDiagonal(false);
        defends = p.getDefends();
        p.clear();

    }
//    public int calculateScope(){
//        return 0;
//    }


    public String toString(){
        return this.color ? "B" : "b";
    }
}
