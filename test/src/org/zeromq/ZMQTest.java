package org.zeromq;

import org.junit.Test;

import java.util.Arrays;

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

}
