package io.quarkiverse.logging.logback.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class LoggingLogbackResourceTest {

    @Test
    public void testHelloEndpoint() {
        given()
                .when().get("/logging-logback")
                .then()
                .statusCode(200)
                .body(is("Hello logging-logback"));
    }
}
