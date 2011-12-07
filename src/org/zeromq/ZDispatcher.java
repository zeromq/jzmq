package org.zeromq;

import java.util.ArrayList;
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
    private final ExecutorService handlerExecutor;

    public ZDispatcher() {
        this.handlerExecutor = Executors.newCachedThreadPool();
    }

    public ZDispatcher(ExecutorService handlerExecutor) {
        this.handlerExecutor = handlerExecutor;
    }

    public void registerHandler(ZMQ.Socket socket, SocketHandler socketHandler) {
        socketHandler.socket = socket;
        socketHandler.active = true;
        handlers.put(socket, socketHandler);
        handlerExecutor.execute(socketHandler);
    }

    public void unregisterHandler(ZMQ.Socket socket) {
        SocketHandler removedHandler = handlers.remove(socket);
        removedHandler.active = false;
    }

    public void shutdown() {
        handlerExecutor.shutdown();
        for (SocketHandler socketHandler : handlers.values()) {
            socketHandler.active = false;
        }
    }


    public static abstract class SocketHandler implements Runnable {
        private ZMQ.Socket socket;
        private BlockingQueue<ZMsg> in = new LinkedBlockingQueue<ZMsg>();
        private BlockingQueue<ZMsg> out = new LinkedBlockingQueue<ZMsg>();
        private volatile boolean active = false;

        public boolean send(ZMsg msg) {
            return out.add(msg);
        }

        public void run() {
            while (active) {
                doReceive();
                doHandle();
                doSend();
            }
        }

        public abstract void handleMessage(ZMsg msg);

        private void doReceive() {
            ZMsg msg;
            while (active && (msg = ZMsg.recvMsg(socket, ZMQ.DONTWAIT)) != null && msg.size() > 0 && msg.getFirst().hasData()) {
                in.add(msg);
            }
        }

        private void doHandle() {
            final ArrayList<ZMsg> messageBuffer = new ArrayList<ZMsg>(in.size());
            in.drainTo(messageBuffer);
            for (ZMsg message : messageBuffer) {
                if (active) {
                    handleMessage(message);
                }
            }
        }

        private void doSend() {
            ZMsg msg = null;
            while ((msg = out.poll()) != null) {
                if (active) {
                    msg.send(socket);
                }
            }
        }
    }
}
