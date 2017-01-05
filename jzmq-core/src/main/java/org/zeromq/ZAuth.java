package org.zeromq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
	public static final String CURVE_ALLOW_ANY = "*";
	
    private Socket pipe; //pipe to backend agent
    private boolean verbose;

    /**
     * A small class for working with ZAP requests and replies. 
     */
    public static class ZAPRequest {

        public Socket handler;              //socket we're talking to
        public String version;              //  Version number, must be "1.0"
        public String sequence;             //  Sequence number of request
        public String domain;               //  Server socket domain
        public String address;              //  Client IP address
        public String identity;             //  Server socket idenntity
        public String mechanism;            //  Security mechansim
        public String username;             //  PLAIN user name
        public String password;             //  PLAIN password, in clear text
        public String clientKey;            //  CURVE client public key in ASCII
        public String principal;            //  GSSAPI principal

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

                // Get mechanism-specific frames
                if (self.mechanism.equals("PLAIN")) {
                    self.username = request.popString();
                    self.password = request.popString();
                } else if (self.mechanism.equals("CURVE")) {
                	ZFrame frame = request.pop();
                	byte[] clientPublicKey = frame.getData();
                	self.clientKey = ZMQ.Curve.z85Encode(clientPublicKey);
                } else if (self.mechanism.equals("GSSAPI")) {
                    self.principal = request.popString();
                }

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
        private ConcurrentMap<String, String> passwords = new ConcurrentHashMap<String, String>(); // PLAIN passwords, if loaded
        private boolean terminated; //did api ask us to quit?
        private File passwords_file;
        private long passwords_modified;
        final private ZAuth auth; //our parent auth, used for authorization callbacks
        private boolean allow_any;
        private ZCertStore certStore = null;
        
        

        private ZAuthAgent(ZAuth auth) {
            this.auth = auth;
        }

        /**
         * handle a message from the front end api
         *
         * @return
         */
        private boolean controlMessage() {
            ZMsg msg = ZMsg.recvMsg(pipe);

            String command = msg.popString();
            if (verbose) {
            	System.out.printf("ZAuth: API command=%s\n",command);
            }
            if (command == null) {
                return false; //interrupted
            }
            if (command.equals("ALLOW")) {
                String address = msg.popString();
                if (verbose) {
                	System.out.printf("ZAuth: - whitelisting ipaddress=%s\n", address);
                }
                whitelist.put(address, "OK");
            } else if (command.equals("DENY")) {
                String address = msg.popString();
                if (verbose) {
                	System.out.printf("ZAuth: - blacklisting ipaddress=%s\n", address);
                }            	
                blacklist.put(address, "OK");
            } else if (command.equals("PLAIN")) {
                // For now we don't do anything with domains
                String domain = msg.popString();
                // Get password file and load into HashMap
                // If the file doesn't exist we'll get an empty map
                String filename = msg.popString();
                this.passwords_file = new File(filename);

                if (verbose) {
            		System.out.println("ZAuth: - activated plain-mechanism with password-file:"+this.passwords_file.getAbsolutePath());
            	}

                this.loadPasswords(true);

                ZMsg reply = new ZMsg();
                reply.add("OK");
                reply.send(pipe);
                reply.destroy();
            } else if (command.equals("CURVE")) {
                //  If location is CURVE_ALLOW_ANY, allow all clients. Otherwise
                //  treat location as a directory that holds the certificates.            	
            	String location = msg.popString();
            	if (location.equals(CURVE_ALLOW_ANY)){
            		allow_any=true;
            	} else {
            		this.certStore = new ZCertStore(location);
            		this.allow_any = false;
            	}
            } else if (command.equals("GSSAPI")) {
                //for now, we don't do anything with domains
                String domain = msg.popString();
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
                        System.out.printf("I: PASSED (whitelist) address = %s\n", request.address);
                    }
                } else {
                    denied = true;
                    if (verbose) {
                        System.out.printf("I: DENIED (not in whitelist) address = %s\n", request.address);
                    }
                }
            } else if (!blacklist.isEmpty()) {
                if (blacklist.containsKey(request.address)) {
                    denied = true;
                    if (verbose) {
                        System.out.printf("I: DENIED (blacklist) address = %s\n", request.address);
                    }
                } else {
                    allowed = true;
                    if (verbose) {
                        System.out.printf("I: PASSED (not in blacklist) address = %s\n", request.address);
                    }
                }
            }

            //mechanism specific check
            if (!denied) {
                if (request.mechanism.equals("NULL") && !allowed) {
                    //  For NULL, we allow if the address wasn't blacklisted
                    if (verbose) {
                        System.out.printf("I: ALLOWED (NULL)\n");
                    }
                    allowed = true;
                } else if (request.mechanism.equals("PLAIN")) {
                    // For PLAIN, even a whitelisted address must authenticate
                    allowed = authenticatePlain(request);
                } else if (request.mechanism.equals("CURVE")) {
                    // For CURVE, even a whitelisted address must authenticate
                	allowed = authenticateCurve(request);
                } else if (request.mechanism.equals("GSSAPI")) {
                    // At this point, the request is authenticated, send to 
                    //zauth callback for complete authorization
                    allowed = auth.authenticateGSS(request);
                } else {
                    System.out.printf("Skipping unknown mechanism%n");
                }
            }

            if (allowed) {
                ZAPRequest.reply(request, "200", "OK");
            } else {
                ZAPRequest.reply(request, "400", "NO ACCESS");
            }

            return true;
        }

        private boolean authenticatePlain(ZAPRequest request) {
            // Refresh the passwords map if the file changed
            this.loadPasswords(false);

            String password = this.passwords.get(request.username);
            if (password != null && password.equals(request.password)) {
                if (this.verbose) {
                    System.out.printf("ZAUTH I: ALLOWED (PLAIN) username=%s password=%s\n",
                                      request.username, request.password);
                }

                return true;
            } else {
                if (this.verbose) {
                    System.out.printf("ZAUTH I: DENIED (PLAIN) username=%s password=%s\n",
                                      request.username, request.password);
                }

                return false;
            }
        }
        
        private boolean authenticateCurve(ZAPRequest request) {
        	if (this.allow_any) {
        		if (this.verbose) {
        			System.out.println("zauth: - allowed (CURVE allow any client)");
        		}
        		return true;
        	} else {
        		if (this.certStore!=null) {
        			if (this.certStore.containsPublicKey(request.clientKey)) {
        				// login allowed
        				if (verbose) {
        					System.out.printf("zauth: - allowed (CURVE) client_key=%s\n",request.clientKey);	
        				}
        				return true;
        			} else {
        				// login not allowed. couldn't find certificate
        				if (verbose) {
        					System.out.printf("zauth: - denied (CURVE) client_key=%s\n",request.clientKey);	
        				}
        				return false;
        			}
        		}
        	}
        	return false;
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

        private void loadPasswords(boolean initial) {
            if (!initial) {
                long lastModified = this.passwords_file.lastModified();
                long age = System.currentTimeMillis() - lastModified;
                if (lastModified > this.passwords_modified && age > 1000) {
                    // File has been modified and is stable, clear hashmap
                    this.passwords.clear();
                } else {
                    return;
                }
            }

            this.passwords_modified = this.passwords_file.lastModified();
            try {
                BufferedReader br = new BufferedReader(new FileReader(this.passwords_file));
                String line;
                while ((line = br.readLine()) != null) {
                    // Skip lines starting with "#" or that do not look like name=value data
                    int equals = line.indexOf('=');
                    if (line.charAt(0) == '#' || equals == -1 || equals == line.length() - 1) {
                        continue;
                    }

                    this.passwords.put(line.substring(0, equals), line.substring(equals + 1, line.length()));
                }
                br.close();
            } catch (Exception ex) {
                // Ignore the exception, just don't read the file
            }
        }
    }

    /**
     * Install authentication for the specified context. Note that until you add
     * policies, all incoming NULL connections are allowed (classic ZeroMQ
     * behaviour), and all PLAIN and CURVE connections are denied.
     */
    public ZAuth(ZContext ctx) {
        pipe = ZThread.fork(ctx, new ZAuthAgent(this));
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
        this.verbose = verbose;

        //agent should also be verbose
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
     * Configure PLAIN authentication for a given domain. PLAIN authentication
     * uses a plain-text password file. To cover all domains, use "*". You can
     * modify the password file at any time; it is reloaded automatically.
     *
     * @param domain
     * @param filename
     */
    public void configurePlain(String domain, String filename) {
        assert (domain != null);
        assert (filename != null);

        ZMsg msg = new ZMsg();
        msg.add("PLAIN");
        msg.add(domain);
        msg.add(filename);
        msg.send(pipe);
        msg.destroy();
    }

    /**
     * Configure CURVE authentication 
     *
     * @param location Can be ZAuth.CURVE_ALLOW_ANY or a directory with public-keys that will be accepted
     */
    public void configureCurve(String location) {
        assert (location != null);

        ZMsg msg = new ZMsg();
        msg.add("CURVE");
        msg.add(location);
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

    /*Configure GSSAPI authentication for a given domain. GSSAPI authentication
     uses an underlying mechanism (usually Kerberos) to establish a secure
     context and perform mutual authentication.  To cover all domains, use "*". */
    public void configureGSSAPI(String domain) {
        assert (domain != null);
        ZMsg msg = new ZMsg();
        msg.add("GSSAPI");
        msg.add(domain);
        msg.send(pipe);
        msg.destroy();
    }

    /*
     * Callback for authorizing an authenticated GSS connection.  Returns true 
     * if the connection is authorized, false otherwise.  Default implementation 
     * authorizes all authenticated connections.
     */
    protected boolean authenticateGSS(ZAPRequest request) {
        if (verbose) {
            System.out.printf("I: ALLOWED (GSSAPI allow any client) principal = %s identity = %s%n", request.principal, request.identity);
        }

        return true;
    }
}
