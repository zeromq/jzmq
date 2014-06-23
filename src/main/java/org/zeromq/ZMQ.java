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
package org.zeromq;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.channels.SelectableChannel;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ZeroMQ JNI Bindings.
 * 
 * @author Gonzalo Diethelm
 * 
 */
public class ZMQ {

    static {
        // if no embedded native library, revert to loading from java.library.path
        if (!EmbeddedLibraryTools.LOADED_EMBEDDED_LIBRARY)
            System.loadLibrary("jzmq");
    }

    // Values for flags in Socket's send and recv functions.
    /**
     * Socket flag to indicate a nonblocking send or recv mode.
     */
    public static final int NOBLOCK = 1;
    public static final int DONTWAIT = 1;
    /**
     * Socket flag to indicate that more message parts are coming.
     */
    public static final int SNDMORE = 2;

    // Socket types, used when creating a Socket.
    /**
     * Flag to specify a exclusive pair of items.
     */
    public static final int PAIR = 0;
    /**
     * Flag to specify a PUB socket, receiving side must be a SUB or XSUB.
     */
    public static final int PUB = 1;
    /**
     * Flag to specify the receiving part of the PUB or XPUB socket.
     */
    public static final int SUB = 2;
    /**
     * Flag to specify a REQ socket, receiving side must be a REP.
     */
    public static final int REQ = 3;
    /**
     * Flag to specify the receiving part of a REQ socket.
     */
    public static final int REP = 4;
    /**
     * Flag to specify a DEALER socket (aka XREQ). DEALER is really a combined ventilator / sink that does
     * load-balancing on output and fair-queuing on input with no other semantics. It is the only socket type that lets
     * you shuffle messages out to N nodes and shuffle the replies back, in a raw bidirectional asynch pattern.
     */
    public static final int DEALER = 5;
    /**
     * Old alias for DEALER flag. Flag to specify a XREQ socket, receiving side must be a XREP.
     * 
     * @deprecated As of release 3.0 of zeromq, replaced by {@link #DEALER}
     */
    public static final int XREQ = DEALER;
    /**
     * Flag to specify ROUTER socket (aka XREP). ROUTER is the socket that creates and consumes request-reply routing
     * envelopes. It is the only socket type that lets you route messages to specific connections if you know their
     * identities.
     */
    public static final int ROUTER = 6;
    /**
     * Old alias for ROUTER flag. Flag to specify the receiving part of a XREQ socket.
     * 
     * @deprecated As of release 3.0 of zeromq, replaced by {@link #ROUTER}
     */
    public static final int XREP = ROUTER;
    /**
     * Flag to specify the receiving part of a PUSH socket.
     */
    public static final int PULL = 7;
    /**
     * Flag to specify a PUSH socket, receiving side must be a PULL.
     */
    public static final int PUSH = 8;
    /**
     * Flag to specify a XPUB socket, receiving side must be a SUB or XSUB. Subscriptions can be received as a message.
     * Subscriptions start with a '1' byte. Unsubscriptions start with a '0' byte.
     */
    public static final int XPUB = 9;
    /**
     * Flag to specify the receiving part of the PUB or XPUB socket. Allows
     */
    public static final int XSUB = 10;

    /**
     * Flag to specify a STREAMER device.
     */
    public static final int STREAMER = 1;

    /**
     * Flag to specify a FORWARDER device.
     */
    public static final int FORWARDER = 2;

    /**
     * Flag to specify a QUEUE device.
     */
    public static final int QUEUE = 3;

    /**
     * @see ZMQ#PULL
     */
    @Deprecated
    public static final int UPSTREAM = PULL;
    /**
     * @see ZMQ#PUSH
     */
    @Deprecated
    public static final int DOWNSTREAM = PUSH;
    
    /**
     * EVENT_CONNECTED: connection established.
     * The EVENT_CONNECTED event triggers when a connection has been 
     * established to a remote peer. This can happen either synchronous 
     * or asynchronous. Value is the FD of the newly connected socket.
     */
    public static final int EVENT_CONNECTED = 1;
    /**
     * EVENT_CONNECT_DELAYED: synchronous connect failed, it's being polled.
     * The EVENT_CONNECT_DELAYED event triggers when an immediate connection
     * attempt is delayed and its completion is being polled for. Value has
     * no meaning.
     */
    public static final int EVENT_CONNECT_DELAYED = 2;
    /**
     * EVENT_CONNECT_RETRIED: asynchronous connect / reconnection attempt.
     * The EVENT_CONNECT_RETRIED event triggers when a connection attempt is
     * being handled by reconnect timer. The reconnect interval's recomputed
     * for each attempt. Value is the reconnect interval.
     */
    public static final int EVENT_CONNECT_RETRIED = 4;

    /**
     * EVENT_LISTENING: socket bound to an address, ready to accept connections.
     * The EVENT_LISTENING event triggers when a socket's successfully bound to
     * a an interface. Value is the FD of the newly bound socket.
     */
    public static final int EVENT_LISTENING = 8;
    /**
     * EVENT_BIND_FAILED: socket could not bind to an address.
     * The EVENT_BIND_FAILED event triggers when a socket could not bind to a
     * given interface. Value is the errno generated by the bind call.
     */
    public static final int EVENT_BIND_FAILED = 16;

    /**
     * EVENT_ACCEPTED: connection accepted to bound interface.
     * The EVENT_ACCEPTED event triggers when a connection from a remote peer
     * has been established with a socket's listen address. Value is the FD of
     * the accepted socket.
     */
    public static final int EVENT_ACCEPTED = 32;
    /**
     * EVENT_ACCEPT_FAILED: could not accept client connection.
     * The EVENT_ACCEPT_FAILED event triggers when a connection attempt to a
     * socket's bound address fails. Value is the errno generated by accept.
     */
    public static final int EVENT_ACCEPT_FAILED = 64;

    /**
     * EVENT_CLOSED: connection closed.
     * The EVENT_CLOSED event triggers when a connection's underlying
     * descriptor has been closed. Value is the former FD of the for the 
     * closed socket. FD has been closed already!
     */
    public static final int EVENT_CLOSED = 128;
    /**
     * EVENT_CLOSE_FAILED: connection couldn't be closed.
     * The EVENT_CLOSE_FAILED event triggers when a descriptor could not be
     * released back to the OS. Implementation note: ONLY FOR IPC SOCKETS.
     * Value is the errno generated by unlink.
     */
    public static final int EVENT_CLOSE_FAILED = 256;
    /**
     * EVENT_DISCONNECTED: broken session.
     * The EVENT_DISCONNECTED event triggers when the stream engine (tcp and
     * ipc specific) detects a corrupted / broken session. Value is the FD of
     * the socket.
     */
    public static final int EVENT_DISCONNECTED = 512;
    /**
     * EVENT_MONITOR_STOPPED: monitor has been stopped.
     * The EVENT_MONITOR_STOPPED event triggers when the monitor for a socket is
     * stopped.
     */
    public static final int EVENT_MONITOR_STOPPED = 1024;

    /**
     * EVENT_ALL: all events known by the java binding.
     * The EVENT_ALL constant can be used to set up a monitor for all events known
     * by the java binding. One could add more flags 
     */
    public static final int EVENT_ALL = EVENT_CONNECTED | EVENT_CONNECT_DELAYED | EVENT_CONNECT_RETRIED |
            EVENT_LISTENING | EVENT_BIND_FAILED | EVENT_ACCEPTED | EVENT_ACCEPT_FAILED |
            EVENT_CLOSED | EVENT_CLOSE_FAILED | EVENT_DISCONNECTED | EVENT_MONITOR_STOPPED;

    /**
     * @return Major version number of the ZMQ library.
     */
    public static int getMajorVersion() {
        return version_major();
    }

    /**
     * @return Major version number of the ZMQ library.
     */
    public static int getMinorVersion() {
        return version_minor();
    }

