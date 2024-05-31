import java.util.List;
import java.util.Set;
import java.util.HashSet;

public class Pawn extends Piece {

    public boolean doubled; // Doubled pawn

    public static final Set<String> promotions = new HashSet<String>(List.of(new String[]{"Q", "R", "N", "B"}));
    public Pawn(Pawn p, Board board){super(p, board);}
    public Pawn(boolean color, int position, Board board){
        super(color, position, board, "Pawn");
    }
    public void updateMoves() {
        Projection p = new Projection(position, color, board);
        p.projectPawnAttack(true);
        p.projectPawnMove();
        legalMoves = p.moves();
        board.filterLegalMoves(legalMoves);

        defenders = p.getDefenders();
        attackers = p.getAttackers();

        doubled = true;
        for (int i = position;board.validPosition(i);i += (color?8:-8)) {
            if (board.get(i) != null && board.get(i).type.equals("Pawn") && board.get(i).color==color) doubled = false;
        }
    }

    //
    public void promote(String promotion){
        switch (promotion) {
            case "Q" -> board.set(position, new Queen(color, position, board));
            case "R" -> board.set(position, new Rook(color, position, board));
            case "N" -> board.set(position, new Knight(color, position, board));
            case "B" -> board.set(position, new Bishop(color, position, board));
        }
    }
    public boolean doubled(){return doubled;}
    public String toString(){
        return this.color ? "P" : "p";
    }
}
