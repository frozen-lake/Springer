
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.Console;
import java.io.InputStream;
import java.util.NoSuchElementException;

public class testChess {

    @Test
    public void testGame1(){
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("d2 d3\ne7 e5\nb1 c3\nf8 b4\nc3 e4\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertNotNull(b.get(18));
            assertEquals("Knight", b.get(18).type);
        }

        System.setIn(System.in);
    }

    @Test
    public void testPromotion1(){

        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\nf7 f5\ne4 f5\ng7 g6\nf5 g6\na7 a5\ng6 h7\na5 a4\nh7 g8\nQ\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertNotNull(b.get(62));
            assertEquals("Queen", b.get(62).type);
        }

        System.setIn(System.in);
    }

    @Test
    public void testPromotion2(){

        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\nf7 f5\na2 a4\nf5 e4\nd2 d3\ne4 d3\na4 a5\nd3 c2\na5 a6\nc2 d1\nQ\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertNotNull(b.get(3));
            assertEquals("Queen", b.get(3).type);
            assertEquals(false, b.get(3).color);
        }

        System.setIn(System.in);
    }

    @Test
    public void testCheckmate(){
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\ne7 e5\nf1 c4\nb8 c6\nd1 f3\na7 a5\nf3 f7\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            System.out.println(((King) b.get(60)).inCheck() + " | " + ((King) b.get(60)).getMoves().size());
            System.out.println(((King) b.get(60)).getMoves());
            assertNotNull(b.winner);
            assertEquals(true, b.winner);
        }
        System.setIn(System.in);
    }
}
