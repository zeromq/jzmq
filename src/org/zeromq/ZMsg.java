package org.zeromq;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.zeromq.ZMQ.Socket;

/**
 * The ZMsg class provides methods to send and receive multipart messages
 * across 0MQ sockets. This class provides a list-like container interface,
 * with methods to work with the overall container.  ZMsg messages are
 * composed of zero or more ZFrame objects.
 * 
 * <pre>
 * // Send a simple single-frame string message on a ZMQSocket "output" socket object
 * ZMsg.newStringMsg("Hello").send(output);
 * 
 * // Add several frames into one message
 * ZMsg msg = new ZMsg();
 * for (int i = 0;i< 10;i++) {
 *     msg.addString("Frame" + i);
 * }
 * msg.send(output);
 * 
 * // Receive message from ZMQSocket "input" socket object and iterate over frames
 * ZMsg receivedMessage = ZMsg.recvMsg(input);
 * for (ZFrame f : receivedMessage) {
 *     // Do something with frame f (of type ZFrame)
 * }
 * </pre>
 *
 * Based on <a href="http://github.com/zeromq/czmq/blob/master/src/zmsg.c">zmsg.c</a> in czmq
 *
 */
public class ZMsg implements Iterable<ZFrame>, Deque<ZFrame>{
	
	/**
	 * Hold internal list of ZFrame objects
	 */
	private ArrayDeque<ZFrame> frames;
	
	/**
	 * Class Constructor
	 */
	protected ZMsg() {
		frames = new ArrayDeque<ZFrame>();
	}
	
	/**
	 * Destructor.
	 * Explicitly destroys all ZFrames contains in the ZMsg
	 */
	public void destroy() {
		if (frames == null)
			return;
		for (ZFrame f : frames) {
			f.destroy();
		}
		frames.clear();
	}
	
	
	/**
	 * Return total number of bytes contained in all ZFrames in this ZMsg
	 * @return
	 */
	public long contentSize() {
		long size = 0;
		for (ZFrame f : frames) {
			size += f.size();
		}
		return size;
	}

