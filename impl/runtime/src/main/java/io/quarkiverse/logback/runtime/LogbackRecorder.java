package io.quarkiverse.logback.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.status.StatusUtil;
import ch.qos.logback.core.util.StatusPrinter;
import io.quarkiverse.logback.runtime.events.BodySub;
import io.quarkiverse.logback.runtime.events.EventSubstitution;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.ShutdownContext;
import io.quarkus.runtime.annotations.Recorder;
import io.smallrye.common.expression.Expression;
import io.smallrye.config.ConfigValue;
import io.smallrye.config.SmallRyeConfig;
import io.smallrye.config.SmallRyeConfigProviderResolver;

@Recorder
public class LogbackRecorder {

    public static final String DELAYED = "$$delayed";
    private static volatile LoggerContext defaultLoggerContext;

    public static final List<DelayedStart> DELAYED_START_HANDLERS = new ArrayList<>();
    private static volatile boolean started;

    public static void addDelayed(DelayedStart delayedStart) {
        if (started) {
            delayedStart.doQuarkusDelayedStart();
        } else {
            DELAYED_START_HANDLERS.add(delayedStart);
        }
    }

    public void init(List<SaxEvent> originalEvents, Set<String> delayedStartClasses, ShutdownContext context,
            Map<String, String> buildSystemProps) {
        EventSubstitution substitution = new EventSubstitution();
        if (defaultLoggerContext == null) {
            SmallRyeConfig config = (SmallRyeConfig) SmallRyeConfigProviderResolver.instance().getConfig();
            List<SaxEvent> configEvents = new ArrayList<>();
            for (SaxEvent i : originalEvents) {
                if (i instanceof StartEvent) {
                    AttributesImpl impl = (AttributesImpl) ((StartEvent) i).attributes;
                    int index = impl.getIndex("class");
                    if (index > -1) {
                        String val = impl.getValue(index);
                        if (delayedStartClasses.contains(val)) {
                            impl.setValue(index, val + DELAYED);
                        }
                    }
                    for (int j = 1; j <= impl.getLength(); ++j) {
                        String val = impl.getValue(index);
                        if (val != null && val.contains("${")) {
                            final String expanded = doExpand(config, val, buildSystemProps);
                            impl.setValue(j, expanded);
                        }
                    }
                    configEvents.add(i);
                } else if (i instanceof BodyEvent) {
                    String val = ((BodyEvent) i).getText();
                    if (val.contains("${")) {
                        final String expanded = doExpand(config, val, buildSystemProps);
                        configEvents.add(substitution.deserialize(
                                new BodySub(i.getNamespaceURI(), i.getLocalName(), i.getQName(), i.getLocator(), expanded)));
                    } else {
                        configEvents.add(i);
                    }
                } else {
                    configEvents.add(i);
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
            context.addLastShutdownTask(new Runnable() {
                @Override
                public void run() {
                    defaultLoggerContext.stop();
                    defaultLoggerContext = null;
                    started = false;
                }
            });
        }
    }

    private String doExpand(SmallRyeConfig config, String val, Map<String, String> buildSystemProps) {
        Expression expression = Expression.compile(val);
        final String expanded = expression.evaluate((resolveContext, stringBuilder) -> {
            final ConfigValue resolve = config.getConfigValue(resolveContext.getKey());
            if (resolve.getValue() != null) {
                stringBuilder.append(resolve.getValue());
            } else if (buildSystemProps.containsKey(resolveContext.getKey())) {
                stringBuilder.append(buildSystemProps.get(resolveContext.getKey()));
            } else if (resolveContext.hasDefault()) {
                resolveContext.expandDefault();
            } else {
                stringBuilder.append("${" + resolveContext.getKey() + "}");
            }
        });
        return expanded;
    }

    public RuntimeValue<Optional<Handler>> createHandler() {
        started = true;
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
