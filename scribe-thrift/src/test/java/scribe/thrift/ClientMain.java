package scribe.thrift;

import java.util.ArrayList;
import java.util.List;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe.Client;

public class ClientMain {

    public static void main(String[] argv) throws Exception {
	TSocket socket = new TSocket("127.0.0.1", 1463);
	TFramedTransport transport = new TFramedTransport(socket);
	TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
	Client client = new Client(protocol, protocol);
	transport.open();
	List<LogEntry> list = new ArrayList<LogEntry>();
	list.add(new LogEntry(argv[0], argv[1]))
	ResultCode rc = client.Log(list);
	transport.flush();
	transport.close();
	System.out.println(rc);
	System.exit(0);
    }

}
