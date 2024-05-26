import java.util.*;

public class Board {
    public static final boolean debugBoard = true;
    /*
    A board object represents a 8x8 chess board of Piece objects using a 2D ArrayList.
    Columns by rows.

     */
    protected ArrayList<ArrayList<Piece>> board;
    protected King kingW;
    protected King kingB;

    protected ArrayList<Move> moves;
    protected Set<Piece> pieces;
    protected Set<Piece> whiteArmy;
    protected Set<Piece> blackArmy;
    protected boolean sideToMove;
    protected Boolean winner;

    public Board(){
        board = new ArrayList<ArrayList<Piece>>(8);
        for(int i=0;i<8;i++){
            board.add(new ArrayList<Piece>(8));
            for(int j=0;j<8;j++){board.get(i).add(null);}
        }
        initializeBoard();
    }

    // Copies the board
    public Board(Board b){
        board = new ArrayList<ArrayList<Piece>>(8);
        for(int i=0;i<8;i++){
            board.add(new ArrayList<Piece>(8));
            for(int j=0;j<8;j++){board.get(i).add(null);}
        }

        initializeBoard();

        ArrayList<Move> ms = new ArrayList<Move>();
        for(int i=0;i<b.moves.size();i++){
            // Piece at corresponding original position
            Move m = b.moves.get(i);
            Piece p = get(m.piece().originalPosition);
            Piece capture = m.capture() == null ? null : get(m.capture().originalPosition);

            ms.add(new Move(p, m.from(), m.to(), capture, m.castle(), m.promotion()));
        }
        for(int i=0;i<ms.size();i++){
            makeMove(ms.get(i));
        }

    }

    private void initializeBoard(){
        pieces = new HashSet<Piece>(); whiteArmy = new HashSet<Piece>(); blackArmy = new HashSet<Piece>();
        moves = new ArrayList<Move>();
        sideToMove = true;
        winner = null;

        populateBoard();
        kingW = (King) get(4);
        kingB = (King) get(60);
    }

    public Set<Move> getWhiteMoves(){
        Set<Move> s = new HashSet<Move>();
        for(Piece p: whiteArmy){
            s.addAll(p.getMoves());
        }
        return s;
    }

    public Set<Move> getBlackMoves(){
        Set<Move> s = new HashSet<Move>();
        for(Piece p: blackArmy){
            s.addAll(p.getMoves());
        }
        return s;
    }

    public void addPiece(Piece p){
        set(p.getPosition(), p);
        pieces.add(p);
        if(p.color){whiteArmy.add(p);}else{blackArmy.add(p);}
//        if(p.type.equals("King")){
//            if(p.color){kingW = (King) p;}
//            else {kingB = (King) p;}
//        }
    }
    public Set<Move> allMoves(){
        Set<Move> s = new HashSet<Move>();
        for(Piece p: pieces){
            s.addAll(p.getMoves());
        }
        return s;
    }

    // Returns the piece at location pos, or null if there is no piece.
    public Piece get(int pos){
        if(!validPosition(pos)) {
            if(debugBoard){
                System.out.println("===get(int pos) DEBUG===\n");
                printBoardW();
            }
            throw new IllegalArgumentException();
        }
        return board.get(pos % 8).get(pos / 8);
    }

    // Set the square at location pos to piece p
    protected void set(int pos, Piece p){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        board.get(pos % 8).set(pos / 8, p);
    }

    // Filters a set of moves by removing self checks.
    public void filterLegalMoves(Set<Move> moves){
        removeSelfChecks(moves);
        Iterator<Move> iter = moves.iterator();
        while(iter.hasNext()){
            Move move = iter.next();
            if(move.capture() != null && move.capture().color == move.piece().color){
                iter.remove();
            }
        }
    }

    // Removes all moves from the given set which place the king, of the same color as
    // the piece being moved, in check.
    public void removeSelfChecks(Set<Move> moves){
        Iterator<Move> iter = moves.iterator();
        while(iter.hasNext()){ // Trial each move and see if own King is put into check
            Move m = iter.next();
            Piece oldTo = get(m.to());

            primitiveMove(m);
            if(m.piece().color && kingW.inCheck()) iter.remove();
            if(!m.piece().color && kingB.inCheck()) iter.remove();
            undoPrimitiveMove(m);
        }
    }

    // Returns true if the passed move is within the relevant piece's dataset.
    public boolean validateMove(Move m){
        if(m.piece() == null || m.piece().position != m.from()
                || !validPosition(m.from()) || !validPosition(m.to())
        ){
            return false;
        }
        if(m.promotion() != null && !isPromotion(m.from(), m.to())) return false;

        return m.piece().getMoves().contains(m);
    }

