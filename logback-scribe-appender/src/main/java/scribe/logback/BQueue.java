package scribe.logback;

import scribe.thrift.LogEntry;

/**
 * Simplified blocking queue. Only offer(e), take() and poll().
 */
public interface BQueue {
    public boolean offer(LogEntry e);
    public LogEntryRecord take() throws InterruptedException;
    public LogEntryRecord poll();
}
