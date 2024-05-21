import java.util.Set;

public abstract class Piece {

    public final boolean color;
    public String type;
    protected int position;
    protected boolean hasMoved;

    public Piece(boolean color, int position){
        this.color = color;
        this.position = position;
        this.hasMoved = false;
        this.type = "";
    }

    public void setPosition(int p){ this.position = p; }

    public int getPosition(){ return position; }
    public abstract Set<Move> getMoves(Board board);

}
