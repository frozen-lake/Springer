import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Board {
    /*
    A board object represents a 8x8 chess board of Piece objects using a 2D ArrayList.
    Columns by rows.

     */
    protected ArrayList<ArrayList<Piece>> board;

    public Board(){
        board = new ArrayList<ArrayList<Piece>>(8);
        for(int i=0;i<8;i++){
            board.add(new ArrayList<Piece>(8));
            for(int j=0;j<8;j++){board.get(i).add(null);}
        }
    }
    public Piece get(int pos){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        return board.get(pos % 8).get(pos / 8);
    }
    public void set(int pos, Piece p){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        board.get(pos % 8).set(pos / 8, p);
    }
    // Returns true if the move is a legal move on this board
    public boolean validateMove(Move m){
        return false;
    }

    // Performs a legal chess move on this board
    public void makeMove(Move m){
        if(!validateMove(m)) throw new IllegalArgumentException();
    }

    public void printBoard(){
        System.out.println("---------------------------------");
        for(int i=0; i<board.size();i++){
            System.out.print("| ");
            for(int j=0;j<board.get(i).size();j++){
                if(board.get(i).get(j) != null){
                    System.out.print(board.get(i).get(j) + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
    }
    public void printBoardr(){
        System.out.println("---------------------------------");
        for(int i=board.size()-1;i>=0;i--){
            System.out.print("| ");
            for(int j=board.get(i).size()-1;j>=0;j--){
                if(board.get(i).get(j) != null){
                    System.out.print(board.get(i).get(j) + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
    }
    public int positionToInt(String pos){
        if(pos.length() > 2) return -1;
        List<Character> cb = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h');
        Character c1 = pos.charAt(0); Character c2 = pos.charAt(1);
        if(!cb.contains(c1)) return -1; if(!Character.isDigit(c2)) return -1;
        return cb.indexOf(c1) + (8 * (Character.getNumericValue(c2) - 1));
    }
    public boolean validPosition(String pos){
        return validPosition(positionToInt(pos));
    }
    protected boolean validPosition(int pos){
        return pos >= 0 && pos <= 63;
    }
    public boolean isPositionEmpty(String pos){
        return isPositionEmpty(positionToInt(pos));
    }
    protected boolean isPositionEmpty(int pos){
        return validPosition(pos) && board.get(pos % 8).get(pos / 8) == null;
    }
    public void populateBoard() {
        // W
        for(int i=0;i<8;i++){
            set(i, new Pawn(true, i));
        }

        // B
        for(int i=0;i<8;i++){
            set(56+i, new Pawn(false, (56)+i));
        }
    }
}
