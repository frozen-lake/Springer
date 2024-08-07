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
    protected Set<Piece> whitePieces;
    protected Set<Piece> blackPieces;
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
            if(m.piece() == null) {
                ms.add(generateNullMove());
                continue;
            }
            Piece p = get(m.piece().originalPosition);
            Piece capture = m.capture() == null ? null : get(m.capture().originalPosition);

            ms.add(new Move(p, m.from(), m.to(), capture, m.castle(), m.promotion()));
        }

        for(int i=0;i<ms.size();i++){
            makeMove(ms.get(i));
        }

    }

    private void initializeBoard(){
        pieces = new HashSet<Piece>(); whitePieces = new HashSet<Piece>(); blackPieces = new HashSet<Piece>();
        moves = new ArrayList<Move>();
        sideToMove = true;
        winner = null;

        populateBoard();
        kingW = (King) get(4);
        kingB = (King) get(60);
        updateProperties();
    }

    public void updatePieces(){
        Set<Piece> set = new HashSet<Piece>();
        Piece p;
        for(int i=0;i<=63;i++){
            p = get(i);
            if(p != null){
                set.add(p);
                p.setPosition(i);
            }
        }
        pieces = set;
    }
    public Set<Piece> pieces(){
        return pieces;
    }

    public void updateWhitePieces(){
        Set<Piece> set = new HashSet<Piece>();
        Piece p;
        for(int i=0;i<=63;i++){
            p = get(i);
            if(p != null && p.color) set.add(p);
        }
        whitePieces = set;
    }
    public Set<Piece> whitePieces(){
        return whitePieces;
    }
    public void updateBlackPieces(){
        Set<Piece> set = new HashSet<Piece>();
        Piece p;
        for(int i=0;i<=63;i++){
            p = get(i);
            if(p != null && !p.color) set.add(p);
        }
        blackPieces =  set;
    }
    public Set<Piece> blackPieces(){
        return blackPieces;
    }
    public Set<Move> getWhiteMoves(){
        Set<Move> s = new HashSet<Move>();
        for(Piece p: whitePieces()){
            s.addAll(p.getMoves());
        }
        return s;
    }

    public Set<Move> getBlackMoves(){
        Set<Move> s = new HashSet<Move>();
        for(Piece p: blackPieces()){
            s.addAll(p.getMoves());
        }
        return s;
    }

    public void addPiece(Piece p){
        set(p.getPosition(), p);
    }

    public Move generateNullMove(){
        Move m = new Move(null);
        return m;
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
            if(move.capture() != null && (move.capture().color == move.piece().color || move.capture().type.equals("King"))){
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

            primitiveMove(m, false);
            if(m.piece().color && kingW.inCheck()) iter.remove();
            if(!m.piece().color && kingB.inCheck()) iter.remove();
            undoPrimitiveMove(m, false);
        }
    }

    // Returns true if the passed move is within the relevant piece's dataset.
    public boolean validateMove(Move m){
        if(m.piece() == null) return true;
        if(m.piece().position != m.from()
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

    // Tests checkmate on the given king, ending the game here if so.
    public void checkCheckmate(King k){
        if(k.inCheckmate()){
            winner = !k.color;
        }
    }

    // Performs a legal chess move on this board after validation. Also checks for checkmate.
    public void makeMove(Move m){
        updateProperties();
        if(!validateMove(m)) {
            if (debugBoard){
                printBoardW(); // Move m not a member of m.piece().getMoves()
                System.out.println("makeMove() call debug: Move being tried: " + m);
                System.out.println(get(53));
                System.out.println("Moves: " + moves);
            }
            throw new IllegalArgumentException();
        }

        primitiveMove(m, true);

        checkCheckmate(sideToMove?kingW:kingB);
    }

    public void undoMove(Move m){
        undoPrimitiveMove(m, true);
    }


    // Executes the primitive move operations without of the decoration from makeMove.
    // Does not validate move but DOES update the piece's position property.
    protected void primitiveMove(Move m, boolean update){
        moves.add(m);
        sideToMove = !sideToMove;
        if(m.piece() != null) {
            if (m.castle() != null) {
                if (m.castle().equals("K")) {
                    ((King) m.piece()).castleShort();
                    if (m.isFirstMove()) m.piece().hasMoved = true;
                    get(m.to() - 1).hasMoved = true;
                } else if (m.castle().equals("Q")) {
                    ((King) m.piece()).castleLong();
                    if (m.isFirstMove()) m.piece().hasMoved = true;
                    get(m.to() + 1).hasMoved = true;
                }
            } else {
                set(m.to(), m.piece());
                set(m.from(), null);
                m.piece().setPosition(m.to());
                if (m.capture() != null) m.capture().captured();
                if (m.isFirstMove()) m.piece().hasMoved = true;

                if (m.promotion() != null) {
                    ((Pawn) m.piece()).promote(m.promotion());
                }
            }
            if (m.piece().position != m.to()) {
                throw new IllegalStateException();
            }
        }
        if(update) updateProperties();
    }
    protected void undoPrimitiveMove(Move m, boolean update){

        if(m.piece() != null && m.promotion() == null && get(m.to()) != m.piece()) {
            if(debugBoard){
                printBoardW();
                System.out.println("===" + m + " | " + m.from() + ", piece: " + m.piece().toStringDebug());
            }
            throw new IllegalArgumentException();
        }
        if(winner != null) winner = null;
        sideToMove = !sideToMove;
        moves.removeLast();

        if(m.piece()!=null) { // not null move
            set(m.to(), m.capture());
            set(m.from(), m.piece());
            m.piece().setPosition(m.from());
            if (m.capture() != null) {
                addPiece(m.capture());
                m.capture().captured = false;
            }
            if (m.isFirstMove()) m.piece().hasMoved = false;
            winner = null;

            // Rook
            if (m.castle() != null) {
                ((King) m.piece()).castled = null;

                if (m.castle().equals("K")) {
                    set(m.to() + 1, get(m.to() - 1));
                    set(m.to() - 1, null);
                    get(m.to() + 1).setPosition(m.to() + 1);
                    get(m.to() + 1).hasMoved = false;
                } else if (m.castle().equals("Q")) {
                    set(m.to() - 2, get(m.to() + 1));
                    set(m.to() + 1, null);
                    get(m.to() - 2).setPosition(m.to() - 2);
                    get(m.to() - 2).hasMoved = false;
                }
            }
        }
        if(update) updateProperties();
    }
    // Updates board and piece properties, typically called after a move has been played.
    public void updateProperties(){
        updatePieces();
        updateWhitePieces();
        updateBlackPieces();

        // Generate new set of all pieces, white pieces, black pieces.
        // Tell every piece to update its local properties.
        for(Piece p: pieces){
            if(p.board != this) throw new IllegalStateException();
            p.updateMoves();
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
        if(get(from) == null || isPromotion(from, to)) {
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
        for(int i=pos-1;i/8==pos/8;i--){
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
        }
        // Go right
        for(int i=pos+1;i/8==pos/8;i++){
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
        for(int i=pos-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i < 0) break;
        }

        // Down + right
        for(int i=pos-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 7 || i < 0) break;
        }

        // Up + left
        for(int i=pos+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!validPosition(i) || (ignoreFriendly && get(i) != null && get(i).color == get(pos).color)) break;
            m.add(createMove(pos, i));
            if(get(i) != null) break;
            if(i % 8 == 0 || i > 63) break;
        }

        // Up + right
        for(int i=pos+9;i<=63;i+=9){
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
//    public int hashCode(){
//        return 0;
//    }
}
