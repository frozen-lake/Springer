import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.NoSuchElementException;

import static org.junit.Assert.*;

public class testCastling {
    @Test
    public void testCanCastleShort1(){
        // Determines if canCastleShort is true correctly for both white and black king
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\ne7 e5\ng1 f3\nb8 c6\nf1 c4\ng8 f6\na2 a4\nf8 c5\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertTrue(b.kingW.canCastleShort());
            assertTrue(b.kingB.canCastleShort());
        }

        System.setIn(System.in);
    }
    @Test
    public void testCanCastleShort2(){
        // Determines if canCastleShort is false correctly for both white and black king
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\nd7 d6\ng1 f3\nc8 e6\nf1 c4\ne6 c4\na2 a3\ng7 g6\na3 a4\nf8 h6\na4 a5\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertTrue(!b.kingW.canCastleShort());
            assertTrue(!b.kingB.canCastleShort());
        }

        System.setIn(System.in);
    }
    @Test
    public void testCastleShort(){
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("e2 e4\ne7 e5\ng1 f3\nb8 c6\nf1 c4\ng8 f6\nO-O\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertNotNull(b.get(6));
            assertNotNull(b.get(5));
            assertEquals("King", b.get(6).type);
            assertEquals("Rook", b.get(5).type);
        }

        System.setIn(System.in);
    }

    @Test
    public void testCanCastleLong1(){

        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("d2 d4\nd7 d5\nc1 f4\nc8 f5\nb1 c3\nb8 c6\nd1 d2\nd8 d7\n".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();

        } catch(NoSuchElementException ignored){
            assertTrue(b.kingW.canCastleLong());
            assertTrue(b.kingB.canCastleLong());
        }

        System.setIn(System.in);
    }
    public void testCanCastleLong2(){

    }
    @Test
    public void testCastleLong(){
        InputStream sysin = System.in;
        ByteArrayInputStream in = new ByteArrayInputStream("d2 d4\nd7 d5\nc1 f4\nc8 f5\nb1 c3\nb8 c6\nd1 d2\nd8 d7\nO-O-O\nO-O-O".getBytes());
        System.setIn(in);

        Chess c = new Chess();
        Board b = c.board;
        try{
            c.startGameW();
        } catch(NoSuchElementException e){
            assertNotNull(b.get(2));
            assertNotNull(b.get(3));
            assertNotNull(b.get(58));
            assertNotNull(b.get(59));
            assertNull(b.get(0));
            assertNull(b.get(4));
            assertNull(b.get(56));
            assertNull(b.get(60));
        }

        System.setIn(System.in);
    }
}
