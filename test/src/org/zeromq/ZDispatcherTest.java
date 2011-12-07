package org.zeromq;

import org.junit.Test;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;

/**
 */
public class ZDispatcherTest {

    @Test
    public void singleMessage() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        ZContext ctx = new ZContext();

        ZMQ.Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zmsg.test");
        ZMQ.Socket logger = ctx.createSocket(ZMQ.PAIR);
        logger.connect("inproc://zmsg.test");

        final String mesgTxt = "Hello";

        ZDispatcher dispatcher = new ZDispatcher();

        ZDispatcher.SocketHandler outputHandler = new ZDispatcher.SocketHandler() {
            @Override
            public void handleMessage(ZMsg msg) {

            }
        };
        dispatcher.registerHandler(output, outputHandler);
        dispatcher.registerHandler(logger, new ZDispatcher.SocketHandler() {
            @Override
            public void handleMessage(ZMsg msg) {

                assertEquals(mesgTxt, msg.poll().toString());
                latch.countDown();
            }
        });

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame(mesgTxt);
        msg.addFirst(frame);
        outputHandler.send(msg);

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testMessagesDispatchedToDifferentHandlersAreExecutedConcurrently() throws InterruptedException, BrokenBarrierException, TimeoutException {
        final AtomicBoolean threadingIssueDetected = new AtomicBoolean(false);
        final Lock guardLock1 = new ReentrantLock();
        final Lock guardLock2 = new ReentrantLock();
        final CyclicBarrier handlersBarrier = new CyclicBarrier(3);

        ZContext ctx = new ZContext();

        ZMQ.Socket socketOne = ctx.createSocket(ZMQ.PAIR);
        socketOne.bind("inproc://zmsg.test");
        ZMQ.Socket socketTwo = ctx.createSocket(ZMQ.PAIR);
        socketTwo.connect("inproc://zmsg.test");

        ZDispatcher dispatcher = new ZDispatcher();
        ZDispatcher.SocketHandler socketHandlerOne = new ZDispatcher.SocketHandler() {

            @Override
            public void handleMessage(ZMsg msg) {
                try {
                    if (guardLock1.tryLock()) {
                        handlersBarrier.await(1, TimeUnit.SECONDS);
                    } else {
                        threadingIssueDetected.set(true);
                    }
                } catch (Exception ex) {
                    threadingIssueDetected.set(true);
                } finally {
                    guardLock1.unlock();
                }
            }
        };
        dispatcher.registerHandler(socketOne, socketHandlerOne);
        ZDispatcher.SocketHandler socketHandlerTwo = new ZDispatcher.SocketHandler() {

            @Override
            public void handleMessage(ZMsg msg) {
                try {
                    if (guardLock2.tryLock()) {
                        handlersBarrier.await(1, TimeUnit.SECONDS);
                    } else {
                        threadingIssueDetected.set(true);
                    }
                } catch (Exception ex) {
                    threadingIssueDetected.set(true);
                } finally {
                    guardLock2.unlock();
                }
            }
        };
        dispatcher.registerHandler(socketTwo, socketHandlerTwo);

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame("Hello");
        msg.addFirst(frame);

        socketHandlerOne.send(msg.duplicate());
        socketHandlerOne.send(msg.duplicate());
        socketHandlerTwo.send(msg.duplicate());
        socketHandlerTwo.send(msg.duplicate());

        handlersBarrier.await(1, TimeUnit.SECONDS);
        handlersBarrier.await(1, TimeUnit.SECONDS);

        assertFalse(threadingIssueDetected.get());
    }
}
