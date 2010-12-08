package org.zeromq;

import org.zeromq.ZMQ.Socket;

/**
 * ZeroMQ Forwarder Device implementation.
 * 
 * @author Alois Belaska <alois.belaska@gmail.com>
 */
public class ZMQForwarder implements Runnable {

	private final ZMQ.Socket inSocket;
	private final ZMQ.Socket outSocket;

	/**
	 * Class constructor.
	 * 
	 * @param inSocket
	 *            input socket
	 * @param outSocket
	 *            output socket
	 */
	public ZMQForwarder(Socket inSocket, Socket outSocket) {
		this.inSocket = inSocket;
		this.outSocket = outSocket;
	}

	/**
	 * Forwarding messages.
	 */
	@Override
	public void run() {
		byte[] msg = null;
		boolean more = true;

		while (true) {
			msg = inSocket.recv(0);

			more = inSocket.hasReceiveMore();

			if (msg != null) {
				outSocket.send(msg, more ? ZMQ.SNDMORE : 0);
			}
		}
	}
}
