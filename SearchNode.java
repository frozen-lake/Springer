import java.util.List;
import java.util.ArrayList;

public class SearchNode implements Comparable<SearchNode> {
    public int score; // Search score
    public int alpha;
    public int beta;
    public int eval; // Concrete evaluation after move
    public SearchNode next; // Next in series of best moves
    public ArrayList<SearchNode> children; // All nodes explored at the next depth level
    public SearchNode parent;
    public Move move;
    public String type;

    public SearchNode(int score){
        type = "N";
        parent = null;
        children = new ArrayList<SearchNode>();
        alpha=Integer.MIN_VALUE;beta=Integer.MAX_VALUE;
        this.score = score;
        this.next = null;
        this.eval = score;
    }
    public SearchNode(int score, Move m, int alpha, int beta){
        type = "N";
        parent = null;
        children = new ArrayList<SearchNode>();
        this.score = score;
        this.alpha = alpha; this.beta = beta;
        this.next = null;
        this.eval = score;
    }
    public SearchNode(Move move){
        type = "N";
        parent = null;
        children = new ArrayList<SearchNode>();
        this.score = 0;
        alpha=Integer.MIN_VALUE;beta=Integer.MAX_VALUE;
        this.next = null;
        this.move = move;
        this.eval = 0;
    }
    public SearchNode(int score, Move move){
        type = "N";
        parent = null;
        children = new ArrayList<SearchNode>();
        alpha=Integer.MIN_VALUE;beta=Integer.MAX_VALUE;
        this.score = score;
        this.next = null;
        this.move = move;
        this.eval = score; // Base case node; end of recursive depth
    }
    public SearchNode(Move move, SearchNode parent){
        type = "N";
        this.parent = parent;
        children = new ArrayList<SearchNode>();
        parent.addChild(this);
        this.score = 0;
        alpha=parent.alpha;beta=parent.beta;
        this.next = null;
        this.move = move;
        this.eval = 0;
    }

    // Leaf constructor
    public SearchNode(int score, Move move, SearchNode parent){
        type = "N";
        this.parent = parent;
        children = new ArrayList<SearchNode>();
        parent.addChild(this);
        this.score = score;
        alpha=parent.alpha;beta=parent.beta;
        this.next = null;
        this.move = move;
        this.eval = score;
    }

    public void addChild(SearchNode child){
        children.add(child);
    }
    public void printChildren(int n){
        for(SearchNode child: children){
            for(int i=0;i<n;i++){System.out.print(" ");};
            System.out.println(child.toString());
            child.printChildren(n+1);
        }
    }

    public String toString(){
        return type+"[" + score + "/"+eval+", " + alpha + "/" + beta + ", " + move + "], "+"{"+next+"}";
    }

    public int compareTo(SearchNode other){
        return score - other.score;
    }

    public void printDebugUp(){
        System.out.println("=== DEBUG UPTREE ===");
        SearchNode p = this;
        while(p != null){
            System.out.println(type + "[" + p.score + "/"+p.eval+", " + p.move + "], ");
            p = p.parent;
        }
    }
    public void printDebugDown(){
        System.out.println("=== DEBUG DOWNTREE ===");
        SearchNode p = this;
        while(p != null){
            System.out.println(type + "[" + p.score + "/"+p.eval+", " + p.move + "], ");
            p = p.next;
        }
    }
}
