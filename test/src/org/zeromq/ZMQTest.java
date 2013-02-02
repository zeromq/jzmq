package org.zeromq;

import org.junit.Test;

import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import static org.junit.Assert.*;


/**
 * @author Cliff Evans
 */
public class ZMQTest
{

  /**
   * Test method for {@link org.zeromq.ZMQ#makeVersion(int, int, int)}.
   */
  @Test
  public void testMakeVersion ()
  {
    assertEquals ( ZMQ.getFullVersion (),
                   ZMQ.makeVersion ( ZMQ.getMajorVersion (),
                                     ZMQ.getMinorVersion (),
                                     ZMQ.getPatchVersion () ) );
  }

  /**
   * Test method for {@link org.zeromq.ZMQ#getVersionString()}.
   */
  @Test
  public void testGetVersion ()
  {
    assertEquals ( ZMQ.getMajorVersion () + "." + ZMQ.getMinorVersion () + "." + ZMQ.getPatchVersion (),
                   ZMQ.getVersionString () );
  }

  /**
   * Test method for {@link org.zeromq.ZMQ.Socket#bindToRandomPort(String)}.
   */
  @Test
  public void testBindToRandomPort()
  {
    ZMQ.Context context = ZMQ.context(1);
    ZMQ.Socket sock = context.socket(ZMQ.DEALER);

    // Check that bindToRandomport generate valid port number
    for (int i=0; i < 100; i++) {
    	sock.bindToRandomPort("tcp://127.0.0.1");
    }

    sock.close();
    sock = context.socket(ZMQ.DEALER);

    // Check that exception different of EADDRINUSE is not catched

    // Invalid protocol
    try {
    	sock.bindToRandomPort("noprotocol://127.0.0.1");
    } catch (ZMQException e) {
    	assertEquals(e.getErrorCode(), ZMQ.EPROTONOSUPPORT());
    }
  }

  @Test
  public void testReqRep ()
  {
	  	ZMQ.Context context = ZMQ.context(1);

		ZMQ.Socket in = context.socket(ZMQ.REQ);
		in.bind("inproc://reqrep");

		ZMQ.Socket out = context.socket(ZMQ.REP);
		out.connect("inproc://reqrep");

		for (int i = 0; i < 10; i++) {
			byte[] req = ("request" + i).getBytes();
			byte[] rep = ("reply" + i).getBytes();

			assertTrue(in.send(req, 0));
			byte[] reqTmp = out.recv(0);
			assertArrayEquals(req, reqTmp);

			assertTrue(out.send(rep, 0));
			byte[] repTmp = in.recv(0);
			assertArrayEquals(rep, repTmp);
		}
  }

    @Test
    public void testXPUBSUB ()
    {
        if (ZMQ.getFullVersion() <  ZMQ.make_version(3, 0, 0)) {
          // Can only test XPUB on ZMQ >= of 3.0
          return;
        }

        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket pub = context.socket(ZMQ.XPUB);
        pub.bind("inproc://xpub");

        ZMQ.Socket sub = context.socket(ZMQ.SUB);
        sub.connect("inproc://xpub");
        ZMQ.Socket xsub = context.socket(ZMQ.XSUB);
        xsub.connect("inproc://xpub");

        sub.subscribe("".getBytes());
        byte[] subcr = pub.recv(0);
        assertArrayEquals(new byte[]{1}, subcr);

        sub.unsubscribe("".getBytes());
        subcr = pub.recv(0);
        assertArrayEquals(new byte[]{0}, subcr);

        byte[] subscription = "subs".getBytes();

        // Append subscription
        byte[] expected = new byte[subscription.length + 1];
        expected[0] = 1;
        System.arraycopy(subscription, 0, expected, 1, subscription.length);

        sub.subscribe(subscription);
        subcr = pub.recv(0);
        assertArrayEquals(expected, subcr);

        // Verify xsub subscription
        xsub.send(expected, 0);
        subcr = pub.recv(1);
        assertNull(subcr);

        for (int i = 0; i < 10; i++) {
            byte[] data = ("subscrip" + i).getBytes();

            assertTrue(pub.send(data, 0));
            // Verify SUB
            byte[] tmp = sub.recv(0);
            assertArrayEquals(data, tmp);

            // Verify XSUB
            tmp = xsub.recv(0);
            assertArrayEquals(data, tmp);
        }
    }

    /**
     * Test method for various set/get options.
     */
    @Test
    public void testSetOption ()
    {
        ZMQ.Context context = ZMQ.context(1);

        ZMQ.Socket sock = context.socket(ZMQ.REQ);

        if (ZMQ.getFullVersion() >= ZMQ.makeVersion(3, 2, 0)) {
            sock.setIPv4Only (false);
            assertEquals (false, sock.getIPv4Only ());

            sock.setIPv4Only (true);
            assertEquals (true, sock.getIPv4Only ());
        }
    }

    static class Client extends Thread {

        private Socket s = null;
        private String name = null;
        public Client (Context ctx, String name_) {
            s = ctx.socket(ZMQ.REQ);
            name = name_;

            s.setIdentity(name.getBytes ());
        }

        @Override
        public void run () {
            s.connect( "tcp://127.0.0.1:6660");
            s.send("hello", 0);
            String msg = s.recvStr(0);
            s.send("world", 0);
            msg = s.recvStr(0);

            s.close();
        }
    }

    static class Dealer extends Thread {

        private Socket s = null;
        private String name = null;
        public Dealer(Context ctx, String name_) {
            s = ctx.socket(ZMQ.DEALER);
            name = name_;

            s.setIdentity(name.getBytes ());
        }

        @Override
        public void run () {

            s.connect( "tcp://127.0.0.1:6661");
            int count = 0;
            while (count < 2) {
                String msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }
                String identity = msg;
                msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }

                msg = s.recvStr(0);
                if (msg == null) {
                    throw new RuntimeException();
                }

                s.send(identity, ZMQ.SNDMORE);
                s.send("", ZMQ.SNDMORE);
                String response = "OK " + msg;

                s.send(response, 0);
                count++;
            }
            s.close();
        }
    }
    static class Main extends Thread {

        Context ctx;
        Main(Context ctx_) {
            ctx = ctx_;
        }

        @Override
        public void run() {
            Socket frontend = ctx.socket(ZMQ.ROUTER);

            assertNotNull (frontend);
            frontend.bind ("tcp://127.0.0.1:6660");


            Socket backend = ctx.socket(ZMQ.DEALER);
            assertNotNull (backend);
            backend.bind ("tcp://127.0.0.1:6661");

            ZMQ.proxy (frontend, backend, null);

            frontend.close();
            backend.close();
        }

    }

    @Test
    public void testProxy ()  throws Exception {

        if (ZMQ.getFullVersion() <  ZMQ.make_version(3, 2, 2)) {
            // Can only test zmq_proxy on ZMQ >= of 3.2.2
            return;
        }

        Context ctx = ZMQ.context (1);
        assert (ctx!= null);

        Main mt = new Main(ctx);
        mt.start();
        new Dealer(ctx, "AA").start();
        new Dealer(ctx, "BB").start();

        Thread.sleep(1000);
        Thread c1 = new Client(ctx, "X");
        c1.start();

        Thread c2 = new Client(ctx, "Y");
        c2.start();

        c1.join();
        c2.join();

        ctx.term();
    }
}
