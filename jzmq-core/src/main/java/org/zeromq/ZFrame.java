package org.zeromq;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import org.zeromq.ZMQ.Socket;

/**
 * ZFrame The ZFrame class provides methods to send and receive single message
 * frames across 0MQ sockets. A 'frame' corresponds to one underlying zmq_msg_t
 * in the libzmq code. When you read a frame from a socket, the more() method
 * indicates if the frame is part of an unfinished multipart message. The send()
 * method normally destroys the frame, but with the ZFRAME_REUSE flag, you can
 * send the same frame many times. Frames are binary, and this class has no
 * special support for text data. Based on <a
 * href="http://github.com/zeromq/czmq/blob/master/src/zframe.c">zframe.c</a> in
 */
public class ZFrame
{
    private static Charset ENCODING = Charset.forName("UTF-8");

    private byte[] data;
    private boolean more;
    private int capacity;
    private ByteBuffer buffer;

    public ZFrame()
    {
    }

    public ZFrame(final byte[] data)
    {
        Utils.checkNotNull(data);
        init(data);
    }

    public ZFrame(final ByteBuffer buffer)
    {
        Utils.checkNotNull(buffer);
        if (buffer.hasArray()) {
            this.data = buffer.array();
        }
        this.capacity = buffer.capacity();
        this.buffer = buffer;
    }

    public ZFrame(String data)
    {
        Utils.checkNotNull(data);
        init(data.getBytes(ENCODING));
    }

    private void init(final byte[] data)
    {
        this.data = data;
        this.buffer = ByteBuffer.wrap(this.data);
        this.capacity = this.data.length;

    }

    public int capacity()
    {
        return capacity;
    }

    /**
     * Destructor.
     */
    public void destroy()
    {
        data = null;
        buffer = null;
        capacity = 0;
    }

    /**
     * @return the data
     */
    public byte[] getData()
    {
        return data;
    }

    /**
     * @return More flag, true if last read had MORE message parts to come
     */
    public boolean hasMore()
    {
        return more;
    }

    /**
     * Returns byte size of frame, if set, else 0
     * @return Number of bytes in frame data, else 0
     */
    public int size()
    {
        return hasData() ? data.length : 0;
    }

    /**
     * Convenience method to ascertain if this frame contains some message data
     * @return True if frame contains data
     */
    public boolean hasData()
    {
        return data != null;
    }

    /**
     * Method to call org.zeromq.Socket send() method.
     * @param socket
     *            0MQ socket to send on
     * @param flags
     *            Valid send() method flags, defined in org.zeromq.ZMQ class
     * @return True if success, else False
     */
    public boolean send(Socket socket, int flags)
    {
        // Note the jzmq Socket.cpp JNI class does a memcpy of the byte data
        // before calling
        // the 0MQ send function, so don't have to clone the message data again
        // here.
        return socket.send(hasData() ? data : new byte[0], flags);
    }

    /**
     * Creates a new frame that duplicates an existing frame
     * @return Duplicate of frame; message contents copied into new byte array
     */
    public ZFrame duplicate()
    {
        int length = size();
        byte[] copy = new byte[length];
        System.arraycopy(this.data, 0, copy, 0, length);
        ZFrame frame = new ZFrame();
        frame.data = copy;
        if (this.buffer != null) {
            frame.buffer = this.buffer.duplicate();
        }
        frame.more = this.more;
        return frame;
    }

    /**
     * Sets new contents for frame
     * @param data
     *            New byte array contents for frame
     */
    public void reset(byte[] data)
    {
        init(data);
    }

    /**
     * Sets new contents for frame
     * @param data
     *            String contents for frame
     */
    public void reset(String data)
    {
        reset(data.getBytes(ENCODING));
    }

    /**
     * Returns frame data as a printable hex string
     * @return
     */
    public String strhex()
    {
        String hexChar = "0123456789ABCDEF";

        StringBuilder b = new StringBuilder();
        for (int nbr = 0; nbr < data.length; nbr++) {
            int b1 = data[nbr] >>> 4 & 0xf;
            int b2 = data[nbr] & 0xf;
            b.append(hexChar.charAt(b1));
            b.append(hexChar.charAt(b2));
        }
        return b.toString();
    }

    /**
     * String equals. Uses String compareTo for the comparison (lexigraphical)
     * @param str
     *            String to compare with frame data
     * @return True if frame body data matches given string
     */
    public boolean streq(String str)
    {
        if (!hasData())
            return false;
        return new String(this.data).compareTo(str) == 0;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        ZFrame zFrame = (ZFrame) o;

        if (!Arrays.equals(data, zFrame.data))
            return false;
        return true;
    }

    @Override
    public int hashCode()
    {
        int hashcode = Arrays.hashCode(data);
        hashcode += more ? 1231 : 1237;
        return hashcode;
    }

    /**
     * Returns a human - readable representation of frame's data
     * @return A text string or hex-encoded string if data contains any
     *         non-printable ASCII characters
     */
    public String toString()
    {
        if (!hasData())
            return "";
        // Dump message as text or hex-encoded string
        boolean isText = true;
        for (int i = 0; i < data.length; i++) {
            if (data[i] < 32 || data[i] > 127)
                isText = false;
        }
        if (isText)
            return new String(data);
        else
            return strhex();
    }

    /**
     * Internal method to call recv on the socket. Does not trap any
     * ZMQExceptions but expects caling routine to handle them.
     * @param socket
     *            0MQ socket to read from
     * @return Byte array
     */
    private byte[] recv(Socket socket, int flags)
    {
        try {
            data = socket.recv(flags);
            more = socket.hasReceiveMore();
        } catch (ZMQException e) {
            ZMQ.Error error = ZMQ.Error.findByCode(e.getErrorCode());
            if (error == ZMQ.Error.ETERM || error == ZMQ.Error.ENOTSOCK) {
                data = null;
                more = false;
            } else {
                throw e;
            }
        }
        return data;
    }

    /**
     * Receives single frame from socket, returns the received frame object, or
     * null if the recv was interrupted. Does a blocking recv, if you want to
     * not block then use recvFrame(socket, ZMQ.DONTWAIT);
     * @param socket
     *            Socket to read from
     * @return received frame, else null
     */
    public static ZFrame recvFrame(Socket socket)
    {
        return recvFrame(socket, 0);
    }

    /**
     * Receive a new frame off the socket, Returns newly-allocated frame, or
     * null if there was no input waiting, or if the read was interrupted.
     * @param socket
     *            Socket to read from
     * @param flags
     *            Pass flags to 0MQ socket.recv call
     * @return received frame, else null
     */
    public static ZFrame recvFrame(Socket socket, int flags)
    {
        ZFrame f = new ZFrame();
        byte[] data = f.recv(socket, flags);
        if (data == null) {
            f = null;
        }
        return f;
    }
}
