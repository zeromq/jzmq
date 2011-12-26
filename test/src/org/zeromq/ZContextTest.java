package org.zeromq;

import org.junit.Test;
import org.zeromq.ZMQ.Socket;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests high-level ZContext class
 * @author richardsmith
 *
 */
public class ZContextTest {

	@Test
	public void testConstruction() {
		ZContext ctx = new ZContext();
		assertTrue(ctx != null);
		assertEquals(1, ctx.getIoThreads());
		assertEquals(0, ctx.getLinger());
		assertTrue(ctx.isMain());
	}
	
	@Test
	public void testDestruction() {
		ZContext ctx = new ZContext();
		ctx.destroy();
		assertTrue(ctx.getSockets().isEmpty());
		
		// Ensure context is not destroyed if not in main thread
		ZContext ctx1 = new ZContext();
		ctx1.setMain(false);
		@SuppressWarnings("unused")
		Socket s = ctx1.createSocket(ZMQ.PAIR);
		ctx1.destroy();
		assertTrue(ctx1.getSockets().isEmpty());
		assertTrue(ctx1.getContext() != null);
	}
	
	@Test
	public void testAddingSockets() {
		// Tests "internal" newSocket method, should not be used outside jzmq itself.
		ZContext ctx = new ZContext();
		try {
			Socket s = ctx.createSocket(ZMQ.PUB);
			assertTrue(s != null);
			assertTrue(s.getType() == ZMQ.PUB);
			Socket s1 = ctx.createSocket(ZMQ.REQ);
			assertTrue(s1 != null);
			assertEquals(2, ctx.getSockets().size());
		} catch (ZMQException e) {
			System.out.println("ZMQException:" + e.toString());
			assertTrue(false);
		}
		ctx.destroy();
	}
	
	@Test
	public void testRemovingSockets() {
		ZContext ctx = new ZContext();
		try {
			Socket s = ctx.createSocket(ZMQ.PUB);
			assertTrue (s != null);
			assertEquals(1, ctx.getSockets().size());
			
			ctx.destroySocket(s);
			assertEquals(0, ctx.getSockets().size());
		} catch (ZMQException e) {
			System.out.println("ZMQException:" + e.toString());
			assertTrue(false);
		}
		ctx.destroy();
	}
	
	@Test
	public void testShadow() {
		ZContext ctx = new ZContext();
		Socket s = ctx.createSocket(ZMQ.PUB);
		assertTrue (s != null);
		assertEquals(1, ctx.getSockets().size());

		ZContext shadowCtx = ZContext.shadow(ctx);
		shadowCtx.setMain(false);
		assertEquals(0, shadowCtx.getSockets().size());
		@SuppressWarnings("unused")
		Socket s1 = shadowCtx.createSocket(ZMQ.SUB);
		assertEquals(1, shadowCtx.getSockets().size());
		assertEquals(1, ctx.getSockets().size());

		shadowCtx.destroy();
		ctx.destroy();
	}
	
	
}
