import java.util.*;

public class Board {
    /*
    A board object represents a 8x8 chess board of Piece objects using a 2D ArrayList.
    Columns by rows.

     */
    protected ArrayList<ArrayList<Piece>> board;
    private King kingW;
    private King kingB;

    public Board(){
        board = new ArrayList<ArrayList<Piece>>(8);
        for(int i=0;i<8;i++){ // Initialize board
            board.add(new ArrayList<Piece>(8));
            for(int j=0;j<8;j++){board.get(i).add(null);}
        }
        populateBoard();
        kingW = (King) get(4);
        kingB = (King) get(60);
    }

    public Piece get(int pos){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        return board.get(pos % 8).get(pos / 8);
    }
    protected void set(int pos, Piece p){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        board.get(pos % 8).set(pos / 8, p);
    }

    public void filterLegalMoves(Set<Move> moves){
        removeSelfChecks(moves);
    }
    public void removeSelfChecks(Set<Move> moves){
        Iterator<Move> iter = moves.iterator();
        while(iter.hasNext()){ // Trial each move and see if own King is put into check
            Move m = iter.next();
            Piece oldTo = get(m.to());

            set(m.to(), m.piece());
            set(m.from(), null);
            m.piece().setPosition(m.to());

            if(m.piece().color && kingW.inCheck()) iter.remove();
            if(!m.piece().color && kingB.inCheck()) iter.remove();

            set(m.from(), m.piece());
            set(m.to(), oldTo);
            m.piece().setPosition(m.from());
        }
    }

    // Returns true if the move is a legal move on this board
    public boolean validateMove(Move m){
        if(!validPosition(m.from()) || !get(m.from()).equals(m.piece())) return false;
        return get(m.from()).getMoves().contains(m);
    }
    public boolean canMoveTo(int from, int to){
        if(!validPosition(from) || get(from) == null) return false;
        Set<Move> s = get(from).getMoves();
        for(Move m : s){
            if(m.to() == to) return true;
        }
        return false;
    }

    // Performs a legal chess move on this board
    public void makeMove(Move m){
        if(!validateMove(m)) throw new IllegalArgumentException();
        // Do something if promotion
        // Do something if castle
        set(m.to(), m.piece());
        set(m.from(), null);
        m.piece().setPosition(m.to());
    }


    public void printBoardB(){
        System.out.println("---------------------------------");

        // Print each row, start at 7 and print each position on a line.
        // Then add 8 and print the next row.
        for(int i=0;i<8;i++){
            System.out.print("| ");
            for(int j=7;j>=0;j--){
                if(get((i*8) + j) != null){
                    System.out.print(get((i*8)+j).toString() + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
    }
    public void printBoardW(){
        System.out.println("---------------------------------");

        // Print each row, start at 56 and print each position on a line.
        // Then subtract 8 and print the next row.
        for(int i=7;i>=0;i--){
            System.out.print("| ");
            for(int j=0;j<8;j++){
                if(get((i*8) + j) != null){
                    System.out.print(get((i*8)+j).toString() + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
    }
    public static int positionToInt(String pos){
        if(pos.length() > 2) return -1;
        List<Character> cb = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h');
        char c1 = pos.charAt(0); char c2 = pos.charAt(1);
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

    // Constructs a move with the "from" and "to" positions, automatically recording the
    // piece, capture and castle information. The given positions are assumed to be a legal move.
    // Not expected to handle pawn promotion.
    protected Move createMove(int from, int to){
        if(get(to) != null){
            if(get(from) != null){
                return new Move(get(from), from, to, true, false, null);
            } else { // Moving king, now check castling
                // TODO: Fix king move and check castling
            }
        } else {
            if(get(from).type.equals("King")) {
                // Check castling
            }
        }
        return new Move(get(from), from, to, false, false, null);
    }

    public Set<Move> straightProjection(int pos){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        Set<Move> m = new HashSet<Move>();
        if(get(pos) == null) return m;
        
        // Go left
        for(int i=pos-1;i>=(pos / 8) * 8;i--){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go right
        for(int i=pos+1;i<=((pos / 8)*8)+7;i++){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go up
        for(int i=pos+8;i<=63;i+=8){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go down
        for(int i=pos-8;i>=0;i-=8){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        
        return m;
    }

    public Set<Move> diagonalProjection(int pos) {
        if(!validPosition(pos)) throw new IllegalArgumentException();
        Set<Move> m = new HashSet<Move>();
        if (get(pos) == null) return m;

        // Down + left
        for(int i=pos-9;true;i-=9){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i < 0) break;
        }

        // Down + right
        for(int i=pos-7;true;i-=7){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 7 || i < 0) break;
        }

        // Up + left
        for(int i=pos+7;true;i+=7){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i > 63) break;
        }

        // Up + right
        for(int i=pos+9;true;i+=9){
            if(!validPosition(i) || (get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 7 || i > 63) break;
        }

        return m;
    }

    public void populateBoard() {
        // W
        for(int i=0;i<8;i++){
            set(8+i, new Pawn(true, 8+i, this));
        }
        set(0, new Rook(true, 1, this));
        set(1, new Knight(true, 1, this));
        set(2, new Bishop(true, 2, this));
        set(3, new Queen(true, 3, this));
        set(4, new King(true, 4, this));
        set(5, new Bishop(true, 5, this));
        set(6, new Bishop(true, 6, this));
        set(7, new Rook(true, 7, this));

        // B
        for(int i=0;i<8;i++){
            set(48+i, new Pawn(false, (48)+i, this));
        }
        set(56, new Rook(false, 56, this));
        set(57, new Knight(false, 57, this));
        set(58, new Bishop(false, 58, this));
        set(59, new Queen(false, 59, this));
        set(60, new King(false, 60, this));
        set(61, new Bishop(false, 61, this));
        set(62, new Bishop(false, 62, this));
        set(63, new Rook(false, 63, this));
    }
}
