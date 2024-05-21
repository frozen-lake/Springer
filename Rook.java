import java.util.Set;

public class Rook extends Piece {

    public Rook(boolean color, int position){
        super(color, position);
        this.type = "Rook";
    }
    public Set<Move> getMoves(Board board){
        return null;
    }
    public String toString(){
        return this.color ? "R" : "r";
    }
}
