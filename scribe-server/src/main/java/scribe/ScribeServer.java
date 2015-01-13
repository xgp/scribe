package scribe;

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TNonblockingServer;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TNonblockingServerTransport;
import org.apache.thrift.transport.TNonblockingServerSocket;
import scribe.thrift.*;

public class ScribeServer {

    private final int port;
    private final LogHandler handler;
    private final scribe.Processor processor;
    private TServer server;
    
    public ScribeServer(int port, LogHandler handler) {
	this.port = port;
	this.handler = handler;
	this.processor = new scribe.Processor(handler);
    }

    public void start() throws Exception {
	TNonblockingServerTransport transport = new TNonblockingServerSocket(port);
	server = new TNonblockingServer(new TNonblockingServer.Args(transport).processor(processor));
	//this.server = new TSimpleServer(new Args(new TServerSocket(port)).processor(processor));
	Runnable server = new Runnable() {
		public void run() {
		    try {
			serve();
		    } catch (Exception e) {
			e.printStackTrace();
		    }
		}
	    };
	new Thread(server).start();
    }

    public void serve() {
	server.serve();
    }

    public void stop() {
	server.stop();
    }
}
    
