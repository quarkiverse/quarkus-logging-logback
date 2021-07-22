package io.quarkiverse.logging.logback.deployment;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.event.BodyEvent;
import ch.qos.logback.core.joran.event.EndEvent;
import ch.qos.logback.core.joran.event.SaxEvent;
import ch.qos.logback.core.joran.event.StartEvent;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.spi.LifeCycle;
import ch.qos.logback.core.util.Loader;
import io.quarkiverse.logback.runtime.DelayedStart;
import io.quarkiverse.logback.runtime.LogbackRecorder;
import io.quarkiverse.logback.runtime.events.BodySub;
import io.quarkiverse.logback.runtime.events.EndSub;
import io.quarkiverse.logback.runtime.events.EventSubstitution;
import io.quarkiverse.logback.runtime.events.StartSub;
import io.quarkus.bootstrap.model.AppArtifactKey;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.HotDeploymentWatchedFileBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;
import io.quarkus.deployment.builditem.RemovedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.ShutdownContextBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;

class LoggingLogbackProcessor {

    private static final String FEATURE = "logging-logback";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    RemovedResourceBuildItem removeSlf4jBinding() {
        return new RemovedResourceBuildItem(new AppArtifactKey("ch.qos.logback", "logback-classic", null, "jar"),
                Collections.singleton("org/slf4j/impl/StaticLoggerBinder.class"));
    }

    @BuildStep
    HotDeploymentWatchedFileBuildItem watchLogback() {
        return new HotDeploymentWatchedFileBuildItem("logback.xml");
    }

    @Record(ExecutionTime.STATIC_INIT)
    @BuildStep
    void init(LogbackRecorder recorder, RecorderContext context,
            BuildProducer<RunTimeConfigurationDefaultBuildItem> runTimeConfigurationDefaultBuildItemBuildProducer,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            ShutdownContextBuildItem shutdownContextBuildItem)
            throws JoranException {
        URL url = getUrl();
        if (url == null) {
            return;
        }
        context.registerSubstitution(StartEvent.class, StartSub.class, (Class) EventSubstitution.class);
        context.registerSubstitution(BodyEvent.class, BodySub.class, (Class) EventSubstitution.class);
        context.registerSubstitution(EndEvent.class, EndSub.class, (Class) EventSubstitution.class);
        final AtomicReference<List<SaxEvent>> events = new AtomicReference<>();

        JoranConfigurator configurator = new JoranConfigurator() {
            @Override
            public void doConfigure(List<SaxEvent> eventList) throws JoranException {
                events.set(eventList);
            }
        };
        configurator.setContext(new LoggerContext());
        configurator.doConfigure(url);

        List<String> loggerPath = Arrays.asList("configuration", "logger");
        List<String> rootPath = Arrays.asList("configuration", "root");
        String rootLevel = null;
        Map<String, String> levels = new HashMap<>();
        Set<String> allClasses = new HashSet<>();
        for (SaxEvent i : events.get()) {
            if (i instanceof StartEvent) {
                StartEvent s = ((StartEvent) i);
                if (Objects.equals(loggerPath, s.elementPath.getCopyOfPartList())) {
                    String level = s.attributes.getValue("level");
                    if (level != null) {
                        levels.put(s.attributes.getValue("name"), level);
                    }
                } else if (Objects.equals(rootPath, s.elementPath.getCopyOfPartList())) {
                    String level = s.attributes.getValue("level");
                    if (level != null) {
                        rootLevel = level;
                    }
                }
                int classIndex = s.attributes.getIndex("class");
                if (classIndex != -1) {
                    allClasses.add(s.attributes.getValue(classIndex));
                }
            }
        }

        boolean disableConsole = false;
        Set<String> delayedClasses = new HashSet<>();
        for (String i : allClasses) {
            if (i.equals("ch.qos.logback.core.ConsoleAppender")) {
                disableConsole = true;
            }
            try {
                Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(i);
                if (LifeCycle.class.isAssignableFrom(c)) {
                    delayedClasses.add(i);
                }
            } catch (ClassNotFoundException exception) {
                throw new RuntimeException(exception);
            }
        }
        if (disableConsole) {
            runTimeConfigurationDefaultBuildItemBuildProducer
                    .produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.console.enable", "false"));
        }

        for (String i : delayedClasses) {
            try (ClassCreator c = new ClassCreator(
                    new GeneratedClassGizmoAdaptor(generatedClasses,
                            (Function<String, String>) s -> s.substring(s.length() - LogbackRecorder.DELAYED.length())),
                    i + LogbackRecorder.DELAYED, null, i, DelayedStart.class.getName())) {
                MethodCreator start = c.getMethodCreator("start", void.class);
                start.invokeInterfaceMethod(MethodDescriptor.ofMethod(List.class, "add", boolean.class, Object.class),
                        start.readStaticField(FieldDescriptor.of(LogbackRecorder.class, "DELAYED_START_HANDLERS", List.class)),
                        start.getThis());
                start.returnValue(null);
                MethodCreator method = c.getMethodCreator("doQuarkusDelayedStart", void.class);
                method.invokeSpecialMethod(MethodDescriptor.ofMethod(i, "start", void.class), method.getThis());
                method.returnValue(null);
            }
        }

        if (rootLevel != null) {
            runTimeConfigurationDefaultBuildItemBuildProducer
                    .produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.level", rootLevel));
        }
        for (Map.Entry<String, String> e : levels.entrySet()) {
            runTimeConfigurationDefaultBuildItemBuildProducer.produce(new RunTimeConfigurationDefaultBuildItem(
                    "quarkus.log.categories.\\\"" + e.getKey() + "\\\".level", e.getValue()));
        }

        recorder.init(events.get(), delayedClasses, shutdownContextBuildItem);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem handler(LogbackRecorder recorder) {
        return new LogHandlerBuildItem(recorder.createHandler());
    }

    private URL getUrl() {
        URL url = Loader.getResource(ContextInitializer.TEST_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
        if (url != null) {
            return url;
        }
        return Loader.getResource(ContextInitializer.AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
    }
}
