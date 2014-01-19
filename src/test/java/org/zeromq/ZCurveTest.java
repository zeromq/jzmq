package org.zeromq;
/*
  Copyright (c) 2007-2010 iMatix Corporation

  This file is part of 0MQ.

  0MQ is free software; you can redistribute it and/or modify it under
  the terms of the Lesser GNU General Public License as published by
  the Free Software Foundation; either version 3 of the License, or
  (at your option) any later version.

  0MQ is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  Lesser GNU General Public License for more details.

  You should have received a copy of the Lesser GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

// FIXME: Debug only
import static java.lang.System.out;

import org.zeromq.ZMQ;
import org.zeromq.ZCurveKeyPair;

/**
 * @author James Gatannah
 */
public class ZCurveTest {

    @Test
    public void testKeyConversion() {
	out.println("Trying to generate a KeyPair");
	ZCurveKeyPair keys = ZCurveKeyPair.Factory();
	assertNotNull("building KeyPair instance failed", keys);

	// Gotta love the JNI. This shouldn't be an issue, but it is.
	out.println("KeyPair created. Accessing the public key");
	byte[] publicKey = keys.publicKey;
	out.println("Accessed: '" + new String(publicKey) + "'\n");

	// TODO: This should really be another test.
	// "Can I generate a key pair?" without throwing exceptions (or dumping
	// core) should be (and is) the first.
	// En/de-coding is really a second, though it's pretty vital.
	out.println("Encoding");
	String encoded = ZCurveKeyPair.Z85Encode(publicKey);
	int length = encoded.length();
	out.println("Decoding '" + encoded + "'\n which is " + length + " bytes long)");
	byte[] decoded = ZCurveKeyPair.Z85Decode(encoded);
	if(decoded != null) {
	    out.println("Successfully decoded *something*:\n'" + new String(decoded) + "'"); 
	}
	else {
	    out.println("Decoding failed. WTF?");
	    assertNotNull(decoded);
	}
	out.println("Decoded '" + new String(decoded) + "'");

	assertArrayEquals("failure - decoded doesn't match original", publicKey, decoded);
    }

    @Test
    public void testEncryptedPush() {
	// Pretty much shamelessly stolen from ZMQTest.testReqRep

	// No encryption before version 4
        if (ZMQ.getFullVersion() >= ZMQ.make_version(4, 0, 0)) {
	    ZCurveKeyPair clientKeys = ZCurveKeyPair.Factory();
	    assertNotNull("Client Key generation failed", clientKeys);
	    ZCurveKeyPair serverKeys = ZCurveKeyPair.Factory();
	    assertNotNull("Server Key generation failed", serverKeys);

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
