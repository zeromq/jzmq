package org.zeromq;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.assertEquals;

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


        ZDispatcher zDispatcher = new ZDispatcher(2);
        zDispatcher.dispatch();

        ZDispatcher.SocketHandler outputHandler = new ZDispatcher.SocketHandler() {
            @Override
            public void handleMessage(ZMsg msg) {

            }
        };
        zDispatcher.register(output, outputHandler);
        zDispatcher.register(logger, new ZDispatcher.SocketHandler() {
            @Override
            public void handleMessage(ZMsg msg) {
                assertEquals("Hello", msg.poll().toString());
                latch.countDown();
            }
        });

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame("Hello");
        msg.addFirst(frame);
        outputHandler.send(msg);

        latch.await(1, TimeUnit.SECONDS);
        assertEquals(0, latch.getCount());
    }
}
