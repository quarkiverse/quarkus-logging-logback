package io.quarkiverse.logback.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Handler;

import org.jboss.logmanager.ExtHandler;
import org.jboss.logmanager.ExtLogRecord;
import org.slf4j.helpers.Util;
import org.xml.sax.helpers.AttributesImpl;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LogbackRecorder {

    public static final String DELAYED = "$$delayed";
    private static volatile LoggerContext defaultLoggerContext;

    public static final List<DelayedStart> DELAYED_START_HANDLERS = new ArrayList<>();

    public void init(List<SaxEvent> configEvents, Set<String> delayedStartClasses) {
        if (defaultLoggerContext == null) {
            for (SaxEvent i : configEvents) {
                if (i instanceof StartEvent) {
                    AttributesImpl impl = (AttributesImpl) ((StartEvent) i).attributes;
                    int index = impl.getIndex("class");
                    if (index > -1) {
                        String val = impl.getValue(index);
                        if (delayedStartClasses.contains(val)) {
                            impl.setValue(index, val + DELAYED);
                        }
                    }
                }
            }
            defaultLoggerContext = new LoggerContext();
            try {
                JoranConfigurator configurator = new JoranConfigurator();
                configurator.setContext(defaultLoggerContext);
                configurator.doConfigure(configEvents);
                // logback-292
                if (!StatusUtil.contextHasStatusListener(defaultLoggerContext)) {
                    StatusPrinter.printInCaseOfErrorsOrWarnings(defaultLoggerContext);
                }
            } catch (Exception t) { // see LOGBACK-1159
                Util.report("Failed to instantiate [" + LoggerContext.class.getName() + "]", t);
            }
        }
    }

    public RuntimeValue<Optional<Handler>> createHandler() {
        for (DelayedStart i : DELAYED_START_HANDLERS) {
            i.doQuarkusDelayedStart();
        }
        DELAYED_START_HANDLERS.clear();
        return new RuntimeValue<>(Optional.of(new ExtHandler() {

            @Override
            public final void doPublish(final ExtLogRecord record) {
                if (defaultLoggerContext == null) {
                    return;
                }
                Logger logger = defaultLoggerContext.getLogger(record.getLoggerName());
                logger.callAppenders(new LoggingEventWrapper(record, getFormatter()));
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        }));

    }

}
