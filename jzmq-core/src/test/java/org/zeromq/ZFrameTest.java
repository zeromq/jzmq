package org.zeromq;

import org.junit.Test;
import org.zeromq.ZMQ.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Tests ZFrame class
 * 
 * @author Richard Smith
 * 
 */
public class ZFrameTest {

    @Test
    public void testZFrameCreation() {
        ZFrame f = new ZFrame("Hello".getBytes());
        assertTrue(f != null);
        assertTrue(f.hasData());
        assertEquals(5, f.size());

        f = new ZFrame();
        assertFalse(f.hasData());
        assertEquals(0, f.size());
    }

    @Test
    public void testZFrameEquals() {
        ZFrame f = new ZFrame("Hello".getBytes());
        ZFrame clone = f.duplicate();
        assertEquals(f, clone);
    }

    @Test
    public void testSending() {
        ZContext ctx = new ZContext();
        Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zframe.test");
        Socket input = ctx.createSocket(ZMQ.PAIR);
        input.connect("inproc://zframe.test");

        // Send five different frames, test ZFRAME_MORE
        for (int i = 0; i < 5; i++) {
            ZFrame f = new ZFrame("Hello".getBytes());
            boolean rt = f.send(output, ZMQ.SNDMORE);
            assertTrue(rt);
        }

        // Send same frame five times
        ZFrame f = new ZFrame("Hello".getBytes());
        for (int i = 0; i < 5; i++) {
            f.send(output, ZMQ.SNDMORE);
        }
        assertEquals(5, f.size());
        ctx.close();
    }

    @Test
    public void testCopyingAndDuplicating() {
        ZContext ctx = new ZContext();
        Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zframe.test");
        Socket input = ctx.createSocket(ZMQ.PAIR);
        input.connect("inproc://zframe.test");

        ZFrame f = new ZFrame("Hello");
        ZFrame copy = f.duplicate();
        assertTrue(copy.equals(f));
        f.destroy();
        assertFalse(copy.equals(f));
        assertEquals(5, copy.size());
        ctx.close();
    }

    @Test
    public void testReceiving() {
        ZContext ctx = new ZContext();
        Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zframe.test");
        Socket input = ctx.createSocket(ZMQ.PAIR);
        input.connect("inproc://zframe.test");

        // Send same frame five times
        ZFrame f = new ZFrame("Hello".getBytes());
        for (int i = 0; i < 5; i++) {
            f.send(output, ZMQ.SNDMORE);
        }

        // Send END frame
        f = new ZFrame("NOT".getBytes());
        f.reset("END".getBytes());
        assertEquals("454E44", f.strhex());
        f.send(output, 0);

        // Read and count until we receive END
        int frame_nbr = 0;
        while (true) {
            f = ZFrame.recvFrame(input);
            frame_nbr++;
            if (f.streq("END")) {
                f.destroy();
                break;
            }
        }
        assertEquals(6, frame_nbr);
        f = ZFrame.recvFrame(input, ZMQ.DONTWAIT);
        assertNull(f);

        ctx.close();
    }

    @Test
    public void testStringFrames() {
        ZContext ctx = new ZContext();
        Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zframe.test");
        Socket input = ctx.createSocket(ZMQ.PAIR);
        input.connect("inproc://zframe.test");

        ZFrame f1 = new ZFrame("Hello");
        assertEquals(5, f1.getData().length);
        f1.send(output, 0);

        ZFrame f2 = ZFrame.recvFrame(input);
        assertTrue(f2.hasData());
        assertEquals(5, f2.getData().length);
        assertTrue(f2.streq("Hello"));
        assertEquals(f2.toString(), "Hello");
        assertTrue(f2.equals(f1));

        ctx.close();
    }
}
