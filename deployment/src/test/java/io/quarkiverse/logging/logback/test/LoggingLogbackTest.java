package io.quarkiverse.logging.logback.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class LoggingLogbackTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("quarkus-logback.xml", "logback.xml"));

    @Test
    public void testLogFile() throws IOException {
        for (String line : Files.readAllLines(Paths.get("target/tests.log"))) {
            Assertions.assertTrue(line.startsWith("LOGBACK"));
        }
    }
}
