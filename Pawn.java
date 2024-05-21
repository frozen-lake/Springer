import java.util.Set;

public class Pawn extends Piece {

    public Pawn(boolean color, int position){
        super(color, position);
        this.type = "Pawn";
    }
    public Set<Move> getMoves(Board board){
        return null;
    }
    public String toString(){
        return this.color ? "P" : "p";
    }
}
