public abstract class Piece {

    public final boolean color;
    protected int position;
    protected boolean hasMoved;

    public Piece(boolean color, int position){
        this.color = color;
        this.position = position;
        this.hasMoved = false;
    }

    public void setPosition(int p){
        this.position = p;
    }
    public abstract Move[] getMoves();

}
