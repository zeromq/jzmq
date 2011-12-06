package org.zeromq;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 */
public class ZDispatcher {
    private ConcurrentMap<ZMQ.Socket, SocketHandler> handlers = new ConcurrentHashMap<ZMQ.Socket, SocketHandler>();
    private volatile Thread selectorThread = null;
    private volatile boolean active = false;
    private final ExecutorService handlerExecutor;

    public ZDispatcher(int threadPoolSize) {
        this.handlerExecutor = Executors.newFixedThreadPool(threadPoolSize);
    }

    public void register(ZMQ.Socket socket, SocketHandler socketHandler) {
        socketHandler.socket = socket;
        handlers.put(socket, socketHandler);
    }

    public void unregister(ZMQ.Socket socket) {
        handlers.remove(socket);
    }

    public void dispatch() {
        if (!active) {
            active = true;
            selectorThread = new Thread() {

                @Override
                public void run() {
                    while (active) {
                        doReceive();
                        doSend();
                    }
                }
            };
            selectorThread.start();
        }
    }

    public void shutdown() {
        if (active) {
            active = false;
            handlerExecutor.shutdownNow();
        }
    }

    private void doReceive() {
        for (ZMQ.Socket socket : handlers.keySet()) {
            ZMsg msg = ZMsg.recvMsg(socket, ZMQ.DONTWAIT);
            if (msg.size() > 0 && msg.getFirst().hasData()) {
                handlers.get(socket).inQueue.add(msg);
            }
        }
    }

    private void doSend() {
        for (SocketHandler handler : handlers.values()) {
            ZMsg msg = null;
            while ((msg = handler.outQueue.poll()) != null) {
                msg.send(handler.socket);
            }
        }
    }

    public static abstract class SocketHandler {
        private ZMQ.Socket socket;
        private BlockingQueue<ZMsg> inQueue = new LinkedBlockingQueue<ZMsg>();
        private BlockingQueue<ZMsg> outQueue = new LinkedBlockingQueue<ZMsg>();

        public boolean send(ZMsg msg) {
            return outQueue.add(msg);
        }

        public abstract void handleMessage(ZMsg msg);
    }
}
