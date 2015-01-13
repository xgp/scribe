package scribe.logback;

import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.encoder.Encoder;

/**
 * Converts an ILoggingEvent to a string based on a given PatternLayoutEncoder's layout.
 * @author garth
 */
public class ScribeConverter<E> {

    protected PatternLayoutEncoder encoder;

    public ScribeConverter(PatternLayoutEncoder encoder) {
	this.encoder = encoder;
    }

    public String getMessage(E logEvent) {
        ILoggingEvent eventObject = (ILoggingEvent) logEvent;
        // String message = String.format("%s\n", eventObject.getMessage());
        String message = encoder.getLayout().doLayout(eventObject);
        return message;
    }

}
