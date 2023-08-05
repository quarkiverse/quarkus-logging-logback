package io.quarkiverse.logging.logback.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingLogbackTimeBasedRollingPolicyTest {

    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("quarkus-logback-TimeBasedRollingPolicy.xml", "logback.xml"));

    @Test
    public void testLogFile() throws IOException {
        List<String> strings = Files.readAllLines(Paths.get("target/tests-TimeBasedRollingPolicy.log"));
        Assertions.assertFalse(strings.isEmpty());
        for (String line : strings) {
            Assertions.assertTrue(line.startsWith("LOGBACK TimeBasedRollingPolicy"));
        }
    }
}
