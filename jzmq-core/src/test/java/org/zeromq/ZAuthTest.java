package org.zeromq;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test are basically the java-ports based on <a href="http://hintjens.com/blog:49">Using ZeroMQ Security (part 2)</a>
 * 
 * @author thomas (dot) trocha (at) gmail (dot) com
 *
 */
public class ZAuthTest {

	private static final String CERTIFICATE_FOLDER=".curve";
	
	@Before
	public void init() {
		// create test-passwords
		try {
			FileWriter write = new FileWriter("passwords");
			write.write("guest=guest\n"); 
			write.write("tourist=1234\n");
			write.write("admin=secret\n");
			write.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testPlainWithPassword() {
	    //  Create context
		ZContext ctx = new ZContext();
		try {
		    //  Start an authentication engine for this context. This engine
		    //  allows or denies incoming connections (talking to the libzmq
		    //  core over a protocol called ZAP).
			ZAuth auth = new ZAuth(ctx);
		    //  Get some indication of what the authenticator is deciding
		    auth.setVerbose(true);
		    //  Whitelist our address; any other address will be rejected
		    auth.allow("127.0.0.1");
		    auth.configurePlain("*", "passwords");
		        
		    //  Create and bind server socket
		    ZMQ.Socket server = ctx.createSocket(ZMQ.PUSH);
		    server.setPlainServer(true);
		    server.setZAPDomain("global".getBytes());
		    server.bind("tcp://*:9000");
		    
		    //  Create and connect client socket
		    ZMQ.Socket client = ctx.createSocket(ZMQ.PULL);
		    client.setPlainUsername("admin".getBytes());
		    client.setPlainPassword("secret".getBytes());
		    client.connect("tcp://127.0.0.1:9000");
		    
		    //  Send a single message from server to client
		    server.send("Hello");
		    String message = client.recvStr(0,Charset.defaultCharset());
		    
		    assert(message.equals("Hello"));
		}
		finally {
			ctx.close();	
		}
	}
	
	@Test
	public void testCurveAnyClient() {
		// accept any client-certificate

		//  Create context
		ZContext ctx = new ZContext();
		try {
			//
			
		    //  Start an authentication engine for this context. This engine
		    //  allows or denies incoming connections (talking to the libzmq
		    //  core over a protocol called ZAP).
			ZAuth auth = new ZAuth(ctx);
		    //  Get some indication of what the authenticator is deciding
		    auth.setVerbose(true);
		    //  Whitelist our address; any other address will be rejected
		    auth.allow("127.0.0.1");
		    auth.configureCurve(ZAuth.CURVE_ALLOW_ANY);

		    //  We need two certificates, one for the client and one for
		    //  the server. The client must know the server's public key
		    //  to make a CURVE connection.
		    ZCert client_cert = new ZCert();
		    ZCert server_cert = new ZCert();
		    
		    //  Create and bind server socket
		    ZMQ.Socket server = ctx.createSocket(ZMQ.PUSH);
		    server.setZAPDomain("global".getBytes());
		    server.setCurveServer(true);
		    server.setCurvePublicKey(server_cert.getPublicKey());
		    server.setCurveSecretKey(server_cert.getSecretKey());
		    server.bind("tcp://*:9000");
		    
		    //  Create and connect client socket
		    ZMQ.Socket client = ctx.createSocket(ZMQ.PULL);
		    client.setCurvePublicKey(client_cert.getPublicKey());
		    client.setCurveSecretKey(client_cert.getSecretKey());
		    client.setCurveServerKey(server_cert.getPublicKey());
		    client.connect("tcp://127.0.0.1:9000");
		    
		    //  Send a single message from server to client
		    server.send("Hello");
		    String message = client.recvStr(0,Charset.defaultCharset());
		    
		    if (message.equals("Hello")) {
		    	System.out.println("Strawhouse test OK");
		    }
		}
		finally {
			ctx.close();	
		}
	    
	}
	
	@Test
	public void testCurveSuccessful() {
		ZContext ctx = new ZContext();
		try {
		    //  Start an authentication engine for this context. This engine
		    //  allows or denies incoming connections (talking to the libzmq
		    //  core over a protocol called ZAP).
			ZAuth auth = new ZAuth(ctx);
		    //  Get some indication of what the authenticator is deciding
		    auth.setVerbose(true);
		    //  Whitelist our address; any other address will be rejected
		    auth.allow("127.0.0.1");
		    //  Tell authenticator to use the certificate store in .curve
		    auth.configureCurve(CERTIFICATE_FOLDER);

		    //  We'll generate a new client certificate and save the public part
		    //  in the certificate store (in practice this would be done by hand
		    //  or some out-of-band process).
		    ZCert client_cert = new ZCert();
		    client_cert.setMeta("name", "Client test certificate");
		    client_cert.savePublic(CERTIFICATE_FOLDER+"/testcert.pub");
		    
		    ZCert server_cert = new ZCert();
		    
		    //  Create and bind server socket
		    ZMQ.Socket server = ctx.createSocket(ZMQ.PUSH);
		    server.setZAPDomain("global".getBytes());
		    server.setCurveServer(true);
		    server.setCurvePublicKey(server_cert.getPublicKey());
		    server.setCurveSecretKey(server_cert.getSecretKey());
		    server.bind("tcp://*:9000");
		    
		    //  Create and connect client socket
		    ZMQ.Socket client = ctx.createSocket(ZMQ.PULL);
		    client.setCurvePublicKey(client_cert.getPublicKey());
		    client.setCurveSecretKey(client_cert.getSecretKey());
		    client.setCurveServerKey(server_cert.getPublicKey());
		    client.connect("tcp://127.0.0.1:9000");
		    client.setReceiveTimeOut(100);
		    
		    //  Send a single message from server to client
		    boolean sendSuccessful = server.send("Hello");
		    assert(sendSuccessful);
		    
		    String message = client.recvStr(0,Charset.defaultCharset());
		    assert(message.equals("Hello"));
		}
		finally {
			ctx.close();	 
		}
	    		
	}

	@Test
	public void testCurveFail() {
		// this is the same test but here we do not save the client's certificate into the certstore's folder
		ZContext ctx = new ZContext();
		try {
		    //  Start an authentication engine for this context. This engine
		    //  allows or denies incoming connections (talking to the libzmq
		    //  core over a protocol called ZAP).
			ZAuth auth = new ZAuth(ctx);
		    //  Get some indication of what the authenticator is deciding
		    auth.setVerbose(true);
		    //  Whitelist our address; any other address will be rejected
		    auth.allow("127.0.0.1");
		    //  Tell authenticator to use the certificate store in .curve
		    auth.configureCurve(CERTIFICATE_FOLDER);

		    //  We'll generate a new client certificate and save the public part
		    //  in the certificate store (in practice this would be done by hand
		    //  or some out-of-band process).
		    ZCert client_cert = new ZCert();
		    client_cert.setMeta("name", "Client test certificate");
		    
		    // HERE IS THE PROBLEM. Not client-certificate means that the client will be rejected
//		    client_cert.savePublic(CERTIFICATE_FOLDER+"/testcert.pub");
		    
		    ZCert server_cert = new ZCert();
		    
		    //  Create and bind server socket
		    ZMQ.Socket server = ctx.createSocket(ZMQ.PUSH);
		    server.setZAPDomain("global".getBytes());
		    server.setCurveServer(true);
		    server.setCurvePublicKey(server_cert.getPublicKey());
		    server.setCurveSecretKey(server_cert.getSecretKey());
		    server.bind("tcp://*:9000");
		    
		    //  Create and connect client socket
		    ZMQ.Socket client = ctx.createSocket(ZMQ.PULL);
		    client.setCurvePublicKey(client_cert.getPublicKey());
		    client.setCurveSecretKey(client_cert.getSecretKey());
		    client.setCurveServerKey(server_cert.getPublicKey());
		    client.connect("tcp://127.0.0.1:9000");
		    // add a timeout so that the client won't wait forever (since it is not connected)
		    client.setReceiveTimeOut(100);
		    
		    //  Send a single message from server to client
		    boolean sendSuccessful = server.send("Hello");
		    assert(sendSuccessful);
		    
		    // the timeout will leave the recvStr-method with null as result
		    String message = client.recvStr(0,Charset.defaultCharset());
		    assert(message == null);
		}
		finally {
			ctx.close();	 
		}
	}
	
	@After
	public void cleanup() {
		File deletePasswords = new File("passwords");
		deletePasswords.delete();
	}
}
