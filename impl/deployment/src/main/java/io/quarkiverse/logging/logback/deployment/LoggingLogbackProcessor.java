package io.quarkiverse.logging.logback.deployment;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

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
import io.quarkus.deployment.pkg.builditem.CurateOutcomeBuildItem;
import io.quarkus.deployment.pkg.builditem.OutputTargetBuildItem;
import io.quarkus.deployment.recording.RecorderContext;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.smallrye.common.version.VersionScheme;

class LoggingLogbackProcessor {

    private static final Logger log = Logger.getLogger(LoggingLogbackProcessor.class);

    private static final String FEATURE = "logging-logback";
    public static final String PROJECT_VERSION = "project.version";

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
            OutputTargetBuildItem outputTargetBuildItem,
            CurateOutcomeBuildItem curateOutcomeBuildItem,
            ShutdownContextBuildItem shutdownContextBuildItem)
            throws Exception {
        //first check the versions
        doVersionCheck();

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
                start.invokeStaticMethod(
                        MethodDescriptor.ofMethod(LogbackRecorder.class, "addDelayed", void.class, DelayedStart.class),
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
                    "quarkus.log.category.\"" + e.getKey() + "\".level", e.getValue()));
        }

        Map<String, String> buildProperties = new HashMap<>(outputTargetBuildItem.getBuildSystemProperties()
                .entrySet().stream().collect(Collectors.toMap(Object::toString, Object::toString)));
        buildProperties.put(PROJECT_VERSION, curateOutcomeBuildItem.getEffectiveModel().getAppArtifact().getVersion());
        recorder.init(events.get(), delayedClasses, shutdownContextBuildItem, buildProperties);
    }

    private void doVersionCheck() throws IOException {
        //if the versions are wrong you get really hard to understand errors
        //easier to just verify this ourselves
        VersionScheme versionScheme = VersionScheme.MAVEN;
        String compiledVersion;
        String coreVersion = null;
        String classicVersion = null;
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("quarkus-logback-version.txt")) {
            compiledVersion = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("META-INF/maven/ch.qos.logback/logback-core/pom.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                coreVersion = p.getProperty("version");
            }
        }
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("META-INF/maven/ch.qos.logback/logback-classic/pom.properties")) {
            if (in != null) {
                Properties p = new Properties();
                p.load(in);
                classicVersion = p.getProperty("version");
            }
        }
        if (coreVersion != null) {
            if (versionScheme.compare(coreVersion, compiledVersion) < 0) {
                throw new RuntimeException("ch.qos.logback:logback-core version " + coreVersion
                        + " is not compatible with quarkus-logback which requires at least " + compiledVersion
                        + " please use the correct logback version");
            }
            if (classicVersion != null) {
                if (versionScheme.compare(classicVersion, coreVersion) != 0) {
                    throw new RuntimeException("logback-core(" + coreVersion + ") and logback-classic(" + classicVersion
                            + ") versions must match");
                }
            }
        } else {
            log.warn("Could not determine logback version on class path");
        }
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem handler(LogbackRecorder recorder) {
        return new LogHandlerBuildItem(recorder.createHandler());
    }

    private URL getUrl() {
        ContextInitializer contextInitializer = new ContextInitializer(new LoggerContext());
        URL url = contextInitializer.findURLOfDefaultConfigurationFile(true);
        if (url != null) {
            // Check that file exists at URL
            if (Files.notExists(Paths.get(url.getPath()))) {
                log.warn("Logback configuration file not found at " + url + ", using default configuration");
            } else {
                return url;
            }
        }
        url = Loader.getResource(ContextInitializer.TEST_AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
        if (url != null) {
            return url;
        }
        return Loader.getResource(ContextInitializer.AUTOCONFIG_FILE, Thread.currentThread().getContextClassLoader());
    }
}
