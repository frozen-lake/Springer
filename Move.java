public record Move(Piece piece, int from, int to, Piece capture, String castle, String promotion){

    public boolean equals(Object o){
        if(!(o instanceof Move)) return false;
        Move m = (Move) o;
        return m.piece().equals(piece)
                && m.from() == from && m.to() == to && m.capture() == capture && m.castle() == castle
                && ((promotion==null&&m.promotion()==null) || (promotion != null && promotion.equals(m.promotion())));
    }
    public String toString(){
        return piece.toString() + " | " + from + " -> " + to;
    }
    public int hashCode(){
        return piece.hashCode() + (31*from + 32*to) + (promotion!=null?promotion.hashCode():-1)
                + (castle!=null?castle.hashCode():-1) +(capture!=null?capture.hashCode():-1);
    }
}
