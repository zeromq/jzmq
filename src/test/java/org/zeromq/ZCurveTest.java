package org.zeromq;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * @author James Gatannah
 */
public class ZCurveTest {

    @Test
    public void testKeyCenversion() {
	ZMQ.CurveKeyPair keys = ZMQ.curveKeyPairFactory();
	byte[] publicKey = keys.publicKey;

	String encoded = ZMQ.Z85Encode(publicKey);
	byte[] decoded = ZMQ.Z85Decode(encoded);

	assertArrayEquals("failure - decoded doesn't match original", publicKey, decoded);
    }

    @Test
    public void testEncryptedPush() {
	// Pretty much shamelessly stolen from ZMQTest.testReqRep

	// No encryption before version 4
        if (ZMQ.getFullVersion() >= ZMQ.make_version(4, 0, 0)) {
	    ZMQ.CurveKeyPair clientKeys = ZMQ.curveKeyPairFactory();
	    ZMQ.CurveKeyPair serverKeys = ZMQ.curveKeyPairFactory();

	    ZMQ.Context context = ZMQ.context(1);

	    ZMQ.Socket in = context.socket(ZMQ.REQ);
	    in.makeIntoCurveClient(clientKeys, serverKeys.publicKey);
	    in.bind("inproc://reqrep");

	    ZMQ.Socket out = context.socket(ZMQ.REP);
	    out.makeIntoCurveServer(serverKeys.privateKey);
	    out.connect("inproc://reqrep");

	    for (int i = 0; i < 10; i++) {
		byte[] req = ("request" + i).getBytes();
		byte[] rep = ("reply" + i).getBytes();

		assertTrue(in.send(req, 0));
		byte[] requestReceived = out.recv(0);
		assertArrayEquals(req, requestReceived);

		assertTrue(out.send(rep, 0));
		byte[] responseReceived = in.recv(0);
		assertArrayEquals(rep, responseReceived);
	    }
	}
    }
}
