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
    private ConcurrentMap<ZMQ.Socket, SocketDispatcher> handlers = new ConcurrentHashMap<ZMQ.Socket, SocketDispatcher>();
    private final ExecutorService handlerExecutor;

    public ZDispatcher() {
        this.handlerExecutor = Executors.newCachedThreadPool();
    }

    public ZDispatcher(ExecutorService handlerExecutor) {
        this.handlerExecutor = handlerExecutor;
    }

    public void registerHandler(ZMQ.Socket socket, ZMessageHandler messageHandler, ZSender sender) {
        registerHandler(socket, messageHandler, sender, Executors.newFixedThreadPool(5));
    }

    public void registerHandler(ZMQ.Socket socket, ZMessageHandler messageHandler, ZSender sender, ExecutorService threadpool) {
        SocketDispatcher socketDispatcher = new SocketDispatcher(messageHandler, sender, threadpool);
        socketDispatcher.socket = socket;
        socketDispatcher.active = true;
        handlers.put(socket, socketDispatcher);
        handlerExecutor.execute(socketDispatcher);
    }

    public void unregisterHandler(ZMQ.Socket socket) {
        SocketDispatcher removedDispatcher = handlers.remove(socket);
        removedDispatcher.active = false;
    }

    public void shutdown() {
        handlerExecutor.shutdown();
        for (SocketDispatcher socketDispatcher : handlers.values()) {
            socketDispatcher.active = false;
        }
        handlers.clear();
    }

    public interface ZMessageHandler {

        public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg);

    }

    public final static class ZSender {
        private final BlockingQueue<ZMsg> out = new LinkedBlockingQueue<ZMsg>();

        public final boolean send(ZMsg msg) {
            return out.add(msg);
        }
    }

    private static final class SocketDispatcher implements Runnable {
        private ZMQ.Socket socket;
        private volatile boolean active = false;
        private final ZMessageHandler handler;
        private final ZSender sender;
        private final ExecutorService threadpool;
        private final BlockingQueue<ZMsg> in = new LinkedBlockingQueue<ZMsg>();
        private static final ThreadLocal<ZMessageBuffer> messageBuffer = new ThreadLocal<ZMessageBuffer>() {
            @Override
            protected ZMessageBuffer initialValue() {
                return new ZMessageBuffer();
            }
        };

        public SocketDispatcher(ZMessageHandler handler, ZSender sender, ExecutorService handleThreadpool) {
            this.handler = handler;
            this.sender = sender;
            this.threadpool = handleThreadpool;
        }

        public void run() {
            while (active) {
                doReceive();
                doHandle();
                doSend();
            }
        }

        private void doReceive() {
            ZMsg msg;
            while (active && (msg = ZMsg.recvMsg(socket, ZMQ.DONTWAIT)) != null && msg.size() > 0 && msg.getFirst().hasData()) {
                in.add(msg);
            }
        }

        private void doHandle() {

            if (in.size() > 0) {
                threadpool.submit(new Runnable() {
                    @Override
                    public void run() {
                        ZMessageBuffer buffer = messageBuffer.get();
                        buffer.drainFrom(in);
                        for (int i = 0; i <= buffer.lastValidIndex; i++) {
                            if (active) {
                                handler.handleMessage(sender, buffer.buffer[i]);
                            }
                        }
                    }
                });
            }
        }

        private void doSend() {
            ZMsg msg = null;
            while ((msg = sender.out.poll()) != null) {
                if (active) {
                    msg.send(socket);
                }
            }
        }

        private static class ZMessageBuffer {
            private final ZMsg[] buffer = new ZMsg[1024];
            private int lastValidIndex = 0;


            private void drainFrom(BlockingQueue<ZMsg> in) {
                int lastIndex = -1;
                ZMsg msg;
                while ((msg = in.poll()) != null) {
                    lastIndex++;
                    if (lastIndex < buffer.length) {
                        buffer[lastIndex] = msg;

                    } else {
                        break;

                    }
                }
                lastValidIndex = lastIndex;
            }
        }
    }
}
