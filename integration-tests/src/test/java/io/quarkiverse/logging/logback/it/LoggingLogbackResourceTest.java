package io.quarkiverse.logging.logback.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingLogbackResourceTest {

    @Test
    public void testHelloEndpoint() throws IOException {
        //test that the app is working properly
        given()
                .when().get("/logging-logback")
                .then()
                .statusCode(200)
                .body(is("Hello logging-logback"));

        //now test that logging config has been picked up
        String line = Files.readAllLines(Paths.get("target/tests.log")).get(0);
        Assertions.assertTrue(line.startsWith("LOGBACK"));
    }
}
