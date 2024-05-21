import java.util.Set;

public class Queen extends Piece {

    public Queen(boolean color, int position){
        super(color, position);
        this.type = "Queen";
    }
    public Set<Move> getMoves(Board board){
        return null;
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
