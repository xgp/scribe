package scribe;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.sift.Discriminator;

public class LoggerNameBasedDiscriminator implements Discriminator<ILoggingEvent> {

    private static final String KEY = "loggerName";

    private boolean started;

    @Override
    public String getDiscriminatingValue(ILoggingEvent iLoggingEvent) {
	return iLoggingEvent.getLoggerName();
    }

    @Override
    public String getKey() {
	return KEY;
    }

    public void start() {
	started = true;
    }

    public void stop() {
	started = false;
    }

    public boolean isStarted() {
	return started;
    }

}
