import java.util.Set;

public class King extends Piece {

    public King(boolean color, int position){
        super(color, position);
        this.type = "King";
    }
    public Set<Move> getMoves(Board board){
        return null;
    }
    public String toString(){
        return this.color ? "K" : "k";
    }
}
