package org.zeromq;

import java.io.Closeable;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.CopyOnWriteArrayList;

import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

/**
 * ZContext provides a high-level ZeroMQ context management class
 * 
 * The ZContext class wraps java org.zeromq.Context objects, which in turn wrap native 0MQ contexts. It manages open
 * sockets in the context and automatically closes these before terminating the context. It provides a simple way to set
 * the linger timeout on sockets, and configure contexts for number of I/O threads. Sets-up signal (interrupt) handling
 * for the process.
 * 
 * @see <a href="http://github.com/zeromq/czmq/blob/master/src/zctx.c">czmq version</a>
 * 
 * @author rsmith (at) rsbatechnology (dot) co (dot) uk
 */
public class ZContext implements Closeable {

    /**
     * Reference to underlying Context object
     */
    private ZMQ.Context context;

    /**
     * List of sockets managed by this ZContext
     */
    private List<Socket> sockets;

    /**
     * Number of io threads allocated to this context, default 1
     */
    private int ioThreads;

    /**
     * Linger timeout, default 0
     */
    private int linger;

    /**
     * Indicates if context object is owned by main thread (useful for multi-threaded applications)
     */
    private boolean main;

    /**
     * Class Constructor
     */
    public ZContext() {
        context = null; // Don't create Context until create 1st 0MQ socket
        sockets = new CopyOnWriteArrayList<Socket>();
        ioThreads = 1;
        linger = 0;
        main = true;
    }

    /**
     * Destructor. Call this to gracefully terminate context and close any managed 0MQ sockets
     */
    public void destroy() {
        ListIterator<Socket> itr = sockets.listIterator();
        while (itr.hasNext()) {
            destroySocket(itr.next());
        }
        sockets.clear();

        // Only terminate context if we are on the main thread
        if (isMain() && context != null)
            context.term();

    }

    /**
     * Creates a new managed socket within this ZContext instance. Use this to get automatic management of the socket at
     * shutdown
     * 
     * @param type socket type (see ZMQ static class members)
     * @return Newly created Socket object
     */
    public Socket createSocket(int type) {
        if (context == null)
            context = ZMQ.context(ioThreads);

        // Create and register socket
        Socket socket = context.socket(type);
        sockets.add(socket);
        return socket;
    }

    /**
     * Destroys managed socket within this context and remove from sockets list
     * 
     * @param s org.zeromq.Socket object to destroy
     */
    public void destroySocket(Socket s) {
        if (s == null)
            return;

        if (sockets.contains(s)) {
            try {
                s.setLinger(linger);
            } catch (ZMQException e) {
                if (e.getErrorCode() != ZMQ.ETERM()) {
                    throw e;
                }
            }
            s.close();
            sockets.remove(s);
        }
    }

    /**
     * Creates new shadow context. Shares same underlying org.zeromq.Context instance but has own list of managed
     * sockets, io thread count etc.
     * 
     * @param ctx Original ZContext to create shadow of
     * @return New ZContext
     */
    public static ZContext shadow(ZContext ctx) {
        ZContext shadow = new ZContext();
        shadow.setContext(ctx.getContext());
        shadow.setMain(false);
        return shadow;
    }

    /**
     * @return the ioThreads
     */
    public int getIoThreads() {
        return ioThreads;
    }

    /**
     * @param ioThreads the ioThreads to set
     */
    public void setIoThreads(int ioThreads) {
        this.ioThreads = ioThreads;
    }

    /**
     * @return the linger
     */
    public int getLinger() {
        return linger;
    }

    /**
     * @param linger the linger to set
     */
    public void setLinger(int linger) {
        this.linger = linger;
    }

    /**
     * @return the main
     */
    public boolean isMain() {
        return main;
    }

    /**
     * @param main the main to set
     */
    public void setMain(boolean main) {
        this.main = main;
    }

    /**
     * @return the context
     */
    public Context getContext() {
        return context;
    }

    /**
     * @param ctx sets the underlying org.zeromq.Context associated with this ZContext wrapper object
     */
    public void setContext(Context ctx) {
        this.context = ctx;
    }

    /**
     * @return the sockets
     */
    public List<Socket> getSockets() {
        return sockets;
    }

    @Override
    public void close() {
        destroy();
    }
}
