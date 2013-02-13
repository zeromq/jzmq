package org.zeromq;

import org.junit.Test;
import org.zeromq.ZMQ.Socket;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ZMsgTest {

	@Test
    public void testMessageEquals() {
        ZMsg msg = new ZMsg();
        ZFrame hello = new ZFrame("Hello");
        ZFrame world = new ZFrame("World");
        msg.add(hello);
        msg.add(world);
        assertEquals(msg, msg.duplicate());

        ZMsg reverseMsg = new ZMsg();
        msg.add(hello);
        msg.addFirst(world);
        assertFalse(msg.equals(reverseMsg));
    }

	@Test
	public void testSingleFrameMessage() {
		ZContext ctx = new ZContext();

		Socket output = ctx.createSocket(ZMQ.PAIR);
		output.bind("inproc://zmsg.test");
		Socket input = ctx.createSocket(ZMQ.PAIR);
		input.connect("inproc://zmsg.test");

		// Test send and receive of a single ZMsg
		ZMsg msg = new ZMsg();
		ZFrame frame = new ZFrame("Hello");
		msg.addFirst(frame);
		assertEquals(1, msg.size());
		assertEquals(5, msg.contentSize());
		msg.send(output);
		
		ZMsg msg2 = ZMsg.recvMsg(input);
		assertTrue(msg2 != null);
		assertEquals(1, msg2.size());
		assertEquals(5, msg2.contentSize());
		
		msg.destroy();
		msg2.destroy();
		ctx.destroy();
	}
	
	@Test
	public void testMultiPart() {
		ZContext ctx = new ZContext();

		Socket output = ctx.createSocket(ZMQ.PAIR);
		output.bind("inproc://zmsg.test2");
		Socket input = ctx.createSocket(ZMQ.PAIR);
		input.connect("inproc://zmsg.test2");
		
		ZMsg msg = new ZMsg();
		for (int i = 0;i < 10;i++)
			msg.addString("Frame" + i);
		ZMsg copy = msg.duplicate();
		copy.send(output);
		msg.send(output);
		
		copy = ZMsg.recvMsg(input);
		assertTrue(copy != null);
		assertEquals(10, copy.size());
		assertEquals(60, copy.contentSize());
		copy.destroy();
		
		msg = ZMsg.recvMsg(input);
		assertTrue(msg != null);
		assertEquals(10, msg.size());
		int count  = 0;
		for (ZFrame f : msg)
			assertTrue(f.streq("Frame" + count++));
		assertEquals(60, msg.contentSize());
		msg.destroy();
		
		ctx.destroy();
	}
	
	@Test
	public void testMessageFrameManipulation() {
		ZMsg msg = new ZMsg();
		for (int i=0;i<10;i++) 
			msg.addString("Frame"+i);
		
		// Remove all frames apart from the first and last one
		for (int i=0;i<8;i++) {
			Iterator<ZFrame> iter = msg.iterator();
			iter.next();	// Skip first frame
			ZFrame f = iter.next();
			msg.remove(f);
			f.destroy();
		}
		
		assertEquals(2, msg.size());
		assertEquals(12, msg.contentSize());
		assertTrue(msg.getFirst().streq("Frame0"));
		assertTrue(msg.getLast().streq("Frame9"));
		
		ZFrame f = new ZFrame("Address");
		msg.push(f);
		assertEquals(3, msg.size());
		assertTrue(msg.getFirst().streq("Address"));
		
		msg.addString("Body");
		assertEquals(4, msg.size());
		ZFrame f0 = msg.pop();
		assertTrue(f0.streq("Address"));
		
		msg.destroy();
		
		msg = new ZMsg();
		f = new ZFrame("Address");
		msg.wrap(f);
		assertEquals(2, msg.size());
		msg.addString("Body");
		assertEquals(3, msg.size());
		f = msg.unwrap();
		f.destroy();
		assertEquals(1, msg.size());
		msg.destroy();
		
	}
	
	@Test
	public void testEmptyMessage() {
		ZMsg msg = new ZMsg();
		assertEquals(0, msg.size());
		assertEquals(null, msg.getFirst());
		assertEquals(null, msg.getLast());
		assertTrue(msg.isEmpty());
		assertEquals(null, msg.pop());
		assertEquals(null,msg.removeFirst());
		assertEquals(false,msg.removeFirstOccurrence(null));
		assertEquals(null,msg.removeLast());
		
		msg.destroy();
		
	}
	
	@Test
	public void testLoadSave() {
		ZMsg msg = new ZMsg();
		for (int i = 0; i < 10; i++) 
			msg.addString("Frame"+i);
		
		try {
			// Save msg to a file
			File f = new File("zmsg.test");
			DataOutputStream dos = new DataOutputStream(new FileOutputStream(f));
			assertTrue(ZMsg.save(msg, dos));
			dos.close();
			
			// Read msg out of the file
			DataInputStream dis = new DataInputStream(new FileInputStream(f));
			ZMsg msg2 = ZMsg.load(dis);
			dis.close();
			f.delete();
			
			assertEquals(10, msg2.size());
			assertEquals(60, msg2.contentSize());
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			assertTrue(false);
		} catch (IOException e) {
			e.printStackTrace();
			assertTrue(false);
		}
	}

    @Test
    public void testNewStringMessage() {
        // A single string => frame
        ZMsg msg = ZMsg.newStringMsg("Foo");
        assertEquals(1, msg.size());
        assertTrue(msg.getFirst().streq("Foo"));

        // Multiple strings => frames
        ZMsg msg2 = ZMsg.newStringMsg("Foo", "Bar", "Baz");
        assertEquals(3, msg2.size());
        assertTrue(msg2.getFirst().streq("Foo"));
        assertTrue(msg2.getLast().streq("Baz"));

        // Empty message (Not very useful)
        ZMsg msg3 = ZMsg.newStringMsg();
        assertTrue(msg3.isEmpty());
    }
}
