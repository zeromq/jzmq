package org.zeromq;

import org.junit.Test;

/**
 */
public class ZDispatcherTest {

    @Test
    public void test() {
        ZContext ctx = new ZContext();

        ZMQ.Socket output = ctx.createSocket(ZMQ.PAIR);
        output.bind("inproc://zmsg.test");
        ZMQ.Socket logger = ctx.createSocket(ZMQ.PAIR);
        logger.connect("inproc://zmsg.test");


        ZDispatcher zDispatcher = new ZDispatcher(10);
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
                System.out.println(msg.poll().toString());
            }
        });

        ZMsg msg = new ZMsg();
        ZFrame frame = new ZFrame("Hello");
        msg.addFirst(frame);
        outputHandler.send(msg);
        System.out.println();

    }
}
