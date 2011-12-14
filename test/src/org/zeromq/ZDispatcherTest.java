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

        ZMQ.Socket logger = ctx.createSocket(ZMQ.PAIR);
        logger.bind("inproc://zmsg.test");
        ZMQ.Socket out = ctx.createSocket(ZMQ.PAIR);
        out.connect("inproc://zmsg.test");

        final String mesgTxt = "Hello";

        ZDispatcher dispatcher = new ZDispatcher();

        ZDispatcher.ZSender outSender = new ZDispatcher.ZSender();
        dispatcher.registerHandler(out, new ZDispatcher.ZMessageHandler() {
                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {

                    }
                }, outSender);

        dispatcher.registerHandler(logger, new ZDispatcher.ZMessageHandler() {
                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {

                        assertEquals(mesgTxt, msg.poll().toString());
                        latch.countDown();
                    }
                }, new ZDispatcher.ZSender());

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame(mesgTxt);
        msg.addFirst(frame);
        outSender.send(msg);

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

        ZDispatcher.ZSender senderOne = new ZDispatcher.ZSender();
        dispatcher.registerHandler(socketOne, new ZDispatcher.ZMessageHandler() {

                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {
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
                }, senderOne);

        ZDispatcher.ZSender senderTwo = new ZDispatcher.ZSender();
        dispatcher.registerHandler(socketTwo, new ZDispatcher.ZMessageHandler() {

                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {
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
                }, senderTwo);

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame("Hello");
        msg.addFirst(frame);

        senderOne.send(msg.duplicate());
        senderOne.send(msg.duplicate());
        senderTwo.send(msg.duplicate());
        senderTwo.send(msg.duplicate());

        handlersBarrier.await(1, TimeUnit.SECONDS);
        handlersBarrier.await(1, TimeUnit.SECONDS);

        assertFalse(threadingIssueDetected.get());
    }

    @Test
    public void testNoMessageAreSentAfterShutdown() throws InterruptedException, BrokenBarrierException, TimeoutException {
        final AtomicBoolean shutdownIssueDetected = new AtomicBoolean(false);
        final CountDownLatch latch = new CountDownLatch(1);
        ZContext ctx = new ZContext();

        ZMQ.Socket socketOne = ctx.createSocket(ZMQ.PAIR);
        socketOne.bind("inproc://zmsg.test");
        ZMQ.Socket socketTwo = ctx.createSocket(ZMQ.PAIR);
        socketTwo.connect("inproc://zmsg.test");

        final ZDispatcher dispatcher = new ZDispatcher();
        final CyclicBarrier handlersBarrier = new CyclicBarrier(2, new Runnable() {
            @Override
            public void run() {
                if (latch.getCount() == 0) {
                    dispatcher.shutdown();
                }
            }
        });

        ZDispatcher.ZSender senderOne = new ZDispatcher.ZSender();
        dispatcher.registerHandler(socketOne, new ZDispatcher.ZMessageHandler() {
                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {
                        latch.countDown();
                        try {
                            handlersBarrier.await(1, TimeUnit.SECONDS);
                        } catch (Exception e) {
                        }
                    }
                }, senderOne);
        ZDispatcher.ZSender senderTwo = new ZDispatcher.ZSender();
        dispatcher.registerHandler(socketTwo, new ZDispatcher.ZMessageHandler() {

                    @Override
                    public void handleMessage(ZDispatcher.ZSender sender, ZMsg msg) {
                        sender.send(msg);
                        shutdownIssueDetected.set(true);
                    }
                }, senderTwo);

        ZMsg msg = new ZMsg();
        msg.add(new ZFrame("Hello"));

        senderTwo.send(msg);
        handlersBarrier.await(1, TimeUnit.SECONDS);

        senderOne.send(msg);
        senderOne.send(msg);

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
        assertFalse(shutdownIssueDetected.get());
    }
}