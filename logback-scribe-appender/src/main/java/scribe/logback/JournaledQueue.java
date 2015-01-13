package scribe.logback;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.fusesource.hawtjournal.api.Journal;
import org.fusesource.hawtjournal.api.Location;
import scribe.thrift.LogEntry;

/**
 * BQueue backed by HawtJournal. Persistent to disk. Replaid from mark on startup.
 */
public class JournaledQueue implements BQueue {

    private final BlockingQueue<Location> queue;
    private final Journal journal;

    public JournaledQueue(Journal journal) {
	this.journal = journal;
	this.queue = new LinkedBlockingQueue<Location>();
	for (Location loc : journal) {
	    queue.offer(loc);
	}
    }
    
    public boolean offer(LogEntry e) {
	try {
	    return queue.offer(journal.write(ByteBuffer.wrap(serialize(e)), false));
	} catch (Exception t) {
	    throw new IllegalArgumentException(t);
	}	
    }

    public LogEntryRecord take() throws InterruptedException {
	try {
	    final Location loc = queue.take();
	    final LogEntry e = deserialize(journal.read(loc).array());
	    return new LogEntryRecord(e) {
		@Override public void remove() {
		    try {
			journal.delete(loc);
		    } catch (IOException t) {
			throw new IllegalStateException(t);
		    }
		}
	    };
	} catch (IOException t) {
	    throw new IllegalStateException(t);
	}
    }

    public LogEntryRecord poll() {
	try {
	    final Location loc = queue.poll();
	    if (loc == null) return null;
	    else {
		final LogEntry e = deserialize(journal.read(loc).array());
		return new LogEntryRecord(e) {
		    @Override public void remove() {
			try {
			    journal.delete(loc);
			} catch (IOException t) {
			    throw new IllegalStateException(t);
			}			    
		    }
		};
	    }
	} catch (IOException t) {
	    throw new IllegalStateException(t);
	}
    }

    private byte[] serialize(LogEntry e) throws IOException {
	try {
	    TSerializer ser = new TSerializer();
	    return ser.serialize(e);
	} catch (TException t) {
	    throw new IOException(t);
	}
    }

    private LogEntry deserialize(byte[] b) throws IOException {
	try {
	    TDeserializer de = new TDeserializer();
	    LogEntry e = new LogEntry();
	    de.deserialize(e, b);
	    return e;
	} catch (TException t) {
	    throw new IOException(t);
	}
    }
	    
}
