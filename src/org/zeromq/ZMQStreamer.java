package org.zeromq;

import org.zeromq.ZMQ.Socket;

/**
 * ZeroMQ Streamer Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQStreamer extends ZMQForwarder {

	/**
	 * Class constructor.
	 * 
	 * @param inSocket
	 *            input socket
	 * @param outSocket
	 *            output socket
	 */
	public ZMQStreamer(Socket inSocket, Socket outSocket) {
		super(inSocket, outSocket);
	}
}