    /**
     * @return Major version number of the ZMQ library.
     */
    public static int getPatchVersion() {
        return version_patch();
    }

    /**
     * @return Full version number of the ZMQ library used for comparing versions.
     */
    public static int getFullVersion() {
        return version_full();
    }

    /**
     * @param major Version major component.
     * @param minor Version minor component.
     * @param patch Version patch component.
     * 
     * @return Comparible single int version number.
     */
    public static int makeVersion(final int major, final int minor, final int patch) {
        return make_version(major, minor, patch);
    }

    /**
     * @return String version number in the form major.minor.patch.
     */
    public static String getVersionString() {
        return String.format("%d.%d.%d", version_major(), version_minor(), version_patch());
    }

    /**
     * Starts the built-in 0MQ proxy in the current application thread. The proxy connects a frontend socket to a
     * backend socket. Conceptually, data flows from frontend to backend. Depending on the socket types, replies may
     * flow in the opposite direction. The direction is conceptual only; the proxy is fully symmetric and there is no
     * technical difference between frontend and backend.
     * 
     * Before calling ZMQ.proxy() you must set any socket options, and connect or bind both frontend and backend
     * sockets. The two conventional proxy models are:
     * 
     * ZMQ.proxy() runs in the current thread and returns only if/when the current context is closed.
     * 
     * @param frontend ZMQ.Socket
     * @param backend ZMQ.Socket
     * @param capture If the capture socket is not NULL, the proxy shall send all messages, received on both frontend
     *            and backend, to the capture socket. The capture socket should be a ZMQ_PUB, ZMQ_DEALER, ZMQ_PUSH, or
     *            ZMQ_PAIR socket.
     * @since 3.2.2
     */
    public static void proxy(Socket frontend, Socket backend, Socket capture) {
        if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2))
            throw new UnsupportedOperationException();

        run_proxy(frontend, backend, capture);
    }

    /**
     * Poll on polling items until timeout
     * 
     * @param items polling items
     * @param timeout timeout in millisecond
     * @return number of events
     */
    public static int poll(PollItem[] items, long timeout) {

        return poll(items, items.length, timeout);
    }

    /**
     * Poll on polling items until timeout
     * 
     * @param items polling items
     * @param count active item count
     * @param timeout timeout in millisecond
     * @return number of events
     */
    public static int poll(PollItem[] items, int count, long timeout) {

        return Poller.run_poll(items, count, timeout);
    }

    protected static native int version_full();

    protected static native int version_major();

    protected static native int version_minor();

    protected static native int version_patch();

    protected static native int make_version(int major, int minor, int patch);

    protected static native long ENOTSUP();

    protected static native long EPROTONOSUPPORT();

    protected static native long ENOBUFS();

    protected static native long ENETDOWN();

    protected static native long EADDRINUSE();

    protected static native long EADDRNOTAVAIL();

    protected static native long ECONNREFUSED();

    protected static native long EINPROGRESS();

    protected static native long EHOSTUNREACH();

    protected static native long EMTHREAD();

    protected static native long EFSM();

    protected static native long ENOCOMPATPROTO();

    protected static native long ETERM();

    protected static native long ENOTSOCK();

    private static native void run_proxy(Socket frontend, Socket backend, Socket capture);

    /**
     * Inner class: Error.
     */
    public enum Error {

        ENOTSUP(ENOTSUP()),

        EPROTONOSUPPORT(EPROTONOSUPPORT()),

        ENOBUFS(ENOBUFS()),

        ENETDOWN(ENETDOWN()),

        EADDRINUSE(EADDRINUSE()),

        EADDRNOTAVAIL(EADDRNOTAVAIL()),

        ECONNREFUSED(ECONNREFUSED()),

        EINPROGRESS(EINPROGRESS()),

        EHOSTUNREACH(EHOSTUNREACH()),

        EMTHREAD(EMTHREAD()),

        EFSM(EFSM()),

        ENOCOMPATPROTO(ENOCOMPATPROTO()),

        ETERM(ETERM()),

        ENOTSOCK(ENOTSOCK());

        private final long code;

        Error(long code) {
            this.code = code;
        }

        public long getCode() {
            return code;
        }

        public static Error findByCode(int code) {
            for (Error e : Error.class.getEnumConstants()) {
                if (e.getCode() == code) {
                    return e;
                }
            }
            throw new IllegalArgumentException("Unknown " + Error.class.getName() + " enum code:" + code);
        }
    }

    /**
     * Create a new Context.
     * 
     * @param ioThreads Number of threads to use, usually 1 is sufficient for most use cases.
     * @return the Context
     */
    public static Context context(int ioThreads) {
        return new Context(ioThreads);
    }

    /**
     * Inner class: Context.
     */
    public static class Context implements Closeable {
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * This is an explicit "destructor". It can be called to ensure the corresponding 0MQ Context has been disposed
         * of.
         */
        public void term() {
            if(closed.compareAndSet(false, true)) {
                destroy();
            }
        }

        /**
         * Create a new Socket within this context.
         * 
         * @param type the socket type.
         * @return the newly created Socket.
         */
        public Socket socket(int type) {
            return new Socket(this, type);
        }

        /**
         * Create a new Poller within this context, with a default size.
         * 
         * @return the newly created Poller.
         * @deprecated use Poller constructor
         */
        public Poller poller() {
            return new Poller(this);
        }

        /**
         * Create a new Poller within this context, with a specified initial size.
         * 
         * @param size the poller initial size.
         * @return the newly created Poller.
         * @deprecated use Poller constructor
         */
        public Poller poller(int size) {
            return new Poller(this, size);
        }

        /**
         * Class constructor.
         * 
         * @param ioThreads size of the threads pool to handle I/O operations.
         */
        protected Context(int ioThreads) {
            construct(ioThreads);
        }

        /** Initialize the JNI interface */
        protected native void construct(int ioThreads);

        /** Free all resources used by JNI interface. */
        protected native void destroy();

        /**
         * Get the underlying context handle. This is private because it is only accessed from JNI, where Java access
         * controls are ignored.
         * 
         * @return the internal 0MQ context handle.
         */
        private long getContextHandle() {
            return this.contextHandle;
        }

        /** Opaque data used by JNI driver. */
        private long contextHandle;

        public void close() {
            term();
        }

        /**
         * Sets the maximum number of sockets allowed on the context
         */
        public native boolean setMaxSockets(int maxSockets);

        /**
         * The maximum number of sockets allowed on the context
         */
        public native int getMaxSockets();
    }

    /**
     * Inner class: Socket.
     */
    public static class Socket implements Closeable {
        private static native void nativeInit();

        static {
            if (!EmbeddedLibraryTools.LOADED_EMBEDDED_LIBRARY)
                System.loadLibrary("jzmq");
            nativeInit();
        }

        private final AtomicBoolean closed = new AtomicBoolean(false);
        /**
         * This is an explicit "destructor". It can be called to ensure the corresponding 0MQ Socket has been disposed
         * of.
         */
        public void close() {
            if(closed.compareAndSet(false, true)) {
                destroy();
            }
        }

        /**
         * The 'ZMQ_TYPE option shall retrieve the socket type for the specified 'socket'. The socket type is specified
         * at socket creation time and cannot be modified afterwards.
         * 
         * @return the socket type.
         * @since 2.1.0
         */
        public int getType() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 0))
                return -1;

            return (int) getLongSockopt(TYPE);
        }

        /**
         * @see #setLinger(long)
         * 
         * @return the linger period.
         * @since 2.1.0
         */
        public long getLinger() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 0))
                return -1;

            return getLongSockopt(LINGER);
        }

        /**
         * @see #setReconnectIVL(long)
         * 
         * @return the reconnectIVL.
         * @since 3.0.0
         */
        public long getReconnectIVL() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 10))
                return -1;

            return getLongSockopt(RECONNECT_IVL);
        }

        /**
         * @see #setBacklog(long)
         * 
         * @return the backlog.
         * @since 3.0.0
         */
        public long getBacklog() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(BACKLOG);
        }

        /**
         * @see #setReconnectIVLMax(long)
         * 
         * @return the reconnectIVLMax.
         * @since 3.0.0
         */
        public long getReconnectIVLMax() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 10))
                return -1;

            return getLongSockopt(RECONNECT_IVL_MAX);
        }

        /**
         * @see #setMaxMsgSize(long)
         * 
         * @return the maxMsgSize.
         * @since 3.0.0
         */
        public long getMaxMsgSize() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(MAXMSGSIZE);
        }

        /**
         * @see #setSndHWM(long)
         * 
         * @return the SndHWM.
         * @since 3.0.0
         */
        public long getSndHWM() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(SNDHWM);
        }

        /**
         * @see #setRcvHWM(long)
         * 
         * @return the recvHWM period.
         * @since 3.0.0
         */
        public long getRcvHWM() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(RCVHWM);
        }

        /**
         * @see #setHWM(long)
         * 
         * @return the High Water Mark.
         */
        public long getHWM() {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(HWM);
        }

        /**
         * @see #setSwap(long)
         * 
         * @return the number of messages to swap at most.
         */
        public long getSwap() {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0))
                return -1;

            return getLongSockopt(SWAP);
        }

        /**
         * @see #setAffinity(long)
         * 
         * @return the affinity.
         */
        public long getAffinity() {
            return getLongSockopt(AFFINITY);
        }

        /**
         * @see #setTCPKeepAlive(long)
         * 
         * @return the keep alive setting.
         */
        public long getTCPKeepAliveSetting() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 2, 0))
                return -1;

            return getLongSockopt(KEEPALIVE);
        }

        /**
         * @see #setTCPKeepAliveIdle(long)
         * 
         * @return the keep alive idle value.
         */
        public long getTCPKeepAliveIdle() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 2, 0))
                return -1;

            return getLongSockopt(KEEPALIVEIDLE);
        }

        /**
         * @see #setTCPKeepAliveInterval(long)
         * 
         * @return the keep alive interval.
         */
        public long getTCPKeepAliveInterval() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 2, 0))
                return -1;

            return getLongSockopt(KEEPALIVEINTVL);
        }

        /**
         * @see #setTCPKeepAliveCount(long)
         * 
         * @return the keep alive count.
         */
        public long getTCPKeepAliveCount() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 2, 0))
                return -1;

            return getLongSockopt(KEEPALIVECNT);
        }

        /**
         * @see #setIdentity(byte[])
         * 
         * @return the Identitiy.
         */
        public byte[] getIdentity() {
            return getBytesSockopt(IDENTITY);
        }

        /**
         * @see #setRate(long)
         * 
         * @return the Rate.
         */
        public long getRate() {
            return getLongSockopt(RATE);
        }

        /**
         * @see #setRecoveryInterval(long)
         * 
         * @return the RecoveryIntervall.
         */
        public long getRecoveryInterval() {
            return getLongSockopt(RECOVERY_IVL);
        }

        /**
         * @see #setMulticastLoop(boolean)
         * 
         * @return the Multicast Loop.
         */
        public boolean hasMulticastLoop() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return false;

            return getLongSockopt(MCAST_LOOP) != 0;
        }

        /**
         * Sets the time-to-live field in every multicast packet sent from this socket. The default is 1 which means
         * that the multicast packets don't leave the local network.
         * 
         * @param mcast_hops
         */
        public void setMulticastHops(long mcast_hops) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(MULTICAST_HOPS, mcast_hops);
        }

        /**
         * @see #setMulticastHops(long)
         * 
         * @return the Multicast Hops.
         */
        public long getMulticastHops() {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return 1;
            return getLongSockopt(MULTICAST_HOPS);
        }

        /**
         * Sets the timeout for receive operation on the socket. If the value is 0, recv will return immediately, with a
         * EAGAIN error if there is no message to receive. If the value is -1, it will block until a message is
         * available. For all other values, it will wait for a message for that amount of time before returning with an
         * EAGAIN error.
         * 
         * @param timeout Timeout for receive operation in milliseconds. Default -1 (infinite)
         */
        public void setReceiveTimeOut(int timeout) {
            if (ZMQ.version_full() < ZMQ.make_version(2, 2, 0))
                return;

            setLongSockopt(RCVTIMEO, timeout);
        }

        /**
         * @see #setReceiveTimeOut(int)
         * 
         * @return the Receive Timeout in milliseconds
         */
        public int getReceiveTimeOut() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 2, 0))
                return -1;
            return (int) getLongSockopt(RCVTIMEO);
        }

        /**
         * Sets the timeout for send operation on the socket. If the value is 0, send will return immediately, with a
         * EAGAIN error if the message cannot be sent. If the value is -1, it will block until the message is sent. For
         * all other values, it will try to send the message for that amount of time before returning with an EAGAIN
         * error.
         * 
         * @param timeout Timeout for send operation in milliseconds. Default -1 (infinite)
         */
        public void setSendTimeOut(int timeout) {
            if (ZMQ.version_full() < ZMQ.make_version(2, 2, 0))
                return;

            setLongSockopt(SNDTIMEO, timeout);
        }

        /**
         * @see #setSendTimeOut(int)
         * 
         * @return the Send Timeout. in milliseconds
         */
        public int getSendTimeOut() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 2, 0))
                return -1;
            return (int) getLongSockopt(SNDTIMEO);
        }

        /**
         * @see #setSendBufferSize(long)
         * 
         * @return the kernel send buffer size.
         */
        public long getSendBufferSize() {
            return getLongSockopt(SNDBUF);
        }

        /**
         * @see #setReceiveBufferSize(long)
         * 
         * @return the kernel receive buffer size.
         */
        public long getReceiveBufferSize() {
            return getLongSockopt(RCVBUF);
        }

        /**
         * @see #setIPv4Only(boolean)
         *
         * @return the IPv4 only socket.
         */
        public boolean getIPv4Only() {
            return getLongSockopt(IPV4ONLY) == 1;
        }

        /**
         * @see #setPlainServer(boolean)
         *
         * @return if the socket is setup for PLAIN security
         */
        public boolean getPlainServer() {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                return getLongSockopt(PLAIN_SERVER) == 1;
            }

            return false;
        }

        /**
         * @see #setPlainUsername(byte[])
         *
         * @return null terminated byte array in server charset
         */
        public byte[] getPlainUsername() {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                return getBytesSockopt(PLAIN_USERNAME);
            }

            return null;
        }

        /**
         * @see #setPlainPassword(byte[])
         *
         * @return null terminated byte array in server charset
         */
        public byte[] getPlainPassword() {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                return getBytesSockopt(PLAIN_PASSWORD);
            }

            return null;
        }

        /**
         * The 'ZMQ_RCVMORE' option shall return a boolean value indicating if the multi-part message currently being
         * read from the specified 'socket' has more message parts to follow. If there are no message parts to follow or
         * if the message currently being read is not a multi-part message a value of zero shall be returned. Otherwise,
         * a value of 1 shall be returned.
         * 
         * @return true if there are more messages to receive.
         */
        public boolean hasReceiveMore() {
            return getLongSockopt(RCVMORE) != 0;
        }

        /**
         * The 'ZMQ_FD' option shall retrieve file descriptor associated with the 0MQ socket. The descriptor can be used
         * to integrate 0MQ socket into an existing event loop. It should never be used for anything else than polling
         * -- such as reading or writing. The descriptor signals edge-triggered IN event when something has happened
         * within the 0MQ socket. It does not necessarily mean that the messages can be read or written. Check
         * ZMQ_EVENTS option to find out whether the 0MQ socket is readable or writeable.
         * 
         * @return the underlying file descriptor.
         * @since 2.1.0
         */
        public long getFD() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 0))
                return -1;

            return getLongSockopt(FD);
        }

        /**
         * The 'ZMQ_EVENTS' option shall retrieve event flags for the specified socket. If a message can be read from
         * the socket ZMQ_POLLIN flag is set. If message can be written to the socket ZMQ_POLLOUT flag is set.
         * 
         * @return the mask of outstanding events.
         * @since 2.1.0
         */
        public long getEvents() {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 0))
                return -1;

            return getLongSockopt(EVENTS);
        }

        /**
         * The 'ZMQ_LINGER' option shall retrieve the period for pending outbound messages to linger in memory after
         * closing the socket. Value of -1 means infinite. Pending messages will be kept until they are fully
         * transferred to the peer. Value of 0 means that all the pending messages are dropped immediately when socket
         * is closed. Positive value means number of milliseconds to keep trying to send the pending messages before
         * discarding them.
         * 
         * @param linger the linger period.
         * @since 2.1.0
         */
        public void setLinger(long linger) {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 0))
                return;

            setLongSockopt(LINGER, linger);
        }

        /**
         * @since 3.0.0
         */
        public void setReconnectIVL(long reconnectIVL) {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 10))
                return;

            setLongSockopt(RECONNECT_IVL, reconnectIVL);
        }

        /**
         * @since 3.0.0
         */
        public void setBacklog(long backlog) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(BACKLOG, backlog);
        }

        /**
         * @since 3.0.0
         */
        public void setReconnectIVLMax(long reconnectIVLMax) {
            if (ZMQ.version_full() < ZMQ.make_version(2, 1, 10))
                return;

            setLongSockopt(RECONNECT_IVL_MAX, reconnectIVLMax);
        }

        /**
         * @since 3.0.0
         */
        public void setMaxMsgSize(long maxMsgSize) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(MAXMSGSIZE, maxMsgSize);
        }

        /**
         * @since 3.0.0
         */
        public void setSndHWM(long sndHWM) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(SNDHWM, sndHWM);
        }

        /**
         * @since 3.0.0
         */
        public void setRcvHWM(long rcvHWM) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(RCVHWM, rcvHWM);
        }

        /**
         * The 'ZMQ_HWM' option shall set the high water mark for the specified 'socket'. The high water mark is a hard
         * limit on the maximum number of outstanding messages 0MQ shall queue in memory for any single peer that the
         * specified 'socket' is communicating with.
         * 
         * If this limit has been reached the socket shall enter an exceptional state and depending on the socket type,
         * 0MQ shall take appropriate action such as blocking or dropping sent messages. Refer to the individual socket
         * descriptions in the man page of zmq_socket[3] for details on the exact action taken for each socket type.
         * 
         * @param hwm the number of messages to queue.
         */
        public void setHWM(long hwm) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(HWM, hwm);
        }

        /**
         * Get the Swap. The 'ZMQ_SWAP' option shall set the disk offload (swap) size for the specified 'socket'. A
         * socket which has 'ZMQ_SWAP' set to a non-zero value may exceed its high water mark; in this case outstanding
         * messages shall be offloaded to storage on disk rather than held in memory.
         * 
         * @param swap The value of 'ZMQ_SWAP' defines the maximum size of the swap space in bytes.
         */
        public void setSwap(long swap) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(SWAP, swap);
        }

        /**
         * Get the Affinity. The 'ZMQ_AFFINITY' option shall set the I/O thread affinity for newly created connections
         * on the specified 'socket'.
         * 
         * Affinity determines which threads from the 0MQ I/O thread pool associated with the socket's _context_ shall
         * handle newly created connections. A value of zero specifies no affinity, meaning that work shall be
         * distributed fairly among all 0MQ I/O threads in the thread pool. For non-zero values, the lowest bit
         * corresponds to thread 1, second lowest bit to thread 2 and so on. For example, a value of 3 specifies that
         * subsequent connections on 'socket' shall be handled exclusively by I/O threads 1 and 2.
         * 
         * See also in the man page of zmq_init[3] for details on allocating the number of I/O threads for a specific
         * _context_.
         * 
         * @param affinity the affinity.
         */
        public void setAffinity(long affinity) {
            setLongSockopt(AFFINITY, affinity);
        }

        /**
         * Override SO_KEEPALIVE socket option (where supported by OS) to enable keep-alive packets for a socket
         * connection. Possible values are -1, 0, 1. The default value -1 will skip all overrides and do the OS default.
         * 
         * @param optVal The value of 'ZMQ_TCP_KEEPALIVE' to turn TCP keepalives on (1) or off (0).
         */
        public void setTCPKeepAlive(long optVal) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0))
                setLongSockopt(KEEPALIVE, optVal);
        }

        /**
         * Override TCP_KEEPCNT socket option (where supported by OS). The default value -1 will skip all overrides and
         * do the OS default.
         * 
         * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_CNT' defines the number of keepalives before death.
         */
        public void setTCPKeepAliveCount(long optVal) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0))
                setLongSockopt(KEEPALIVECNT, optVal);
        }

        /**
         * Override TCP_KEEPINTVL socket option (where supported by OS). The default value -1 will skip all overrides
         * and do the OS default.
         * 
         * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_INTVL' defines the interval between keepalives. Unit is OS
         *            dependant.
         */
        public void setTCPKeepAliveInterval(long optVal) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0))
                setLongSockopt(KEEPALIVEINTVL, optVal);
        }

        /**
         * Override TCP_KEEPCNT (or TCP_KEEPALIVE on some OS) socket option (where supported by OS). The default value
         * -1 will skip all overrides and do the OS default.
         * 
         * @param optVal The value of 'ZMQ_TCP_KEEPALIVE_IDLE' defines the interval between the last data packet sent
         *            over the socket and the first keepalive probe. Unit is OS dependant.
         */
        public void setTCPKeepAliveIdle(long optVal) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0))
                setLongSockopt(KEEPALIVEIDLE, optVal);
        }

        /**
         * The 'ZMQ_IDENTITY' option shall set the identity of the specified 'socket'. Socket identity determines if
         * existing 0MQ infastructure (_message queues_, _forwarding devices_) shall be identified with a specific
         * application and persist across multiple runs of the application.
         * 
         * If the socket has no identity, each run of an application is completely separate from other runs. However,
         * with identity set the socket shall re-use any existing 0MQ infrastructure configured by the previous run(s).
         * Thus the application may receive messages that were sent in the meantime, _message queue_ limits shall be
         * shared with previous run(s) and so on.
         * 
         * Identity should be at least one byte and at most 255 bytes long. Identities starting with binary zero are
         * reserved for use by 0MQ infrastructure.
         * 
         * @param identity
         */
        public void setIdentity(byte[] identity) {
            setBytesSockopt(IDENTITY, identity);
        }

        /**
         * The 'ZMQ_SUBSCRIBE' option shall establish a new message filter on a 'ZMQ_SUB' socket. Newly created
         * 'ZMQ_SUB' sockets shall filter out all incoming messages, therefore you should call this option to establish
         * an initial message filter.
         * 
         * An empty 'option_value' of length zero shall subscribe to all incoming messages. A non-empty 'option_value'
         * shall subscribe to all messages beginning with the specified prefix. Mutiple filters may be attached to a
         * single 'ZMQ_SUB' socket, in which case a message shall be accepted if it matches at least one filter.
         * 
         * @param topic
         */
        public void subscribe(byte[] topic) {
            setBytesSockopt(SUBSCRIBE, topic);
        }

        /**
         * The 'ZMQ_UNSUBSCRIBE' option shall remove an existing message filter on a 'ZMQ_SUB' socket. The filter
         * specified must match an existing filter previously established with the 'ZMQ_SUBSCRIBE' option. If the socket
         * has several instances of the same filter attached the 'ZMQ_UNSUBSCRIBE' option shall remove only one
         * instance, leaving the rest in place and functional.
         * 
         * @param topic
         */
        public void unsubscribe(byte[] topic) {
            setBytesSockopt(UNSUBSCRIBE, topic);
        }

        /**
         * The 'ZMQ_RATE' option shall set the maximum send or receive data rate for multicast transports such as in the
         * man page of zmq_pgm[7] using the specified 'socket'.
         * 
         * @param rate
         */
        public void setRate(long rate) {
            setLongSockopt(RATE, rate);
        }

        /**
         * The 'ZMQ_RECOVERY_IVL' option shall set the recovery interval for multicast transports using the specified
         * 'socket'. The recovery interval determines the maximum time in seconds (before version 3.0.0) or milliseconds
         * (version 3.0.0 and after) that a receiver can be absent from a multicast group before unrecoverable data loss
         * will occur.
         * 
         * CAUTION: Exercise care when setting large recovery intervals as the data needed for recovery will be held in
         * memory. For example, a 1 minute recovery interval at a data rate of 1Gbps requires a 7GB in-memory buffer.
         * {Purpose of this Method}
         * 
         * @param recovery_ivl
         */
        public void setRecoveryInterval(long recovery_ivl) {
            setLongSockopt(RECOVERY_IVL, recovery_ivl);
        }

        /**
         * The 'ZMQ_MCAST_LOOP' option shall control whether data sent via multicast transports using the specified
         * 'socket' can also be received by the sending host via loopback. A value of zero disables the loopback
         * functionality, while the default value of 1 enables the loopback functionality. Leaving multicast loopback
         * enabled when it is not required can have a negative impact on performance. Where possible, disable
         * 'ZMQ_MCAST_LOOP' in production environments.
         * 
         * @param mcast_loop
         */
        public void setMulticastLoop(boolean mcast_loop) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 0, 0))
                return;

            setLongSockopt(MCAST_LOOP, mcast_loop ? 1 : 0);
        }

        /**
         * The 'ZMQ_SNDBUF' option shall set the underlying kernel transmit buffer size for the 'socket' to the
         * specified size in bytes. A value of zero means leave the OS default unchanged. For details please refer to
         * your operating system documentation for the 'SO_SNDBUF' socket option.
         * 
         * @param sndbuf
         */
        public void setSendBufferSize(long sndbuf) {
            setLongSockopt(SNDBUF, sndbuf);
        }

        /**
         * The 'ZMQ_RCVBUF' option shall set the underlying kernel receive buffer size for the 'socket' to the specified
         * size in bytes. A value of zero means leave the OS default unchanged. For details refer to your operating
         * system documentation for the 'SO_RCVBUF' socket option.
         * 
         * @param rcvbuf
         */
        public void setReceiveBufferSize(long rcvbuf) {
            setLongSockopt(RCVBUF, rcvbuf);
        }

        /**
         * The 'ZMQ_IPV4ONLY' option shall set the underlying native socket type. An IPv6 socket lets applications
         * connect to and accept connections from both IPv4 and IPv6 hosts.
         * 
         * @param v4only A value of true will use IPv4 sockets, while the value of false will use IPv6 sockets
         */
        public void setIPv4Only(boolean v4only) {
            setLongSockopt(IPV4ONLY, v4only ? 1L : 0L);
        }

        /**
         * Sets the ROUTER socket behavior when an unroutable message is encountered.
         * 
         * @param mandatory A value of false is the default and discards the message silently when it cannot be routed.
         *            A value of true returns an EHOSTUNREACH error code if the message cannot be routed.
         */
        public void setRouterMandatory(boolean mandatory) {
            setLongSockopt(ROUTER_MANDATORY, mandatory ? 1L : 0L);
        }

        /**
         * Sets the XPUB socket behavior on new subscriptions and unsubscriptions.
         *
         * @param verbose A value of false is the default and passes only new subscription messages to upstream.
         *            A value of true passes all subscription messages upstream.
         * @since 3.2.2
         */
        public void setXpubVerbose(boolean verbose) {
            if (ZMQ.version_full() < ZMQ.make_version(3, 2, 2))
                return;
              
            setLongSockopt(XPUB_VERBOSE, verbose ? 1L : 0L);
        }

        /**
         * Sets if the socket is for a server using the PLAIN security mechanism.
         * @see <a href="http://rfc.zeromq.org/spec:24">PLAIN RFC</a>
         * @param plain whether or not to use PLAIN security
         * @since 4.0.0
         */
        public void setPlainServer(boolean plain) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setLongSockopt(PLAIN_SERVER, plain ? 1L : 0L);
            }
        }

        /**
         * Sets the username used for the PLAIN security mechanism.
         * @see <a href="http://rfc.zeromq.org/spec:24">PLAIN RFC</a>
         * @param username null terminated string in server charset
         * @since 4.0.0
         */
        public void setPlainUsername(byte[] username) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setBytesSockopt(PLAIN_USERNAME, username);
            }
        }

        /**
         * Sets the password used for the PLAIN security mechanism.
         * @see <a href="http://rfc.zeromq.org/spec:24">PLAIN RFC</a>
         * @param password null terminated string in server charset
         * @since 4.0.0
         */
        public void setPlainPassword(byte[] password) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setBytesSockopt(PLAIN_PASSWORD, password);
            }
        }
        
        /**
         * Sets the domain for ZAP (ZMQ RFC 27) authentication.
         * @param domain  For NULL security (the default on all tcp:// connections), 
         * ZAP authentication only happens if you set a non-empty domain. For PLAIN and CURVE security, 
         * ZAP requests are always made, if there is a ZAP handler present. 
         * See http://rfc.zeromq.org/spec:27 for more details.
         */
        public void setZAPDomain(byte[] domain) {
            if(ZMQ.version_full() >= ZMQ.make_version(4, 1, 0)) {
                setBytesSockopt(ZAP_DOMAIN, domain);
            }
        }
        
        public void setGSSAPIServer(boolean isServer) {
            if(ZMQ.version_full() >= ZMQ.makeVersion(4, 1, 0)) {
                setLongSockopt(GSSAPI_SERVER, isServer ? 1L : 0L);
            }   
        }

        public void setGSSAPIPrincipal(byte[] principal) {
            if(ZMQ.version_full() >= ZMQ.make_version(4, 1, 0)) {
                setBytesSockopt(GSSAPI_PRINCIPAL, principal);
            }
        }

        public void setGSSAPIServicePrincipal(byte[] principal) {
            if(ZMQ.version_full() >= ZMQ.make_version(4, 1, 0)) {
                setBytesSockopt(GSSAPI_SERVICE_PRINCIPAL, principal);
            }
        }

        /**
         * Sets whether socket should keep only last received/to be sent message in its inbound/outbound queue. 
         *
         * @param conflate A value of false is the default which means socket preserves all messages with respect
         *            to the RECVHWM and SENDHWM options. A value of true means only last message is kept ignoring
         *            the RECVHWM and SENDHWM options.
         * @since 4.0.0
         */
        public void setConflate(boolean conflate) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setLongSockopt(CONFLATE, conflate ? 1L : 0L);
            }
        }

        /**
         * Indicate whether socket should keep only last received/to be sent message in its inbound/outbound queue.
         *
         * @return true if should keep only last received/to be sent message in its inbound/outbound queue.
         * @since 4.0.0
         */
        public boolean getConflate() {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                return getLongSockopt(CONFLATE) != 0L;
            }
            else {
                return false;
            }
        }

        /**
         * Indicate whether socket should only queue messages to completed connections.
         *
         * @return true if should only queue messages to completed connections.
         * @since 3.2.0
         */
        public boolean getImmediate() {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0)) {
                return getLongSockopt(IMMEDIATE) != 0L;
            }
            else {
                return false;
            }
        }

        /**
         * Sets whether socket should only queue messages to completed connections.
         *
         * @param immediate A value of false is the default which means socket will not queue messages to
         *            to incomplete connections. This will cause the socket to block if there are no other connections,
         *            but will prevent queues from filling on pipes awaiting connection.
         * @since 3.2.0
         */
        public void setImmediate(boolean immediate) {
            if (ZMQ.version_full() >= ZMQ.make_version(3, 2, 0)) {
                setLongSockopt(IMMEDIATE, immediate ? 1L : 0L);
            }
        }

        public void setReqRelaxed(boolean isRelaxed) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setLongSockopt(REQ_RELAXED, isRelaxed ? 1L : 0L);
            }
        }

        public void setReqCorrelate(boolean isCorrelate) {
            if (ZMQ.version_full() >= ZMQ.make_version(4, 0, 0)) {
                setLongSockopt(REQ_CORRELATE, isCorrelate ? 1L : 0L);
            }
        }

        /**
         * Bind to network interface. Start listening for new connections.
         * 
         * @param addr the endpoint to bind to.
         */
        public native void bind(String addr);

        /**
         * Bind to network interface to a random port. Start listening for new connections.
         * 
         * @param addr the endpoint to bind to.
         */
        public int bindToRandomPort(String addr) {
            return bindToRandomPort(addr, 2000, 20000, 100);
        }

        /**
         * Bind to network interface to a random port. Start listening for new connections.
         * 
         * @param addr the endpoint to bind to.
         * @param min_port The minimum port in the range of ports to try.
         */
        public int bindToRandomPort(String addr, int min_port) {
            return bindToRandomPort(addr, min_port, 20000, 100);
        }

        /**
         * Bind to network interface to a random port. Start listening for new connections.
         * 
         * @param addr the endpoint to bind to.
         * @param min_port The minimum port in the range of ports to try.
         * @param max_port The maximum port in the range of ports to try.
         */
        public int bindToRandomPort(String addr, int min_port, int max_port) {
            return bindToRandomPort(addr, min_port, max_port, 100);
        }

        /**
         * Bind to network interface to a random port. Start listening for new connections.
         * 
         * @param addr the endpoint to bind to.
         * @param min_port The minimum port in the range of ports to try.
         * @param max_port The maximum port in the range of ports to try.
         * @param max_tries The number of attempt to bind.
         */
        public int bindToRandomPort(String addr, int min_port, int max_port, int max_tries) {
            int port;
            Random rand = new Random();
            for (int i = 0; i < max_tries; i++) {
                port = rand.nextInt(max_port - min_port + 1) + min_port;
                try {
                    bind(String.format("%s:%s", addr, port));
                    return port;
                } catch (ZMQException e) {
                    if (e.getErrorCode() != ZMQ.EADDRINUSE()) {
                        throw e;
                    }
                    continue;
                }
            }
            throw new ZMQException("Could not bind socket to random port.", (int) ZMQ.EADDRINUSE());
        }

        /**
         * Unbind from network interface. Stop listening for connections.
         * 
         * @param addr the endpoint to unbind from.
         */
        public native void unbind(String addr);

        /**
         * Connect to remote application.
         * 
         * @param addr the endpoint to connect to.
         */
        public native void connect(String addr);

        /**
         * Disconnect from a remote application.
         * 
         * @param addr the endpoint to disconnect from.
         */
        public native void disconnect(String addr);
        
        /**
         * Start a monitoring socket where events can be received.
         * 
         * @param addr the endpoint to receive events from. (must be inproc transport)
         * @param events the events of interest.
         * @return true if monitor socket setup is successful
         * @throws ZMQException
         */
        public native boolean monitor(String addr, int events) throws ZMQException;

        /**
         * Send a message.
         * 
         * @param msg the message to send, as an array of bytes.
         * @param offset the offset of the message to send.
         * @param flags the flags to apply to the send operation.
         * @return true if send was successful, false otherwise.
         */
        public boolean send(byte[] msg, int offset, int flags) {
            return send(msg, offset, msg.length, flags);
        }

        /**
         * 
         * @param msg
         * @param offset
         * @param len
         * @param flags
         * @return
         */
        public native boolean send(byte[] msg, int offset, int len, int flags);

        /**
         * Perform a zero copy send. The buffer must be allocated using ByteBuffer.allocateDirect
         * 
         * @param buffer
         * @param len
         * @param flags
         * @return
         */
        public native boolean sendZeroCopy(ByteBuffer buffer, int len, int flags);

        /**
         * Send a message.
         * 
         * @param msg the message to send, as an array of bytes.
         * @param flags the flags to apply to the send operation.
         * @return true if send was successful, false otherwise.
         */
        public boolean send(byte[] msg, int flags) {
            return send(msg, 0, msg.length, flags);
        }

        /**
         * Send a String.
         * 
         * @param msg the message to send, as a String.
         * @return true if send was successful, false otherwise.
         */

        public boolean send(String msg) {
            byte[] b = msg.getBytes();
            return send(b, 0, b.length, 0);
        }

        /**
         * Send a String.
         * 
         * @param msg the message to send, as a String.
         * @return true if send was successful, false otherwise.
         */

        public boolean sendMore(String msg) {
            byte[] b = msg.getBytes();
            return send(b, 0, b.length, SNDMORE);
        }

        /**
         * Send a String.
         * 
         * @param msg the message to send, as a String.
         * @param flags the flags to apply to the send operation.
         * @return true if send was successful, false otherwise.
         */

        public boolean send(String msg, int flags) {
            byte[] b = msg.getBytes();
            return send(b, 0, b.length, flags);
        }

        /**
         * Send a message
         *
         * @param bb ByteBuffer payload
         * @param flags the flags to apply to the send operation
         * @return the number of bytes sent
         */
        public native int sendByteBuffer(ByteBuffer bb, int flags);

        /**
         * Receive a message.
         * 
         * @param flags the flags to apply to the receive operation.
         * @return the message received, as an array of bytes; null on error.
         */
        public native byte[] recv(int flags);

        /**
         * Receive a message in to a specified buffer.
         * 
         * @param buffer byte[] to copy zmq message payload in to.
         * @param offset offset in buffer to write data
         * @param len max bytes to write to buffer. If len is smaller than the incoming message size, the message will
         *            be truncated.
         * @param flags the flags to apply to the receive operation.
         * @return the number of bytes read, -1 on error
         */
        public native int recv(byte[] buffer, int offset, int len, int flags);

        /**
         * Zero copy recv
         * 
         * @param buffer
         * @param len
         * @param flags
         * @return bytes read, -1 on error
         */
        public native int recvZeroCopy(ByteBuffer buffer, int len, int flags);

        /**
         * Receive a message.
         * 
         * @return the message received, as an array of bytes; null on error.
         */
        public final byte[] recv() {
            return recv(0);
        }

        /**
         * Receive a message as a String with the default Charset.
         *
         * @deprecated use {@link #recvStr(Charset)} instead.
         * @return the message received, as a String; null on error.
         */
        @Deprecated
        public String recvStr() {
            return recvStr(0);
        }

        /**
         * Receive a message as a String with a given Charset.
         *
         * @param charset the charset of the resulting string.
         * @return the message received, as a String; null on error.
         */
        public String recvStr(Charset charset) {
            return recvStr(0, charset);
        }

        /**
         * Receive a message as a String with the default charset.
         *
         * @deprecated use {@link #recvStr(int, Charset)} instead.
         * @param flags the flags to apply to the receive operation.
         * @return the message received, as a String; null on error.
         */
        @Deprecated
        public String recvStr(int flags) {
            return recvStr(flags, Charset.defaultCharset());
        }

        /**
         * Receive a message as a String.
         *
         * @param flags the flags to apply to the receive operation.
         * @param charset the charset of the resulting string.
         * @return the message received, as a String; null on error.
         */
        public String recvStr(int flags, Charset charset) {
            byte[] data = recv(flags);

            if (data == null) {
                return null;
            } else {
                return new String(data, charset);
            }
        }
        /**
         * Receive a message
         *
         * @param buffer
         * @param flags
         * @return bytes read, -1 on error
         */
        public native int recvByteBuffer(ByteBuffer buffer, int flags);

        /**
         * Class constructor.
         * 
         * @param context a 0MQ context previously created.
         * @param type the socket type.
         */
        protected Socket(Context context, int type) {
            // We keep a local handle to context so that
            // garbage collection won't be too greedy on it.
            this.context = context;
            construct(context, type);
        }

        /** Initialize the JNI interface */
        protected native void construct(Context ctx, int type);

        /** Free all resources used by JNI interface. */
        protected native void destroy();

        /**
         * Get the socket option value, as a long.
         * 
         * @param option ID of the option to set.
         * @return The socket option value (as a long).
         */
        protected native long getLongSockopt(int option);

        /**
         * Get the socket option value, as a byte array.
         * 
         * @param option ID of the option to set.
         * @return The socket option value (as a byte array).
         */
        protected native byte[] getBytesSockopt(int option);

        /**
         * Set the socket option value, given as a long.
         * 
         * @param option ID of the option to set.
         * @param optval value (as a long) to set the option to.
         */
        protected native void setLongSockopt(int option, long optval);

        /**
         * Set the socket option value, given as a byte array.
         * 
         * @param option ID of the option to set.
         * @param optval value (as a byte array) to set the option to.
         */
        protected native void setBytesSockopt(int option, byte[] optval);

        /**
         * Get the underlying socket handle. This is private because it is only accessed from JNI, where Java access
         * controls are ignored.
         * 
         * @return the internal 0MQ socket handle.
         */
        private long getSocketHandle() {
            return this.socketHandle;
        }
        
        /** Opaque data used by JNI driver. */
        private long socketHandle;
        private final Context context;
        // private Constants use the appropriate setter instead.
        private static final int HWM = 1;
        // public static final int LWM = 2; // No longer supported
        private static final int SWAP = 3;
        private static final int AFFINITY = 4;
        private static final int IDENTITY = 5;
        private static final int SUBSCRIBE = 6;
        private static final int UNSUBSCRIBE = 7;
        private static final int RATE = 8;
        private static final int RECOVERY_IVL = 9;
        private static final int MCAST_LOOP = 10;
        private static final int SNDBUF = 11;
        private static final int RCVBUF = 12;
        private static final int RCVMORE = 13;
        private static final int FD = 14;
        private static final int EVENTS = 15;
        private static final int TYPE = 16;
        private static final int LINGER = 17;
        private static final int RECONNECT_IVL = 18;
        private static final int BACKLOG = 19;
        private static final int RECONNECT_IVL_MAX = 21;
        private static final int MAXMSGSIZE = 22;
        private static final int SNDHWM = 23;
        private static final int RCVHWM = 24;
        private static final int MULTICAST_HOPS = 25;
        private static final int RCVTIMEO = 27;
        private static final int SNDTIMEO = 28;
        private static final int IPV4ONLY = 31;
        private static final int ROUTER_MANDATORY = 33;
        private static final int KEEPALIVE = 34;
        private static final int KEEPALIVECNT = 35;
        private static final int KEEPALIVEIDLE = 36;
        private static final int KEEPALIVEINTVL = 37;
        private static final int IMMEDIATE = 39;
        private static final int XPUB_VERBOSE = 40;
        private static final int PLAIN_SERVER = 44;
        private static final int PLAIN_USERNAME = 45;
        private static final int PLAIN_PASSWORD = 46;
        private static final int REQ_CORRELATE = 52;
        private static final int REQ_RELAXED = 53;
        private static final int CONFLATE = 54;
        private static final int ZAP_DOMAIN = 55;
        private static final int GSSAPI_SERVER = 62;
        private static final int GSSAPI_PRINCIPAL = 63;
        private static final int GSSAPI_SERVICE_PRINCIPAL = 64;

    }

    public static class PollItem {
        private Socket socket;
        private SelectableChannel channel;
        private int events;
        private int revents;

        public PollItem(Socket socket, int events) {
            this.socket = socket;
            this.events = events;
            this.revents = 0;
        }

        public PollItem(SelectableChannel channel, int events) {
            this.channel = channel;
            this.events = events;
            this.revents = 0;
        }

        public SelectableChannel getRawSocket() {
            return channel;
        }

        public Socket getSocket() {
            return socket;
        }

        public boolean isError() {
            return (revents & Poller.POLLERR) > 0;
        }

        public int readyOps() {
            return revents;
        }

        public boolean isReadable() {
            return (revents & Poller.POLLIN) > 0;
        }

        public boolean isWritable() {
            return (revents & Poller.POLLOUT) > 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PollItem))
                return false;

            PollItem target = (PollItem) obj;
            if (socket != null && socket == target.socket)
                return true;

            if (channel != null && channel == target.channel)
                return true;

            return false;
        }
    }

    /**
     * Inner class: Poller.
     */
    public static class Poller {
        /**
         * These values can be ORed to specify what we want to poll for.
         */
        public static final int POLLIN = 1;
        public static final int POLLOUT = 2;
        public static final int POLLERR = 4;

        /**
         * Register a Socket for polling on all events.
         * 
         * @param socket the Socket we are registering.
         * @return the index identifying this Socket in the poll set.
         */
        public int register(Socket socket) {
            return register(socket, POLLIN | POLLOUT | POLLERR);
        }

        /**
         * Register a Channel for polling on all events.
         * 
         * @param channel the Channel we are registering.
         * @return the index identifying this Channel in the poll set.
         */
        public int register(SelectableChannel channel) {
            return register(channel, POLLIN | POLLOUT | POLLERR);
        }

        /**
         * Register a Socket for polling on the specified events.
         * 
         * Automatically grow the internal representation if needed.
         * 
         * @param socket the Socket we are registering.
         * @param events a mask composed by XORing POLLIN, POLLOUT and POLLERR.
         * @return the index identifying this Socket in the poll set.
         */
        public int register(Socket socket, int events) {
            return registerInternal(new PollItem(socket, events));
        }

        /**
         * Register a Channel for polling on the specified events.
         * 
         * Automatically grow the internal representation if needed.
         * 
         * @param channel the Channel we are registering.
         * @param events a mask composed by XORing POLLIN, POLLOUT and POLLERR.
         * @return the index identifying this Channel in the poll set.
         */
        public int register(SelectableChannel channel, int events) {
            return registerInternal(new PollItem(channel, events));
        }

        /**
         * Register a Channel for polling on the specified events.
         * 
         * Automatically grow the internal representation if needed.
         * 
         * @param item the PollItem we are registering.
         * @return the index identifying this Channel in the poll set.
         */
        public int register(PollItem item) {
            return registerInternal(item);
        }

        /**
         * Register a Socket for polling on the specified events.
         * 
         * Automatically grow the internal representation if needed.
         * 
         * @param item the PollItem we are registering.
         * @return the index identifying this Socket in the poll set.
         */
        private int registerInternal(PollItem item) {
            int pos = -1;

            if (!this.freeSlots.isEmpty()) {
                // If there are free slots in our array, remove one
                // from the free list and use it.
                pos = this.freeSlots.remove();
            } else {
                if (this.next >= this.size) {
                    // It is necessary to grow the arrays.

                    // Compute new size for internal arrays.
                    int nsize = this.size + SIZE_INCREMENT;

                    // Create new internal arrays.
                    PollItem[] ns = new PollItem[nsize];
                    short[] ne = new short[nsize];
                    short[] nr = new short[nsize];

                    // Copy contents of current arrays into new arrays.
                    for (int i = 0; i < this.next; ++i) {
                        ns[i] = this.items[i];
                    }

                    // Swap internal arrays and size to new values.
                    this.size = nsize;
                    this.items = ns;
                }
                pos = this.next++;
            }

            this.items[pos] = item;
            this.used++;
            return pos;
        }

        /**
         * Unregister a Socket for polling on the specified events.
         * 
         * @param socket the Socket to be unregistered
         */
        public void unregister(Socket socket) {
            unregisterInternal(socket);
        }

        /**
         * Unregister a Channel for polling on the specified events.
         * 
         * @param channel the Channel to be unregistered
         */
        public void unregister(SelectableChannel channel) {
            unregisterInternal(channel);
        }

        /**
         * Unregister a Socket for polling on the specified events.
         * 
         * @param socket the Socket to be unregistered
         */
        private void unregisterInternal(Object socket) {
            for (int i = 0; i < this.next; ++i) {
                PollItem item = this.items[i];
                if (item == null) {
                    continue;
                }
                if (item.socket == socket || item.channel == socket) {
                    this.items[i] = null;

                    this.freeSlots.add(i);
                    --this.used;

                    break;
                }
            }
        }

        /**
         * Get the socket associated with an index.
         * 
         * @param index the desired index.
         * @return the Socket associated with that index (or null).
         */
        public Socket getSocket(int index) {
            if (index < 0 || index >= this.next)
                return null;
            return this.items[index].socket;
        }

        /**
         * Get the PollItem associated with an index.
         * 
         * @param index the desired index.
         * @return the PollItem associated with that index (or null).
         */
        public PollItem getItem(int index) {
            if (index < 0 || index >= this.next)
                return null;
            return this.items[index];
        }

        /**
         * Get the current poll timeout.
         * 
         * @return the current poll timeout in microseconds.
         * @deprecated Timeout handling has been moved to the poll() methods.
         */
        public long getTimeout() {
            return this.timeout;
        }

        /**
         * Set the poll timeout.
         * 
         * @param timeout the desired poll timeout in microseconds.
         * @deprecated Timeout handling has been moved to the poll() methods.
         */
        public void setTimeout(long timeout) {
            if (timeout < -1)
                return;

            this.timeout = timeout;
        }

        /**
         * Get the current poll set size.
         * 
         * @return the current poll set size.
         */
        public int getSize() {
            return this.size;
        }

        /**
         * Get the index for the next position in the poll set size.
         * 
         * @return the index for the next position in the poll set size.
         */
        public int getNext() {
            return this.next;
        }

        /**
         * Issue a poll call. If the poller's internal timeout value has been set, use that value as timeout; otherwise,
         * block indefinitely.
         * 
         * @return how many objects where signalled by poll ().
         */
        public long poll() {
            long tout = -1;
            if (this.timeout > -1) {
                tout = this.timeout;
            }
            return poll(tout);
        }

        /**
         * Issue a poll call, using the specified timeout value.
         * <p>
         * Since ZeroMQ 3.0, the timeout parameter is in <i>milliseconds</i>,
         * but prior to this the unit was <i>microseconds</i>.
         * 
         * @param tout the timeout, as per zmq_poll if -1, it will block
         *             indefinitely until an event happens; if 0, it will
         *             return immediately; otherwise, it will wait for at most
         *             that many milliseconds/microseconds (see above).
         * 
         * @see <a href="http://api.zeromq.org/2-1:zmq-poll">2.1 docs</a>
         * @see <a href="http://api.zeromq.org/3-0:zmq-poll">3.0 docs</a>
         * 
         * @return how many objects where signalled by poll ()
         */
        public int poll(long tout) {
            if (tout < -1) {
                return 0;
            }
            if (this.size <= 0 || this.next <= 0) {
                return 0;
            }

            return run_poll(this.items, this.used, tout);
        }

        /**
         * Check whether the specified element in the poll set was signalled for input.
         * 
         * @param index
         * 
         * @return true if the element was signalled.
         */
        public boolean pollin(int index) {
            return poll_mask(index, POLLIN);
        }

        /**
         * Check whether the specified element in the poll set was signalled for output.
         * 
         * @param index
         * 
         * @return true if the element was signalled.
         */
        public boolean pollout(int index) {
            return poll_mask(index, POLLOUT);
        }

        /**
         * Check whether the specified element in the poll set was signalled for error.
         * 
         * @param index
         * 
         * @return true if the element was signalled.
         */
        public boolean pollerr(int index) {
            return poll_mask(index, POLLERR);
        }

        /**
         * Constructor
         * 
         * @param size the number of Sockets this poller will contain.
         */
        public Poller(int size) {
            this(null, size);
        }

        /**
         * Class constructor.
         * 
         * @param context a 0MQ context previously created.
         */
        protected Poller(Context context) {
            this(context, SIZE_DEFAULT);
        }

        /**
         * Class constructor.
         * 
         * @param context a 0MQ context previously created.
         * @param size the number of Sockets this poller will contain.
         */
        protected Poller(Context context, int size) {
            this.context = context;
            this.size = size;
            this.next = 0;

            this.items = new PollItem[this.size];

            freeSlots = new LinkedList<Integer>();
        }

        /**
         * Issue a poll call on the specified 0MQ items.
         * <p>
         * Since ZeroMQ 3.0, the timeout parameter is in <i>milliseconds</i>, but prior to this the unit was
         * <i>microseconds</i>.
         * 
         * @param items an array of PollItem to poll.
         * @param timeout the maximum timeout in milliseconds/microseconds (see above).
         * @return how many objects where signalled by poll.
         * @see <a href="http://api.zeromq.org/2-1:zmq-poll">2.1 docs</a>
         * @see <a href="http://api.zeromq.org/3-0:zmq-poll">3.0 docs</a>
         */
        protected native static int run_poll(PollItem[] items, int count, long timeout);

        /**
         * Check whether a specific mask was signalled by latest poll call.
         * 
         * @param index the index indicating the socket.
         * @param mask a combination of POLLIN, POLLOUT and POLLERR.
         * @return true if specific socket was signalled as specified.
         */
        private boolean poll_mask(int index, int mask) {
            if (mask <= 0 || index < 0 || index >= this.next || this.items[index] == null) {
                return false;
            }
            return (this.items[index].revents & mask) > 0;
        }

        private Context context = null;
        private long timeout = -2; // mark as uninitialized
        private int size = 0;
        private int next = 0;
        private int used = 0;
        private PollItem[] items = null;
        // When socket is removed from polling, store free slots here
        private LinkedList<Integer> freeSlots = null;

        private static final int SIZE_DEFAULT = 32;
        private static final int SIZE_INCREMENT = 16;
    }
    
    /**
     * Inner class: Event.
     * Monitor socket event class
     */
    public static class Event {
        private static native void nativeInit();

        static {
            nativeInit();
        }
        
        private final int event;
        private final Object value;
        private final String address;
        
        private Event(int event, int value, String address) {
            this(event, Integer.valueOf(value), address != null ? address : "");
        }

        public Event(int event, Object value, String address) {
            this.event = event;
            this.value = value;
            this.address = address;
        }

        public int getEvent() {
            return event;
        }

        public Object getValue() {
            return value;
        }

        /**
         * Get the address.
         * For libzmq versions 3.2.x the address will be an empty string.
         * @return 
         */
        public String getAddress() {
            return address;
        }
        
        private static native Event recv(long socket, int flags) throws ZMQException;
        
        /**
         * Receive an event from a monitor socket.
         * For libzmq versions 3.2.x the address will be an empty string.
         * @param socket the socket
         * @param flags the flags to apply to the receive operation.
         * @return the received event or null if no message was received.
         * @throws ZMQException
         */
        public static Event recv(Socket socket, int flags) throws ZMQException {
            return Event.recv(socket.socketHandle, flags);
        }
        
        /**
         * Receive an event from a monitor socket.
         * Does a blocking recv.
         * For libzmq versions 3.2.x the address will be an empty string.
         * @param socket the socket
         * @return the received event.
         * @throws ZMQException
         */
        public static Event recv(Socket socket) throws ZMQException {
            return Event.recv(socket, 0);
        }
    }
}
