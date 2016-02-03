package org.zeromq;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Dispatcher for ZeroMQ Sockets.
 *
 * Warning:
 * The Dispatcher uses a busy spin loop when waiting on events.
 * This is ideal for low latency applications but not in all situations.
 * It has the side effect of consuming 100% of a CPU when waiting for events.
 *
 * With this dispatcher, you can register ONE handler per socket 
 * and get a Sender for sending ZMsg.
 */
public class ZDispatcher {
    private ConcurrentMap<ZMQ.Socket, SocketDispatcher> dispatchers = new ConcurrentHashMap<ZMQ.Socket, SocketDispatcher>();
    private final ExecutorService dispatcherExecutor;

    public ZDispatcher() {
        this.dispatcherExecutor = Executors.newCachedThreadPool();
    }

    public ZDispatcher(ExecutorService dispatcherExecutor) {
        this.dispatcherExecutor = dispatcherExecutor;
    }

    public void registerHandler(ZMQ.Socket socket, ZMessageHandler messageHandler, ZSender sender) {
        registerHandler(socket, messageHandler, sender, Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()));
    }

    public void registerHandler(ZMQ.Socket socket, ZMessageHandler messageHandler, ZSender sender, ExecutorService threadpool) {
        SocketDispatcher socketDispatcher = new SocketDispatcher(socket, messageHandler, sender, threadpool);
        if (dispatchers.putIfAbsent(socket, socketDispatcher) != null) {
            throw new IllegalArgumentException("This socket already have a message handler");
        }
        socketDispatcher.start();
        dispatcherExecutor.execute(socketDispatcher);
    }

    public void unregisterHandler(ZMQ.Socket socket) {
        SocketDispatcher removedDispatcher = dispatchers.remove(socket);
        if (removedDispatcher == null) {
            throw new IllegalArgumentException("This socket doesn't have a message handler");
        }
        removedDispatcher.shutdown();
    }

    public void shutdown() {
        dispatcherExecutor.shutdown();
        for (SocketDispatcher socketDispatcher : dispatchers.values()) {
            socketDispatcher.shutdown();
        }
        dispatchers.clear();
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
        private volatile boolean active = false;
        private final CountDownLatch shutdownLatch = new CountDownLatch(1);
        private final ZMQ.Socket socket;
        private final ZMessageHandler handler;
        private final ZSender sender;
        private final ExecutorService threadpool;
        private final BlockingQueue<ZMsg> in = new LinkedBlockingQueue<ZMsg>();
        private static final int BUFFER_SIZE = 1024;
        private static final ThreadLocal<ZMessageBuffer> messages = new ThreadLocal<ZMessageBuffer>() {
            @Override
            protected ZMessageBuffer initialValue() {
                return new ZMessageBuffer();
            }
        };
        private final AtomicBoolean busy = new AtomicBoolean(false);

        public SocketDispatcher(ZMQ.Socket socket, ZMessageHandler handler, ZSender sender, ExecutorService handleThreadpool) {
            this.socket = socket;
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
            threadpool.shutdown();
            shutdownLatch.countDown();
        }

        public void start() {
            this.active = true;
        }

        public void shutdown() {
            try {
                this.active = false;
                this.shutdownLatch.await();

            } catch (InterruptedException e) {
            }
        }

        private void doReceive() {
            ZMsg msg;
            int remainingBuffer = BUFFER_SIZE;
            while (active && remainingBuffer-- > 0 && (msg = ZMsg.recvMsg(socket, ZMQ.DONTWAIT)) != null && msg.size() > 0 && msg.getFirst().hasData()) {
                in.add(msg);
            }
        }

        private void doHandle() {
            if (!in.isEmpty() && busy.compareAndSet(false, true)) {
                threadpool.submit(new Runnable() {
                    @Override
                    public void run() {
                        ZMessageBuffer messages = SocketDispatcher.this.messages.get();
                        messages.drainFrom(in);
                        busy.set(false);
                        for (int i = 0; i <= messages.lastValidIndex; i++) {
                            if (active) {
                                handler.handleMessage(sender, messages.buffer[i]);
                            }
                        }
                    }
                });
            }
        }

        private void doSend() {
            ZMsg msg;
            int remainingBuffer = BUFFER_SIZE;
            while (active && remainingBuffer-- > 0 && (msg = sender.out.poll()) != null) {
                msg.send(socket);
            }
        }

        private static class ZMessageBuffer {
            private final ZMsg[] buffer = new ZMsg[BUFFER_SIZE];
            private int lastValidIndex;

            private void drainFrom(BlockingQueue<ZMsg> in) {
                int lastIndex = lastValidIndex = -1;
                ZMsg msg;
                while (++lastIndex < buffer.length && (msg = in.poll()) != null) {
                    buffer[lastIndex] = msg;
                    lastValidIndex = lastIndex;
                }
            }
        }
    }
}
