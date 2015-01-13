package scribe.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.transport.TFramedTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransportException;
import org.fusesource.hawtjournal.api.Journal;
import scribe.thrift.LogEntry;
import scribe.thrift.ResultCode;
import scribe.thrift.scribe.Client;

/**
 * Implementation of a logback Appender that writes log events to Scribe. This is an asynchronous
 * appender which attempts to write batches to scribe. While it makes efforts to rewrite failed
 * events, and flush its queue on shutdown, it may lose events.
 * @author garth
 */
public class ScribeAppender<E> extends UnsynchronizedAppenderBase<E> {

    private BQueue blockingQueue;
    private TFramedTransport transport = null;
    private Client client;
    private ScribeConverter converter;
    private PatternLayoutEncoder encoder;

    // The following are configurable via logback configuration
    private String facility = "scribe";
    private String host = "127.0.0.1";
    private int port = 1463;
    private String category = "logback";
    private String logDir = ".";
    public static final int DEFAULT_RETRY_INTERVAL = 300;
    private int retryInterval = DEFAULT_RETRY_INTERVAL;
    public static final int DEFAULT_BATCH_SIZE = 256;
    private int batchSize = DEFAULT_BATCH_SIZE;
    public static final String DEFAULT_BUFFER_TYPE = "memory";
    private String bufferType;
    
    Worker worker = new Worker();

    @Override
    public void start() {
	// bufferType
	if (bufferType == null) bufferType = DEFAULT_BUFFER_TYPE;
	if ("memory".equals(bufferType)) {
	    blockingQueue = new MemoryQueue();
	} else if ("journal".equals(bufferType)) {
	    Journal journal = new Journal();
	    journal.setDirectory(new File(logDir));
	    journal.setArchiveFiles(false);
	    journal.setChecksum(true);
	    journal.setMaxFileLength(1024 * 1024);
	    journal.setMaxWriteBatchSize(1024 * 10);
	    blockingQueue = new JournaledQueue(journal);	    
	} else {
	    throw new IllegalStateException("Unknown bufferType = " + bufferType);
	}

	// retryInterval
	if (retryInterval < 1) {
	    addError("Invalid retry interval [" + retryInterval + "]");
	    retryInterval = DEFAULT_RETRY_INTERVAL;
	}

	if (encoder == null) {
	    encoder = new PatternLayoutEncoder();
	    encoder.setPattern("%msg%n");
	    encoder.start();
	}
	converter = new ScribeConverter(encoder);
	reconnect();
	worker.setDaemon(true);
	worker.setName("ScribeAppender-Worker-" + worker.getName());
	// make sure this instance is marked as "started" before staring the worker Thread
	super.start();
	worker.start();
    }

    /**
     * Attempts to reconnect to the Scribe server.
     */
    private synchronized void reconnect() {
        try {
	    if (transport != null) {
		if (transport.isOpen()) { // this checks the underlying socket
		    return;
		} else {
		    transport.close(); // this closes the underlying socket
		}
	    }
            addInfo(String.format("Connecting to Scribe Server: %s:%d with category: %s", host, port, category));
	    TSocket socket = new TSocket(host, port);
            transport = new TFramedTransport(socket);
            TBinaryProtocol protocol = new TBinaryProtocol(transport, false, false);
            client = new Client(protocol, protocol);
            addInfo("Opening transport socket");
            transport.open();
        } catch (Exception e) {
            throw new RuntimeException("Error reconnecting to scribe", e);
        }
    }
    
    @Override
    public void stop() {
	if (!isStarted())
	    return;

	// mark this appender as stopped so that Worker can also processPriorToRemoval if it is invoking aii.appendLoopOnAppenders
	// and sub-appenders consume the interruption
	super.stop();
	
	// interrupt the worker thread so that it can terminate. Note that the interruption can be consumed
	// by sub-appenders
	worker.interrupt();
	try {
	    worker.join(10000); // wait 10s in case the worker is flushing events to scribe
	} catch (InterruptedException e) {
	    addError("Failed to join worker thread", e);
	}
        if (transport != null) {
	    try {
		transport.flush();
	    } catch (TTransportException e) {
		addError("Problem flushing transport", e);
	    } finally {
		transport.close();
	    }
        }
    }
    
    @Override
    protected void append(E eventObject) {
	blockingQueue.offer(new LogEntry(category, converter.getMessage(eventObject)));
    }

    public void setEncoder(PatternLayoutEncoder encoder) {
	this.encoder = encoder;
    }

    public PatternLayoutEncoder getEncoder() {
	return encoder;
    }
    
    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
  
    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }
  
    public int getRetryInterval() {
	return retryInterval;
    }

    public void setRetryInterval(int retryInterval) {
	this.retryInterval = retryInterval;
    }

    public int getBatchSize() {
	return batchSize;
    }

    public void setBatchSize(int batchSize) {
	this.batchSize = batchSize;
    }

    private class Worker extends Thread {

	public void run() {
	    ScribeAppender<E> parent = ScribeAppender.this;

	    // loop while the parent is started
	    List<LogEntryRecord> logEntries = new ArrayList<LogEntryRecord>();
	    while (parent.isStarted()) {
		try {
		    LogEntryRecord logEvent = parent.blockingQueue.take();
		    do {
			logEntries.add(logEvent);
			logEvent = parent.blockingQueue.poll();
		    } while (logEvent != null && logEntries.size() < parent.batchSize);
		    flush(logEntries);
		} catch (InterruptedException ie) {
		    break;
		}
	    }

	    addInfo("Worker thread will flush remaining events before exiting. ");
	    LogEntryRecord logEvent = parent.blockingQueue.poll();
	    while (logEvent != null) {
		logEntries.add(logEvent);
		logEvent = parent.blockingQueue.poll();
	    }
	    try {
		flush(logEntries);
	    } catch (InterruptedException ie) {
		addError("Failed in final flush: " + ie.getMessage());
	    }
	}

	private boolean flush(List<LogEntryRecord> logEntries) throws InterruptedException {
	    try {
		List<LogEntry> l = new ArrayList<LogEntry>();
		for (LogEntryRecord entry : logEntries) l.add(entry.get());
		ResultCode resultCode = client.Log(l);
		if (resultCode == ResultCode.OK) {
		    for (LogEntryRecord entry : logEntries) {
			entry.remove();
		    }
		    logEntries.clear();
		    return true;
		}
	    } catch (TException e) {
		addError(e.getMessage());
		try {
		    reconnect();
		} catch (Exception re) {
		    addError(e.getMessage());
		} finally {
		    if (!transport.isOpen()) Thread.sleep(retryInterval);
		}
	    }
	    return false;
	}
	
    }

}

