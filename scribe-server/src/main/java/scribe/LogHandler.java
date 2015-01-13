package scribe;

import com.facebook.fb303.fb_status;
import java.util.List;
import java.util.Map;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scribe.thrift.*;

public class LogHandler implements scribe.Iface {

    @Override public ResultCode Log(List<LogEntry> messages) throws TException {
	for (LogEntry entry : messages) {
	    Logger log = LoggerFactory.getLogger(entry.getCategory());
	    log.info(entry.getMessage());
	}
	return ResultCode.OK;
    }

    // fb303 noops    
    /**
     * Returns a descriptive name of the service
     */
    @Override public String getName() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Returns the version of the service
     */
    @Override public String getVersion() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Gets the status of this service
     */
    @Override public fb_status getStatus() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * User friendly description of status, such as why the service is in
     * the dead or warning state, or what is being started or stopped.
     */
    @Override public String getStatusDetails() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Gets the counters for this service
     */
    @Override public Map<String,Long> getCounters() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Gets the value of a single counter
     * 
     * @param key
     */
    @Override public long getCounter(String key) throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Sets an option
     * 
     * @param key
     * @param value
     */
    @Override public void setOption(String key, String value) throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Gets an option
     * 
     * @param key
     */
    @Override public String getOption(String key) throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Gets all options
     */
    @Override public Map<String,String> getOptions() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Returns a CPU profile over the given time interval (client and server
     * must agree on the profile format).
     * 
     * @param profileDurationInSec
     */
    @Override public String getCpuProfile(int profileDurationInSec) throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Returns the unix time that the server has been running since
     */
    @Override public long aliveSince() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Tell the server to reload its configuration, reopen log files, etc
     */
    @Override public void reinitialize() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

    /**
     * Suggest a shutdown to the server
     */
    @Override public void shutdown() throws TException {
	throw new TProtocolException(TProtocolException.NOT_IMPLEMENTED);
    }

}