	/**
	 * Add a String as a new ZFrame to the end of list
	 * @param str
	 * 				String to add to list
	 */
	public void addString(String str) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		frames.add(new ZFrame(str));
	}
	
	/**
	 * Creates copy of this ZMsg.
	 * Also duplicates all frame content.
	 * @return
	 * 			The duplicated ZMsg object, else null if this ZMsg contains an empty frame set
	 */
	public ZMsg duplicate() {
		if (frames != null) {
			ZMsg msg = new ZMsg();
			for (ZFrame f : frames)
				msg.add(f.duplicate());
			return msg;
		} else
			return null;
	}
	
	/**
	 * Push frame plus empty frame to front of message, before 1st frame.
	 * Message takes ownership of frame, will destroy it when message is sent.
	 * @param frame
	 */
	public void wrap(ZFrame frame) {
		if (frame != null) {
			push(new ZFrame(""));
			push(frame);
		}
	}
	
	/**
	 * Pop frame off front of message, caller now owns frame.
	 * If next frame is empty, pops and destroys that empty frame
	 * (e.g. useful when unwrapping ROUTER socket envelopes)
	 * @return
	 * 			Unwrapped frame
	 */
	public ZFrame unwrap() {
		if (size() == 0)
			return null;
		ZFrame f = pop();
		ZFrame empty = getFirst();
		if (empty.hasData() && empty.size() == 0) {
			empty = pop();
			empty.destroy();
		}
		return f;
	}
	
	/**
	 * Send message to 0MQ socket, destroys contents after sending.
	 * If the message has no frames, sends nothing but still destroy()s the ZMsg object
	 * @param socket
	 * 				0MQ socket to send ZMsg on.
	 */
	public void send(Socket socket) {
		if (socket == null)
			throw new IllegalArgumentException("socket is null");
		if (frames.size() == 0)
			return;
		Iterator<ZFrame> i = frames.iterator();
		while(i.hasNext()) {
			ZFrame f = i.next();
			f.sendAndKeep(socket, (i.hasNext()) ? ZMQ.SNDMORE : 0);
		}
		destroy();
	}
	
	
	
	/**
     * Receives message from socket, returns ZMsg object or null if the
     * recv was interrupted. Does a blocking recv, if you want not to block then use
     * the ZLoop class or ZMQ.Poller to check for socket input before receiving.
     * @param	socket
	 * @return
	 */
	public static ZMsg recvMsg(Socket socket) {
		if (socket == null)
			throw new IllegalArgumentException("socket is null");

		ZMsg msg = new ZMsg();
		
		while (true) {
			ZFrame f = ZFrame.recvFrame(socket);
			if (f == null) {
				// If receive failed or was interrupted
				msg.destroy();
				break;
			}
			msg.add(f);
			if (!f.hasMore())
				break;
		}
		return msg;
	}

	/**
	 * Save message to an open data output stream.
	 * 
	 * Data saved as:
	 * 		4 bytes: number of frames
	 * 	For every frame:
	 * 		4 bytes: byte size of frame data
	 * 		+ n bytes: frame byte data
	 * 
	 * @param msg
	 * 			ZMsg to save
	 * @param file
	 * 			DataOutputStream
	 * @return
	 * 			True if saved OK, else false
	 */
	public static boolean save(ZMsg msg, DataOutputStream file) {
		if (msg == null)
			return false;

		try {
			// Write number of frames
			file.writeInt(msg.size());
			if (msg.size() > 0 ) {
				for (ZFrame f : msg) {
					// Write byte size of frame
					file.writeInt(f.size());
					// Write frame byte data
					file.write(f.getData());
				}
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}
	
	/**
	 * Load / append a ZMsg from an open DataInputStream
	 * 
	 * @param file
	 * 			DataInputStream connected to file
	 * @return
	 * 			ZMsg object
	 */
	public static ZMsg load(DataInputStream file) {
		if (file == null)
			return null;
		ZMsg rcvMsg = new ZMsg();
		
		try {
			int msgSize = file.readInt();
			if (msgSize > 0) {
				int msgNbr = 0;
				while (++msgNbr <= msgSize) {
					int frameSize = file.readInt();
					byte[] data = new byte[frameSize];
					file.read(data);
					rcvMsg.add(new ZFrame(data));
				}
			}
			return rcvMsg;
		} catch (IOException e) {
			return null;
		}
	}
	
	// ********* Implement Iterable Interface *************** //
	@Override
	public Iterator<ZFrame> iterator() {
		// TODO Auto-generated method stub
		return frames.iterator();
	}

	// ********* Implement Deque Interface ****************** //
	@Override
	public boolean addAll(Collection<? extends ZFrame> arg0) {
		return frames.addAll(arg0);
	}

	@Override
	public void clear() {
		frames.clear();
		
	}

	@Override
	public boolean containsAll(Collection<?> arg0) {
		return frames.containsAll(arg0);
	}

	@Override
	public boolean isEmpty() {
		return frames.isEmpty();
	}

	@Override
	public boolean removeAll(Collection<?> arg0) {
		return frames.removeAll(arg0);
	}

	@Override
	public boolean retainAll(Collection<?> arg0) {
		return frames.retainAll(arg0);
	}

	@Override
	public Object[] toArray() {
		return frames.toArray();
	}

	@Override
	public <T> T[] toArray(T[] arg0) {
		return frames.toArray(arg0);
	}

	@Override
	public boolean add(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		return frames.add(e);
	}

	@Override
	public void addFirst(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		frames.addFirst(e);
	}

	@Override
	public void addLast(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		frames.addLast(e);
		
	}

	@Override
	public boolean contains(Object o) {
		return frames.contains(o);
	}

	@Override
	public Iterator<ZFrame> descendingIterator() {
		return frames.descendingIterator();
	}

	@Override
	public ZFrame element() {
		return frames.element();
	}

	@Override
	public ZFrame getFirst() {
		try {
			return frames.getFirst();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public ZFrame getLast() {
		try {
			return frames.getLast();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public boolean offer(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		return frames.offer(e);
	}

	@Override
	public boolean offerFirst(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		return frames.offerFirst(e);
	}

	@Override
	public boolean offerLast(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		return frames.offerLast(e);
	}

	@Override
	public ZFrame peek() {
		return frames.peek();
	}

	@Override
	public ZFrame peekFirst() {
		try {
			return frames.peekFirst();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public ZFrame peekLast() {
		try {
			return frames.peekLast();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public ZFrame poll() {
		return frames.poll();
	}

	@Override
	public ZFrame pollFirst() {
		return frames.pollFirst();
	}

	@Override
	public ZFrame pollLast() {
		return frames.pollLast();
	}

	@Override
	public ZFrame pop() {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		try {
			return frames.pop();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public void push(ZFrame e) {
		if (frames == null)
			frames = new ArrayDeque<ZFrame>();
		frames.push(e);
	}

	@Override
	public ZFrame remove() {
		return frames.remove();
	}

	@Override
	public boolean remove(Object o) {
		return frames.remove(o);
	}

	@Override
	public ZFrame removeFirst() {
		try {
			return frames.removeFirst();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		return frames.removeFirstOccurrence(o);
	}

	@Override
	public ZFrame removeLast() {
		try {
			return frames.removeLast();
		} catch (NoSuchElementException e) {
			return null;
		}
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		return frames.removeLastOccurrence(o);
	}

	@Override
	public int size() {
		return frames.size();
	}
	
	
}
