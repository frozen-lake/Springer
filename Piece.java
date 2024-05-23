import java.util.Set;

public abstract class Piece {

    public final boolean color;
    public final String type;
    protected int position;
    protected boolean hasMoved;
    public final Board board;

    public Piece(boolean color, int position, Board board, String type){
        this.color = color;
        this.position = position;
        this.hasMoved = false;
        this.board = board;
        this.type = type;
    }

    public void setPosition(int p){ this.position = p; }

    public int getPosition(){ return position; }
    public abstract Set<Move> getMoves();

    public boolean equals(Object o){
        if(!(o instanceof Piece)) return false;
        Piece p = (Piece) o;
        return type.equals(p.type) && color == p.color && position == p.position;
    }


}
