import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;

public class Projection {
    Set<ProjectionNode> nodes;
    Board board;

    int origin;
    boolean stop; // Stop projection once we hit a piece?
    boolean color; // Include the piece we stopped at?
    public Projection(int origin, boolean color, Board board){
        this.nodes = new HashSet<ProjectionNode>();
        this.board = board; this.origin = origin;
        this.color = color;
        stop = true;
    }
    public Projection(int origin, boolean color, boolean stop, Board board){
        this.nodes = new HashSet<ProjectionNode>();
        this.board = board; this.origin = origin; this.stop = stop;
        this.color = color;
    }


    public void projectStraight(boolean ignoreFriendly){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Go left
        for(int i=origin-1;i/8==origin/8;i--){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }
        // Go right
        for(int i=origin+1;i/8==origin/8;i++){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }
        // Go up
        for(int i=origin+8;i<=63;i+=8){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }
        // Go down
        for(int i=origin-8;i>=0;i-=8){
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }
    }
    public void projectDiagonal(boolean ignoreFriendly){
        if(!board.validPosition(origin)) throw new IllegalArgumentException();
        if (board.get(origin) == null) return;

        // Down + left
        for(int i=origin-9;i>=0;i-=9){
            if(i%8 == 7 || (i%8)+1!=((i+9)%8)) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }

        // Down + right
        for(int i=origin-7;i>=0;i-=7){
            if(i%8 == 0 || (i%8)-1!=((i+7) %8)) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }

        // Up + left
        for(int i=origin+7;i<=63;i+=7){
            if(i%8==7 || (i%8)+1!=(i-7)%8) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }

        // Up + right
        for(int i=origin+9;i<=63;i+=9){
            if(i%8==0 || (i%8)-1!=(i-9)%8) break;
            if(!board.validPosition(i) || (ignoreFriendly && board.get(i) != null && board.get(i).color == board.get(origin).color)) break;
            nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            if(stop&&board.get(i) != null) break;
        }
    }

    public void projectKnight(boolean ignoreFriendly){
        int[] around = {origin - 6, origin + 6, origin - 10, origin + 10,
                origin - 15, origin + 15, origin - 17, origin + 17};
        Set<Move> moves = new HashSet<Move>();
        for(int j : around){
            if(board.validPosition(j) && (board.get(j) == null || !ignoreFriendly || board.get(j).color != board.get(origin).color) && !(origin % 8 <= 1 && j % 8 >= 6 || origin % 8 >= 6 && j % 8 <= 1)){
                nodes.add(new ProjectionNode(board.get(origin), origin, j, board.get(j)));
            }
        }
    }

    public void projectPawnAttack(boolean ignoreFriendly){
        boolean color = board.get(origin).color;
        int forward = color ? origin + 8 : origin - 8;

        int[] j = {forward + 1, forward - 1};
        for(int i: j){
            if(board.validPosition(i) && (board.get(i) == null || !ignoreFriendly || board.get(i).color != board.get(origin).color) && !((origin%8<=1&&i%8>=6) || (origin%8>=6&&i%8<=1)) ){
                nodes.add(new ProjectionNode(board.get(origin), origin, i, board.get(i)));
            }
        }
    }

    public void projectPawnMove(){
        boolean color = board.get(origin).color;
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
            if((board.validPosition(j) && (board.get(j) == null || !ignoreFriendly || board.get(j).color != board.get(origin).color))
            && !((origin%8==7&&j%8==0) || (origin%8==0&&j%8==7))
            ){
                nodes.add(new ProjectionNode(board.get(origin), origin, j, board.get(j)));
            }
        }
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