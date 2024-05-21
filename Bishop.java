public class Bishop extends Piece {

    public Bishop(boolean color, int position){
        super(color, position);
    }
    public Move[] getMoves(){
        // Direct projection in one direction


        return new Move[0];
    }
    public String toString(){
        return this.color ? "B" : "b";
    }
}
