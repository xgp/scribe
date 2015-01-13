package scribe.logback;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import scribe.thrift.LogEntry;

/**
 * BQueue backed by LinkedBlockingQueue. Entries are lost when the process dies.
 */
public class MemoryQueue implements BQueue {

    private final BlockingQueue<LogEntry> queue;

    public MemoryQueue() {
	this.queue = new LinkedBlockingQueue<LogEntry>();
    }
    
    public boolean offer(LogEntry e) {
	return queue.offer(e);
    }

    public LogEntryRecord take() throws InterruptedException {
	return new LogEntryRecord(queue.take());
    }

    public LogEntryRecord poll() {
	LogEntry e =  queue.poll();
	if (e == null) return null;
	else return new LogEntryRecord(e);
    }

}
