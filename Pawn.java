public class Pawn extends Piece {

    public Pawn(boolean color, int position){
        super(color, position);
    }
    public Move[] getMoves(){
        // Direct projection in one direction


        return new Move[0];
    }
    public String toString(){
        return this.color ? "P" : "p";
    }
}
