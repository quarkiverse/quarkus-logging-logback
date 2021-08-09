package io.quarkiverse.logging.logback.test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

public class BuildSystemPropertiesTest {

    // Start unit test with your extension loaded
    @RegisterExtension
    static final QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource("quarkus-logback-build-system.xml", "logback.xml"));

    @Test
    public void testProjectVersion() throws IOException {
        //bit of a hack, grab the current version from the pom
        File f = new File(".");
        String version = null;
        for (;;) {
            File pom = new File(f, "pom.xml");
            if (pom.exists()) {
                Matcher m = Pattern.compile("<version>(.*?)</version>").matcher(Files.readString(pom.toPath()));
                if (!m.find()) {
                    throw new RuntimeException("Could not resolve project version");
                }
                version = m.group(1);
                break;
            }
            f = f.getParentFile();
            if (f == null) {
                throw new RuntimeException("Could not resolve project pom");
            }
        }
        List<String> strings = Files.readAllLines(Paths.get("target/tests.log"));
        Assertions.assertFalse(strings.isEmpty());
        for (String line : strings) {
            Assertions.assertTrue(line.startsWith(version));
        }

    }
}