    // Checks if the piece on the from square can move to the to square by
    // generating and checking the piece's legal moves.
    // If no piece exists on the from square, returns false.
    public boolean canMoveTo(int from, int to){
        if(!validPosition(from) || get(from) == null) return false;
        Set<Move> s = get(from).getMoves();
        for(Move m : s){
            if(m.to() == to) return true;
        }
        return false;
    }

    // Executes the primitive move operations without of the decoration from makeMove.
    // Does not validate move but DOES update the piece's position property.
    protected void primitiveMove(Move m){
        if(m.castle() != null){
            if(m.castle().equals("K")){
                ((King) m.piece()).castleShort();
                if(m.isFirstMove()) m.piece().hasMoved = true;
                get(m.to() - 1).hasMoved = true;
                return;
            } else if(m.castle().equals("Q")){
                ((King) m.piece()).castleLong();
                if(m.isFirstMove()) m.piece().hasMoved = true;
                get(m.to() + 1).hasMoved = true;
                return;
            }
        } else {
            set(m.to(), m.piece());
            set(m.from(), null);
            m.piece().setPosition(m.to());
            if(m.isFirstMove()) m.piece().hasMoved = true;
        }
    }
    protected void undoPrimitiveMove(Move m){
        if(get(m.to()) != m.piece()) throw new IllegalArgumentException();
        set(m.to(), m.capture());
        set(m.from(), m.piece());
        m.piece().setPosition(m.from());
        if(m.capture() != null) m.capture().setPosition(m.to());
        if(m.isFirstMove()) m.piece().hasMoved = false;

        // Rook
        if(m.castle()!=null) {

            if (m.castle().equals("K")) {
                set(m.to() + 1, get(m.to() - 1));
                set(m.to() - 1, null);
                get(m.to() + 1).setPosition(m.to() + 1);
                get(m.to() + 1).hasMoved = false;
            } else if (m.castle().equals("Q")){
                set(m.to() - 2, get(m.to() + 1));
                set(m.to() + 1, null);
                get(m.to() - 2).setPosition(m.to() - 2);
                get(m.to() - 2).hasMoved = false;
            }
        }
    }
    // Records move, checks for end of game, and passes turn.
    public void endTurn(Move m){
        moves.add(m);
        checkCheckmate(!m.piece().color?kingW:kingB);
        sideToMove = !sideToMove;
    }

    // Performs a legal chess move on this board. Validates,
    // Executes castling maneuvers and promotion, and updates hasMoved property.
    public void makeMove(Move m){
        if(!validateMove(m)) {
            if (debugBoard){
                printBoardW(); // Move m not a member of m.piece().getMoves()
                System.out.println("makeMove() call debug: Move being tried: " + m);
                System.out.println(get(4).hasMoved + " | " + get(7).hasMoved);
                System.out.println(((King) get(4)).canCastleShort() + " | " + get(7));
                System.out.println("Possible moves for " + m.piece() + ": " + m.piece().getMoves());
                System.out.println("Moves: " + moves);
            }
            throw new IllegalArgumentException();
        }

        primitiveMove(m);

        if(m.capture() != null) m.capture().captured();

        if(m.promotion() != null){
            ((Pawn) m.piece()).promote(m.promotion());
        }

        endTurn(m);
    }

    // WIP Urgent: castling and promotion
    public void undoMove(Move m){

        if(kingW.inCheckmate()) winner = null;
        if(kingB.inCheckmate()) winner = null;

        undoPrimitiveMove(m);



        if(m.capture() != null) {
            addPiece(m.capture());
        }

        //if((m.from() >= (m.piece().color?8:48) && m.from() <= (m.piece().color?15:55) && m.piece().type.equals("Pawn"))) m.piece().hasMoved = false;
        //if(m.piece().type.equals("King") && m.from() == (m.piece().color?4:60)) m.piece().hasMoved = false;
        //if(m.piece().type.equals("Rook") && (m.from() == (m.piece().color?0:56)) || (m.from() == (m.piece().color?7:63))) m.piece().hasMoved = false;


        set(m.from(), m.piece()); // undo promote


        moves.removeLast();
        sideToMove = !sideToMove;
    }

    // Tests checkmate on the given king, ending the game here if so.
    public void checkCheckmate(King k){
        if(k.inCheckmate()){
            winner = k.color;
//            if(winner){printBoardW();}else{printBoardB();}
//            System.out.println("Game over! " + (!k.color?"White":"Black") + " won by checkmate.");
        } else {

        }
    }

