package scribe;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import java.io.File;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;

public class Scribed {

    public static void main(String[] argv) {
	try {
	    final Scribed scribed = new Scribed();
	    CmdLineParser parser = new CmdLineParser(scribed);
	    parser.setUsageWidth(80);
	    parser.parseArgument(argv);
	    scribed.validate();
	    scribed.start();
	    Runtime.getRuntime().addShutdownHook(new Thread() {
		    public void run() {
			scribed.stop();
		    }
		});
	    while (true) Thread.sleep(24*60*60*1000);
	} catch (Exception e) {
	    e.printStackTrace();
	    System.exit(1);
	}
    }

    public Scribed() {}

    public Scribed(int port, File config) {
	this.port = port;
	this.config = config;
    }

    @Option(name = "-d", aliases = { "-dort" }, required = false, usage = "Debug flag")
    private boolean debug = false;
    @Option(name = "-p", aliases = { "-port" }, required = true, usage = "Port to listen")
    private int port;
    @Option(name = "-c", aliases = { "-config" }, required = true, usage = "Logback configuration file")
    private File config;

    private LoggerContext loggerContext;
    private LogHandler handler;
    private ScribeServer server;

    public void validate() throws Exception {
	if (config == null || !config.isFile()) {
	    throw new IllegalArgumentException("" + config + " is not a valid input file.");
	}
	if (port < 0 || port > 65535) {
	    throw new IllegalArgumentException("Port " + port + " is out of range.");
	}
    }	
    
    public void start() throws Exception {
	loggerContext = configureLogging();
	handler = new LogHandler();
	server = new ScribeServer(port, handler);	
	server.start();
    }

    private LoggerContext configureLogging() throws Exception {
	// assume SLF4J is bound to logback in the current environment
	LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	// configure our logging system
	JoranConfigurator configurator = new JoranConfigurator();
	configurator.setContext(lc);
	// the context was probably already configured by default configuration rules
	lc.reset();
	configurator.doConfigure(config);
	if (debug) StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
	return lc;
    }

    public void stop() {
	server.stop();
	loggerContext.stop();
        if (debug) StatusPrinter.printInCaseOfErrorsOrWarnings(loggerContext);
    }

    private void err(String message, Object... args) {
	if (debug) System.err.println(String.format(message, args));
    }
}
