public class Move{
    private Piece piece;
    private int from;
    private int to;
    private Piece capture;
    private String castle;
    private String promotion;
    private boolean firstMove;
    public Move(Piece piece, int from, int to, Piece capture, String castle, String promotion){
        this.piece = piece; this.capture = capture;
        this.from = from; this.to = to;
        this.castle = castle; this.promotion = promotion;
        this.firstMove = !piece.hasMoved;
    }
    public Move(Piece piece, int from, int to, Piece capture){
        this.piece = piece; this.capture = capture;
        this.from = from; this.to = to;
        this.firstMove = !piece.hasMoved;

    }
    public Move(Piece piece, int from, int to){
        this.piece = piece; this.capture = null;
        this.from = from; this.to = to;
        this.firstMove = !piece.hasMoved;
    }
    public boolean equals(Object o){
        if(!(o instanceof Move)) return false;
        Move m = (Move) o;
        return m.piece().equals(piece)
                && m.from() == from() && m.to() == to()
                && ((m.capture()!=null && m.capture().equals(capture())) || (capture()==null&&m.capture()==null))
                && ((m.castle() != null && m.castle().equals(castle())) || (m.castle()==null&&castle()==null))
                && ((promotion==null&&m.promotion()==null) || (promotion != null && promotion.equals(m.promotion())));
    }
    public String toString(){
        return piece.toString() + (capture!=null?"x"+capture:"") + (promotion!=null?"->"+promotion:"") + " | " + from + " -> " + to;
    }
    public int hashCode(){
        return piece.hashCode() + (31*from + 32*to) + (promotion!=null?promotion.hashCode():-1)
                + (castle!=null?castle.hashCode():-1) +(capture!=null?capture.hashCode():-1);
    }
    public Piece piece(){return piece;}
    public int from(){return from;}
    public int to(){return to;}
    public Piece capture(){return capture;}
    public String castle(){return castle;}
    public String promotion(){return promotion;}
    public boolean isFirstMove(){return firstMove;}
    protected void setFirstMove(boolean b){firstMove = b;}
}
