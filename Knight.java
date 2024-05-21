
import java.util.Set;
public class Knight extends Piece {

    public Knight(boolean color, int position){
        super(color, position);
        this.type = "Knight";
    }
    public Set<Move> getMoves(Board board){
        return null;
    }
    public String toString(){
        return this.color ? "N" : "n";
    }
}
