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
        Set<Move> moves = p.moves();
        board.filterLegalMoves(moves);
        this.legalMoves = moves;

        p.clear();
        defenders = p.getDefenders();
        attackers = p.getAttackers();

    }
//    public int calculateScope(){
//        return 0;
//    }


    public String toString(){
        return this.color ? "B" : "b";
    }
}
