import java.util.Set;

public class Bishop extends Piece {

    public Bishop(boolean color, int position){
        super(color, position);
        this.type = "Bishop";
    }

    @Override
    public Set<Move> getMoves(Board board) {
        return null;
    }


    public String toString(){
        return this.color ? "B" : "b";
    }
}
