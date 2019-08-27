package net.spy.memcached.compat.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper of SLF4J SimpleLogger for Memcached logging.
 * <p>
 * Written in Java because it's easier to create a constructor recognised
 * by memcached loggerfactory's reflection than in Scala.
 */
public class SimpleLoggerWrapper extends AbstractLogger {

    private Logger logger = LoggerFactory.getLogger(getName());

    public SimpleLoggerWrapper(String nm) {
        super(nm);
    }

    @Override
    public boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    @Override
    public boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isInfoEnabled();
    }

    @Override
    public void log(Level level, Object message, Throwable e) {
        switch (level) {
            case TRACE:
                logger.trace(message.toString(), e);
                break;
            case DEBUG:
                logger.debug(message.toString(), e);
                break;
            case INFO:
                logger.info(message.toString(), e);
                break;
            case WARN:
                logger.warn(message.toString(), e);
                break;
            case ERROR:
            case FATAL:
                logger.error(message.toString(), e);
                break;
        }
    }
}
