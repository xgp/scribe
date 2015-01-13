package scribe.logback;

import scribe.thrift.LogEntry;

/**
 * 
 */
public class LogEntryRecord {

    protected final LogEntry entry;
    public LogEntryRecord(LogEntry entry) {
	this.entry = entry;
    }
    
    public LogEntry get() {
	return this.entry;
    }
    
    public void remove() {
	//noop
    }
}