    public void printBoardB(){
        System.out.println("    -------------------------------");

        // Print each row, start at 7 and print each position on a line.
        // Then add 8 and print the next row.
        for(int i=0;i<8;i++){
            System.out.print(i+1 + "  | ");
            for(int j=7;j>=0;j--){
                if(get((i*8) + j) != null){
                    System.out.print(get((i*8)+j).toString() + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
        System.out.println("     h   g   f   e   d   c   b   a");
    }
    public void printBoardW(){
        System.out.println("    -------------------------------");

        // Print each row, start at 56 and print each position on a line.
        // Then subtract 8 and print the next row.
        for(int i=7;i>=0;i--){
            System.out.print(i+1 + "  | ");
            for(int j=0;j<8;j++){
                if(get((i*8) + j) != null){
                    System.out.print(get((i*8)+j).toString() + " | ");
                } else {
                    System.out.print("  | ");
                }
            }
            System.out.println();
        }
        System.out.println("     a   b   c   d   e   f   g   h");
    }
    public static int positionToInt(String pos){
        if(pos.length() > 2) return -1;
        List<Character> cb1 = Arrays.asList('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h');
        List<Character> cb2 = Arrays.asList('A', 'B', 'C', 'D', 'E', 'F', 'G', 'H');
        char c1 = pos.charAt(0); char c2 = pos.charAt(1);

        if(!Character.isDigit(c2)) return -1;
        if(cb1.contains(c1)) return cb1.indexOf(c1) + (8 * (Character.getNumericValue(c2) - 1));
        if(cb2.contains(c1)) return cb2.indexOf(c1) + (8 * (Character.getNumericValue(c2) - 1));
        return -1;
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
    public boolean isPromotion(int from, int to){
        if(!validPosition(from) || !validPosition(to)) return false;
        if(get(from) == null || !get(from).type.equals("Pawn")) return false;
        if(get(from).color && to >= 56 && to <= 63) return true;
        if(!get(from).color && to >= 0 && to <= 7) return true;
        return false;
    }
    // Constructs a move with the "from" and "to" positions, automatically recording the
    // piece, capture and castle information. The given positions are assumed to be a legal move.
    // Not expected to handle pawn promotion.
    protected Move createMove(int from, int to){
        if(get(from) == null) {
            if (debugBoard) {
                printBoardW();
                System.out.println(from + " | " + to);
            }
            throw new IllegalArgumentException();
        }
        if(get(to) != null){
            return new Move(get(from), from, to, get(to), null, null);
        } else {
            if(get(from).type.equals("King") && to - from == 2) {
                return new Move(get(from), from, to, null, "K", null);
            } else if(get(from).type.equals("King") && from - to == 2) {
                return new Move(get(from), from, to, null, "Q", null);
            }
        }
        return new Move(get(from), from, to, null, null, null);
    }
    protected Move createMove(int from, int to, String promotion){
        if(get(to) != null){
            return new Move(get(from), from, to, get(to), null, promotion);
        }
        return new Move(get(from), from, to, null, null, promotion);
    }

    public Set<Move> straightProjection(int pos, boolean ignoreFriendly){
        if(!validPosition(pos)) throw new IllegalArgumentException();
        Set<Move> m = new HashSet<Move>();
        if(get(pos) == null) return m;
        
        // Go left
        for(int i=pos-1;i>=(pos / 8) * 8;i--){
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go right
        for(int i=pos+1;i<=((pos / 8)*8)+7;i++){
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go up
        for(int i=pos+8;i<=63;i+=8){
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go down
        for(int i=pos-8;i>=0;i-=8){
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        
        return m;
    }

    public Set<Move> diagonalProjection(int pos, boolean ignoreFriendly) {
        if(!validPosition(pos)) throw new IllegalArgumentException();
        Set<Move> m = new HashSet<Move>();
        if (get(pos) == null) return m;

        // Down + left
        for(int i=pos-9;true;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i < 0) break;
        }

        // Down + right
        for(int i=pos-7;true;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 7 || i < 0) break;
        }

        // Up + left
        for(int i=pos+7;true;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i > 63) break;
        }

        // Up + right
        for(int i=pos+9;true;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
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
        set(0, new Rook(true, 0, this));
        set(1, new Knight(true, 1, this));
        set(2, new Bishop(true, 2, this));
        set(3, new Queen(true, 3, this));
        set(4, new King(true, 4, this));
        set(5, new Bishop(true, 5, this));
        set(6, new Knight(true, 6, this));
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
        set(62, new Knight(false, 62, this));
        set(63, new Rook(false, 63, this));

        ((Rook) get(63)).otherRook = (Rook) get(56);
        ((Rook) get(56)).otherRook = (Rook) get(63);
        ((Rook) get(0)).otherRook = (Rook) get(7);
        ((Rook) get(7)).otherRook = (Rook) get(0);
    }

    public Board copy(){
        return new Board(this);
    }

    public boolean equals(Object o){
        if(!(o instanceof Board)) return false;
        Board b = (Board) o;
        for(int i=0;i<=63;i++){
            if(get(i) == null && b.get(i) == null) continue;
            if(get(i) != null && !get(i).equals(b.get(i))) return false;
            if(!b.get(i).equals(get(i))) return false;
        }
        return true;
    }
}
