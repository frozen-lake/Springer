import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

public class Projection {
    Set<ProjectionNode> nodes;
    Board board;

    int origin;
    boolean color; // Include the piece we stopped at?
    public Projection(int origin, boolean color, Board board){
        this.nodes = new HashSet<ProjectionNode>();
        this.board = board; this.origin = origin;
        this.color = color;
    }
    public Projection(int origin, boolean color, boolean stop, Board board){
        this.nodes = new HashSet<ProjectionNode>();
        this.board = board; this.origin = origin;
        this.color = color;
    }


    public void projectStraight(boolean attack){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Go left
        for(int i=origin-1;i/8==origin/8;i--){
            if(!board.validPosition(i)) break;
            if(attack&&board.get(i)!=null&&(board.get(i).color==color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }
        // Go right
        for(int i=origin+1;i/8==origin/8;i++){
            if(!board.validPosition(i)) break;
            if(attack&&board.get(i)!=null&&(board.get(i).color==color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }
        // Go up
        for(int i=origin+8;i<=63;i+=8){
            if(!board.validPosition(i)) break;
            if(attack&&board.get(i)!=null&&(board.get(i).color==color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }
        // Go down
        for(int i=origin-8;i>=0;i-=8){
            if(!board.validPosition(i)) break;
            if(attack&&board.get(i)!=null&&(board.get(i).color==color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }
    }
    public void projectDiagonal(boolean attack){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Down + left
        for(int i=origin-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!board.validPosition(i) || (attack && board.get(i) != null && board.get(i).color == color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack && board.get(i) != null && board.get(i).color != color) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }

        // Down + right
        for(int i=origin-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!board.validPosition(i) || (attack && board.get(i) != null && board.get(i).color == color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack && board.get(i) != null && board.get(i).color != color) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }

        // Up + left
        for(int i=origin+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!board.validPosition(i) || (attack && board.get(i) != null && board.get(i).color == color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack && board.get(i) != null && board.get(i).color != color) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }

        // Up + right
        for(int i=origin+9;i<=63;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!board.validPosition(i) || (attack && board.get(i) != null && board.get(i).color == color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(attack && board.get(i) != null && board.get(i).color != color) break;
            if(!attack&&board.get(i)!=null&&(board.get(i).color!=color)) break;
        }
    }

    public void projectKnight(boolean ignoreFriendly){
        int[] around = {origin - 6, origin + 6, origin - 10, origin + 10,
                origin - 15, origin + 15, origin - 17, origin + 17};
        Set<Move> moves = new HashSet<Move>();
        for(int j : around){
            if(board.validPosition(j) && (board.get(j) == null || !ignoreFriendly || board.get(j).color != color) && !(origin % 8 <= 1 && j % 8 >= 6 || origin % 8 >= 6 && j % 8 <= 1)){
                nodes.add(new ProjectionNode(board.get(origin), origin, j, board.get(j)));
            }
        }
    }

    public void projectPawnAttack(boolean ignoreFriendly){
        //boolean color = board.get(origin).color;
        int forward = (color ? origin + 8 : origin - 8);

        int[] j = {forward + 1, forward - 1};
        for(int i: j){
            if(board.validPosition(i) && board.get(i)!=null&&(!ignoreFriendly || board.get(i).color != color)
                    && !((i%8==7&&origin%8==0)||(i%8==0&&origin%8==7)) ){
                nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            }
        }
    }

    public void projectPawnMove(){
        //boolean color = board.get(origin).color;
        int forward = color ? origin + 8 : origin - 8;

        if(board.validPosition(forward) && (board.get(forward) == null)){

            nodes.add(new ProjectionNode(board.get(origin), origin, forward, board.get(forward)));


            int doubleForward = color ? forward + 8 : forward - 8;
            if(!board.get(origin).hasMoved && board.validPosition(doubleForward) && board.get(doubleForward)==null){
                nodes.add(new ProjectionNode(board.get(origin), origin, doubleForward, board.get(doubleForward)));
            }
        }
    }
    public void projectKing(boolean ignoreFriendly){
        if(!board.validPosition(origin) || board.get(origin) == null || !board.get(origin).type.equals("King")){
            throw new IllegalStateException("Dislocated projection is not supported for king");
        }
        int[] around = {origin - 1, origin + 1, origin - 7, origin + 7,
                origin - 8, origin + 8, origin - 9, origin + 9};
        for(int j : around){
            if((board.validPosition(j) && (board.get(j) == null || !ignoreFriendly || board.get(j).color != color))
            && !((origin%8==7&&j%8==0) || (origin%8==0&&j%8==7))
            ){
                nodes.add(new ProjectionNode(board.get(origin), origin, j, board.get(j)));
            }
        }
    }


    // Should be used after other projections to return all pieces defended
    public Set<Piece> getDefends(){
        Set<Piece> defends = new HashSet<Piece>();
        for(ProjectionNode node: nodes){
            if(node.dest != null && node.dest.color == color){
                defends.add(node.dest);
            }
        }
        return defends;
    }

    public Set<Piece> getDefenders(){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) throw new IllegalArgumentException();

        // Check relevant pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.

        Set<Piece> defenders = new HashSet<Piece>();
        clear();

        // Bishops and queens on diagonals
        // Down + left
        for(int i=origin-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Down + right
        for(int i=origin-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Up + left
        for(int i=origin+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Up + right
        for(int i=origin+9;i<=63;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        Iterator<ProjectionNode> iter =nodes .iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color == color && (node.dest.type.equals("Bishop")||node.dest.type.equals("Queen"))){
                defenders.add(node.dest);
            }
        }

        // Rooks and queens, straight horizontal and vertical
        clear();
        // Go left
        for(int i=origin-1;i/8==origin/8;i--){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go right
        for(int i=origin+1;i/8==origin/8;i++){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go up
        for(int i=origin+8;i<=63;i+=8){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go down
        for(int i=origin-8;i>=0;i-=8){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && ((!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")&&board.get(i).color!=color) || (board.get(i).color==color&&!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        iter = nodes.iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color == color && (node.dest.type.equals("Rook")||node.dest.type.equals("Queen"))){
                defenders.add(node.dest);
            }
        }


        // King
        int[] around = {origin - 1, origin + 1, origin - 7, origin + 7,
                origin - 8, origin + 8, origin - 9, origin + 9};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("King")
                    && board.get(j).color == color
                    && !((origin%8==7&&j%8==0) || (origin%8==0&&j%8==7))
            ){
                defenders.add(board.get(j));
            }
        }

        // Knights
        around = new int[]{origin - 6, origin + 6, origin - 10, origin + 10,
                origin - 15, origin + 15, origin - 17, origin + 17};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("Knight")
                    && board.get(j).color == color
                    && !(origin % 8 <= 1 && j % 8 >= 6 || origin % 8 >= 6 && j % 8 <= 1)){
                defenders.add(board.get(j));
            }
        }

        // Pawns
        int forward = !color ? origin + 8 : origin - 8;
        if(board.validPosition(forward-1) && board.get(forward-1) != null && board.get(forward-1).type.equals("Pawn") && board.get(forward-1).color == color
            && !(((forward-1)%8==7&&origin%8==0)||((forward-1)%8==0&&origin%8==7))
        ){
            defenders.add(board.get(forward-1));
        }
        if(board.validPosition(forward+1) && board.get(forward+1) != null && board.get(forward+1).type.equals("Pawn") && board.get(forward+1).color == color
            && !(((forward+1)%8==7&&origin%8==0)||((forward+1)%8==0&&origin%8==7))){
                defenders.add(board.get(forward+1));
        }

        defenders.remove(board.get(origin));

        return defenders;
    }
    public Set<Piece> getAttackers(){
        Set<Piece> attackers = new HashSet<Piece>();
        clear();
        // Check relevant pawn diagonals, bishops and queens on diagonals, rooks and queen on horizontals,
        // enemy king on all surrounding squares, and all 8 knight squares.

        // Bishops and queens on diagonals
        // Down + left
        for(int i=origin-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Down + right
        for(int i=origin-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Up + left
        for(int i=origin+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        // Up + right
        for(int i=origin+9;i<=63;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Bishop")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        Iterator<ProjectionNode> iter =nodes .iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color != color && (node.dest.type.equals("Bishop")||node.dest.type.equals("Queen"))){
                attackers.add(node.dest);
            }
        }

        // Rooks and queens, straight horizontal and vertical
        clear();
        // Go left
        for(int i=origin-1;i/8==origin/8;i--){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go right
        for(int i=origin+1;i/8==origin/8;i++){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go up
        for(int i=origin+8;i<=63;i+=8){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }
        // Go down
        for(int i=origin-8;i>=0;i-=8){
            if(!board.validPosition(i)) break;
            if(board.get(i) != null && (board.get(i).color==color || (!board.get(i).type.equals("Rook")&&!board.get(i).type.equals("Queen")))) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
        }

        iter = nodes.iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color != color && (node.dest.type.equals("Rook")||node.dest.type.equals("Queen"))){
                attackers.add(node.dest);
            }
        }


        iter = nodes.iterator();
        while(iter.hasNext()){
            ProjectionNode node = iter.next();
            if(node.dest != null && node.dest.color != color && (node.dest.type.equals("Rook")||node.dest.type.equals("Queen"))){
                attackers.add(node.dest);
            }
        } clear();


        // King
        int[] around = {origin - 1, origin + 1, origin - 7, origin + 7,
                origin - 8, origin + 8, origin - 9, origin + 9};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("King")
                    && board.get(j).color != color
                    && !((origin%8==7&&j%8==0) || (origin%8==0&&j%8==7))
            ){
                attackers.add(board.get(j));
            }
        }

        // Knights
        around = new int[]{origin - 6, origin + 6, origin - 10, origin + 10,
                origin - 15, origin + 15, origin - 17, origin + 17};
        for(int j : around){
            if(board.validPosition(j) && board.get(j) != null && board.get(j).type.equals("Knight")
                    && board.get(j).color != color
                    && !(origin % 8 <= 1 && j % 8 >= 6 || origin % 8 >= 6 && j % 8 <= 1)){
                attackers.add(board.get(j));
            }
        }

        // Pawns
        int forward = color ? origin + 8 : origin - 8;
        if(board.validPosition(forward-1) && board.get(forward-1) != null && board.get(forward-1).type.equals("Pawn")
                && board.get(forward-1).color != color
                && !(((forward-1)%8==7&&origin%8==0)||((forward-1)%8==0&&origin%8==7))
        ){
            attackers.add(board.get(forward-1));
        }
        if(board.validPosition(forward+1) && board.get(forward+1) != null && board.get(forward+1).type.equals("Pawn")
                && board.get(forward+1).color != color
                && !(((forward+1)%8==7&&origin%8==0)||((forward+1)%8==0&&origin%8==7))){
            attackers.add(board.get(forward+1));
            }

        attackers.remove(board.get(origin));

        return attackers;
    }

    // Returns set of moves from projection. Note: will not work for promotion!
    public Set<Move> moves(){
        Set<Move> moveSet = new HashSet<Move>();
        for(ProjectionNode node : nodes){
            if(board.isPromotion(node.from, node.to)){
                for(String p : Pawn.promotions){
                    moveSet.add(board.createMove(node.from, node.to, p));
                }
            } else {
                moveSet.add(board.createMove(node.from, node.to));
            }
        }

        return moveSet;
    }
    public void clear(){
        this.nodes = new HashSet<ProjectionNode>();
    }

    public Iterator<ProjectionNode> iterator(){
        return nodes.iterator();
    }
}