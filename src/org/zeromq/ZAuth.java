package org.zeromq;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.zeromq.ZMQ.PollItem;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZThread.IAttachedRunnable;

/**
 * ZAuth takes over authentication for all incoming connections in its context.
 * Note that libzmq provides four levels of security: default NULL (which zauth
 * does not see), and authenticated NULL, PLAIN, and CURVE, which zauth can see.
 *
 * Based on <a
 * href="http://github.com/zeromq/czmq/blob/master/src/zauth.c">zauth.c</a> in
 * czmq
 *
 * @author cbusbey (at) connamara (dot) com
 */
public class ZAuth {

    private Socket pipe; //pipe to backend agent

    /**
     * A small class for working with ZAP requests and replies. This isn't
     * exported in the JZMQ API just used internally in zauth to simplify
     * working with RFC 27 requests and replies.
     */
    private static class ZAPRequest {

        Socket handler; //socket we're talking to
        String version;              //  Version number, must be "1.0"
        String sequence;             //  Sequence number of request
        String domain;               //  Server socket domain
        String address;              //  Client IP address
        String identity;             //  Server socket idenntity
        String mechanism;            //  Security mechansim
        String username;             //  PLAIN user name
        String password;             //  PLAIN password, in clear text
        String clientKey;           //  CURVE client public key in ASCII

        static ZAPRequest recvRequest(Socket handler) {
            if (ZMQ.getMajorVersion() == 4) {
                ZMsg request = ZMsg.recvMsg(handler);
                ZAPRequest self = new ZAPRequest();

                //  Store handler socket so we can send a reply easily
                self.handler = handler;

                //  Get all standard frames off the handler socket
                self.version = request.popString();
                self.sequence = request.popString();
                self.domain = request.popString();
                self.address = request.popString();
                self.identity = request.popString();
                self.mechanism = request.popString();

                //  If the version is wrong, we're linked with a bogus libzmq, so die
                assert (self.version.equals("1.0"));

                //  TODO: Get mechanism-specific frames
                
                request.destroy();
                return self;
            } else {
                return null;
            }
        }

        /**
         * Send a zap reply to the handler socket
         *
         * @param request
         */
        static void reply(ZAPRequest request, String statusCode, String statusText) {
            if (request == null) {
                return;
            }

            ZMsg msg = new ZMsg();
            msg.add("1.0");
            msg.add(request.sequence);
            msg.add(statusCode);
            msg.add(statusText);
            msg.add("");
            msg.add("");
            msg.send(request.handler);
        }
    }

    /**
     * ZAuthAgent is the backend agent which we talk to over a pipe. This lets
     * the agent do work asynchronously in the background while our application
     * does other things. This is invisible to the caller, who sees a classic
     * API.
     */
    private static class ZAuthAgent implements IAttachedRunnable {

        private Socket pipe; //pipe back to application api
        private Socket handler; //ZAP handler socket
        private boolean verbose; //trace output to stdout
        private ConcurrentMap<String, String> whitelist = new ConcurrentHashMap<String, String>(); //whitelisted addresses
        private ConcurrentMap<String, String> blacklist = new ConcurrentHashMap<String, String>(); //blacklisted addresses
        private boolean terminated; //did api ask us to quit?

        /**
         * handle a message from the front end api
         *
         * @return
         */
        private boolean controlMessage() {
            ZMsg msg = ZMsg.recvMsg(pipe);

            String command = msg.popString();
            if (command == null) {
                return false; //interrupted
            }
            if (command.equals("ALLOW")) {
                String address = msg.popString();
                whitelist.put(address, "OK");
            } else if (command.equals("DENY")) {
                String address = msg.popString();
                blacklist.put(address, "OK");
            } else if (command.equals("VERBOSE")) {
                String verboseStr = msg.popString();
                this.verbose = verboseStr.equals("true");
            } else if (command.equals("TERMINATE")) {
                this.terminated = true;
                ZMsg reply = new ZMsg();
                reply.add("OK");
                reply.send(pipe);
                reply.destroy();
            }

            msg.destroy();

            return true;
        }

