public class Queen extends Piece {

    public Queen(boolean color, int position){
        super(color, position);
    }
    public Move[] getMoves(){
        // Direct projection in one direction


        return new Move[0];
    }
    public String toString(){
        if(this.color){
            return "Q";
        } else {
            return "q";
        }
    }
}
