package io.quarkiverse.logging.logback.test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusDevModeTest;
import io.restassured.RestAssured;

public class LoggingLogbackExpressionsDevModeTest {

    // Start hot reload (DevMode) test with your extension loaded
    @RegisterExtension
    static final QuarkusDevModeTest devModeTest = new QuarkusDevModeTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class)
                    .addAsResource(new StringAsset("lg.pre=EXP"), "application.properties")
                    .addAsResource("quarkus-logback-expressions.xml", "logback.xml"));

    @Test
    public void testLogbackXmlChanges() throws IOException {
        for (String line : Files.readAllLines(Paths.get("target/tests.log"))) {
            Assertions.assertTrue(line.startsWith("EXP"));
        }
        devModeTest.modifyResourceFile("application.properties", s -> s.replaceAll("EXP", "MODIFIED"));
        RestAssured.get(); //trigger hot reload
        boolean foundMod = false;
        for (String line : Files.readAllLines(Paths.get("target/tests.log"))) {
            if (line.startsWith("MODIFIED")) {
                foundMod = true;
                break;
            }
        }
        Assertions.assertTrue(foundMod, "Log changes did not take effect");
    }
}
