package io.quarkiverse.logback.runtime;

import java.util.Map;
import java.util.logging.Formatter;

import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.Marker;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.ThrowableProxy;

public class LoggingEventWrapper implements ILoggingEvent {

    final ExtLogRecord logRecord;
    final Formatter formatter;

    public LoggingEventWrapper(ExtLogRecord logRecord, Formatter formatter) {
        this.logRecord = logRecord;
        this.formatter = formatter;
    }

    @Override
    public String getThreadName() {
        return logRecord.getThreadName();
    }

    @Override
    public Level getLevel() {
        if (logRecord.getLevel().intValue() >= org.jboss.logmanager.Level.ERROR.intValue()) {
            return Level.ERROR;
        } else if (logRecord.getLevel().intValue() >= org.jboss.logmanager.Level.WARNING.intValue()) {
            return Level.WARN;
        } else if (logRecord.getLevel().intValue() >= org.jboss.logmanager.Level.INFO.intValue()) {
            return Level.INFO;
        } else if (logRecord.getLevel().intValue() >= org.jboss.logmanager.Level.DEBUG.intValue()) {
            return Level.DEBUG;
        } else if (logRecord.getLevel().intValue() >= org.jboss.logmanager.Level.TRACE.intValue()) {
            return Level.TRACE;
        }
        return Level.OFF;
    }

    @Override
    public String getMessage() {
        return logRecord.getMessage();
    }

    @Override
    public Object[] getArgumentArray() {
        return logRecord.getParameters();
    }

    @Override
    public String getFormattedMessage() {
        return logRecord.getFormattedMessage();
    }

    @Override
    public String getLoggerName() {
        return logRecord.getLoggerName();
    }

    @Override
    public LoggerContextVO getLoggerContextVO() {
        return LogbackRecorder.defaultLoggerContext.getLoggerContextRemoteView();
    }

    @Override
    public IThrowableProxy getThrowableProxy() {
        if (logRecord.getThrown() != null) {
            return new ThrowableProxy(logRecord.getThrown());
        }
        return null;
    }

    @Override
    public StackTraceElement[] getCallerData() {
        return null;
    }

    @Override
    public boolean hasCallerData() {
        return false;
    }

    @Override
    public Marker getMarker() {
        return (Marker) logRecord.getMarker();
    }

    @Override
    public Map<String, String> getMDCPropertyMap() {
        return logRecord.getMdcCopy();
    }

    @Override
    public Map<String, String> getMdc() {
        return getMDCPropertyMap();
    }

    @Override
    public long getTimeStamp() {
        return logRecord.getMillis();
    }

    @Override
    public void prepareForDeferredProcessing() {

    }
}