        private boolean authenticate() {
            ZAPRequest request = ZAPRequest.recvRequest(handler);
            if (request == null) {
                return false;
            }

            //is the address explicitly whitelisted or blacklisted?
            boolean allowed = false;
            boolean denied = false;

            if (!whitelist.isEmpty()) {
                if (whitelist.containsKey(request.address)) {
                    allowed = true;
                    if (verbose) {
                        System.out.printf("I: PASSED (whitelist) address = %s%n", request.address);
                    }
                } else {
                    denied = true;
                    if (verbose) {
                        System.out.printf("I: DENIED (not in whitelist) address = %s%n", request.address);
                    }
                }
            } else if (!blacklist.isEmpty()) {
                if (whitelist.containsKey(request.address)) {
                    denied = true;
                    if (verbose) {
                        System.out.printf("I: DENIED (blacklist) address = %s%n", request.address);
                    }
                } else {
                    allowed = true;
                    if (verbose) {
                        System.out.printf("I: PASSED (not in blacklist) address = %s%n", request.address);
                    }
                }
            }

            //mechanism specific check
            if (!denied) {
                if (request.mechanism.equals("NULL") && !allowed) {
                    //  For NULL, we allow if the address wasn't blacklisted
                    if (verbose) {
                        System.out.printf("I: ALLOWED (NULL)%n");
                    }
                    allowed = true;
                }
            }
            
            if (allowed) {
                ZAPRequest.reply(request, "200", "OK");
            } else {
                ZAPRequest.reply(request, "400", "NO ACCESS");
            }

            return true;
        }

        @Override
        public void run(Object[] args, ZContext ctx, Socket pipe) {
            this.pipe = pipe;

            //create ZAP handler and get ready for requests
            handler = ctx.createSocket(ZMQ.REP);
            try {
                handler.bind("inproc://zeromq.zap.01");
            } catch (ZMQException e) {
                pipe.send("ERROR");
                return;
            }

            pipe.send("OK");

            PollItem[] pollItems = {new PollItem(pipe, Poller.POLLIN), new PollItem(handler, Poller.POLLIN)};
            while (!terminated && !Thread.currentThread().isInterrupted()) {
                int rc = ZMQ.poll(pollItems, -1);
                if (rc == -1) {
                    break; //interrupt
                }

                if (pollItems[0].isReadable()) {
                    if (!controlMessage()) {
                        break;
                    }
                }

                if (pollItems[1].isReadable()) {
                    if (!authenticate()) {
                        break;
                    }
                }
            }
        }
    }

    /**
     * Install authentication for the specified context. Note that until you add
     * policies, all incoming NULL connections are allowed (classic ZeroMQ
     * behaviour), and all PLAIN and CURVE connections are denied.
     */
    public ZAuth(ZContext ctx) {
        pipe = ZThread.fork(ctx, new ZAuthAgent());
        ZMsg msg = ZMsg.recvMsg(pipe);
        String response = msg.popString();

        msg.destroy();
    }

    /**
     * Enable verbose tracing of commands and activity
     *
     * @param verbose
     */
    public void setVerbose(boolean verbose) {
        ZMsg msg = new ZMsg();
        msg.add("VERBOSE");
        msg.add(String.format("%b", verbose));
        msg.send(pipe);
        msg.destroy();
    }

    /**
     * Allow (whitelist) a single IP address. For NULL, all clients from this
     * address will be accepted. For PLAIN and CURVE, they will be allowed to
     * continue with authentication. You can call this method multiple times to
     * whitelist multiple IP addresses. If you whitelist a single address, any
     * non-whitelisted addresses are treated as blacklisted.
     *
     */
    public void allow(String address) {
        assert (address != null);

        ZMsg msg = new ZMsg();
        msg.add("ALLOW");
        msg.add(address);
        msg.send(pipe);
        msg.destroy();
    }

    /**
     * Deny (blacklist) a single IP address. For all security mechanisms, this
     * rejects the connection without any further authentication. Use either a
     * whitelist, or a blacklist, not not both. If you define both a whitelist
     * and a blacklist, only the whitelist takes effect.
     */
    public void deny(String address) {
        assert (address != null);

        ZMsg msg = new ZMsg();
        msg.add("DENY");
        msg.add(address);
        msg.send(pipe);
        msg.destroy();
    }

    /**
     * Destructor.
     */
    public void destroy() {
        ZMsg request = new ZMsg();
        request.add("TERMINATE");
        request.send(pipe);
        request.destroy();

        ZMsg reply = ZMsg.recvMsg(pipe);
        reply.destroy();
    }
}
