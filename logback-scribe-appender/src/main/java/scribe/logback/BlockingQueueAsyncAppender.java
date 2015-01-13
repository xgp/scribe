package ch.qos.logback.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * Derived from ch.qos.logback.core.AsyncAppenderBase and ch.qos.logback.core.AsyncAppender
 * to allow specifying a BlockingQueue implementation to be used.
 * @author garth
 */
public class BlockingQueueAsyncAppender extends UnsynchronizedAppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent> {

    AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<ILoggingEvent>();
    BlockingQueue<ILoggingEvent> blockingQueue;

    int appenderCount = 0;
    static final int UNDEFINED = -1;
    int discardingThreshold = UNDEFINED;
    Worker worker = new Worker();
    boolean includeCallerData = false;
    Level discardingLevel = Level.INFO;

    /**
     * @param event
     * @return true if the event is of level <= discardingLevel
     */
    protected boolean isDiscardable(ILoggingEvent event) {
	Level level = event.getLevel();
	return level.toInt() <= discardingLevel.toInt();
    }

    public Level getDiscardingLevel() {
	return this.discardingLevel;
    }
    
    public void setDiscardinglevel(Level discardingLevel) {
	this.discardingLevel = discardingLevel;
    }

    protected void preprocess(ILoggingEvent eventObject) {
	eventObject.prepareForDeferredProcessing();
	if(includeCallerData) eventObject.getCallerData();
    }

    public boolean isIncludeCallerData() {
	return includeCallerData;
    }

    public void setIncludeCallerData(boolean includeCallerData) {
	this.includeCallerData = includeCallerData;
    }

    public BlockingQueue<ILoggingEvent> getBlockingQueue() {
	return this.blockingQueue;
    }
    
    public void setBlockingQueue(BlockingQueue<ILoggingEvent> blockingQueue) {
	this.blockingQueue = blockingQueue;
    }
    
    @Override
    public void start() {
	if (appenderCount == 0) {
	    addError("No attached appenders found.");
	    return;
	}
	//blockingQueue = new ArrayBlockingQueue<E>(queueSize);
	
	addInfo("Setting discardingThreshold to " + discardingThreshold);
	worker.setDaemon(true);
	worker.setName("AsyncAppender-Worker-" + worker.getName());
	// make sure this instance is marked as "started" before staring the worker Thread
	super.start();
	worker.start();
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
	    worker.join(1000);
	} catch (InterruptedException e) {
	    addError("Failed to join worker thread", e);
	}
    }


    @Override
    protected void append(ILoggingEvent eventObject) {
	if (isQueueBelowDiscardingThreshold() && isDiscardable(eventObject)) {
	    return;
	}
	preprocess(eventObject);
	put(eventObject);
    }

    private boolean isQueueBelowDiscardingThreshold() {
	if (discardingThreshold == UNDEFINED) return false;
	return (blockingQueue.remainingCapacity() < discardingThreshold);
    }

    private void put(ILoggingEvent eventObject) {
	try {
	    blockingQueue.put(eventObject);
	} catch (InterruptedException e) {
	}
    }

    public int getDiscardingThreshold() {
	return discardingThreshold;
    }

    public void setDiscardingThreshold(int discardingThreshold) {
	this.discardingThreshold = discardingThreshold;
    }

    /**
     * Returns the number of elements currently in the blocking queue.
     *
     * @return number of elements currently in the queue.
     */
    public int getNumberOfElementsInQueue() {
	return blockingQueue.size();
    }

    /**
     * The remaining capacity available in the blocking queue.
     *
     * @return the remaining capacity
     * @see {@link java.util.concurrent.BlockingQueue#remainingCapacity()}
     */
    public int getRemainingCapacity() {
	return blockingQueue.remainingCapacity();
    }

    public void addAppender(Appender<ILoggingEvent> newAppender) {
	if (appenderCount == 0) {
	    appenderCount++;
	    addInfo("Attaching appender named ["+newAppender.getName()+"] to AsyncAppender.");
	    aai.addAppender(newAppender);
	} else {
	    addWarn("One and only one appender may be attached to AsyncAppender.");
	    addWarn("Ignoring additional appender named [" + newAppender.getName() + "]");
	}
    }

    public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
	return aai.iteratorForAppenders();
    }

    public Appender<ILoggingEvent> getAppender(String name) {
	return aai.getAppender(name);
    }

    public boolean isAttached(Appender<ILoggingEvent> eAppender) {
	return aai.isAttached(eAppender);
    }

    public void detachAndStopAllAppenders() {
	aai.detachAndStopAllAppenders();
    }

    public boolean detachAppender(Appender<ILoggingEvent> eAppender) {
	return aai.detachAppender(eAppender);
    }

    public boolean detachAppender(String name) {
	return aai.detachAppender(name);
    }

    class Worker extends Thread {
	public void run() {
	    BlockingQueueAsyncAppender parent = BlockingQueueAsyncAppender.this;
	    AppenderAttachableImpl<ILoggingEvent> aai = parent.aai;

	    // loop while the parent is started
	    while (parent.isStarted()) {
		try {
		    ILoggingEvent e = parent.blockingQueue.take();
		    aai.appendLoopOnAppenders(e);
		} catch (InterruptedException ie) {
		    break;
		}
	    }

	    addInfo("Worker thread will flush remaining events before exiting. ");
	    for (ILoggingEvent e : parent.blockingQueue) {
		aai.appendLoopOnAppenders(e);
	    }

	    aai.detachAndStopAllAppenders();
	}
    }

}
