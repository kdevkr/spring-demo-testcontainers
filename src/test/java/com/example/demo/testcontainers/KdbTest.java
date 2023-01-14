package com.example.demo.testcontainers;

import com.kx.c;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.TimeZone;

@Slf4j
@DisplayName("KDB+ 통합 테스트")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class KdbTest {

    private static final int KDB_PORT = 5000;

    @Container
    private static final GenericContainer<?> kdb =
            new GenericContainer<>(new ImageFromDockerfile().withFileFromClasspath("Dockerfile", "docker/kdb/Dockerfile")
                    .withFileFromClasspath("entrypoint.sh", "docker/kdb/entrypoint.sh")
                    .withFileFromClasspath("q.q", "docker/kdb/q.q"))
                    .withClasspathResourceMapping("docker/kdb/kc.lic", "/opt/kx/kc.lic", BindMode.READ_ONLY)
                    .withExposedPorts(KDB_PORT);

    private c c;

    @BeforeEach
    void prepareClient() {
        String host = kdb.getHost();
        Integer port = kdb.getMappedPort(KDB_PORT);
        Assertions.assertDoesNotThrow(() -> {
            c = new c(host, port);
        });
    }

    @AfterEach
    void destroyClient() {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    @Order(0)
    @DisplayName("버전 체크")
    @Test
    void TestVersion() {
        Assertions.assertDoesNotThrow(() -> {
            Object o = c.k(".z.K");
            Double version = (Double) o;
            Assertions.assertNotNull(version);
            Assertions.assertEquals(4.0, version);
        });
    }

    @Order(1)
    @DisplayName("운영체제 검증")
    @Test
    void TestOs() {
        Assertions.assertDoesNotThrow(() -> {
            Object o = c.k(".z.o");
            String os = (String) o;
            Assertions.assertNotNull(os);
            Assertions.assertEquals("l64", os);
        });
    }

    @Order(2)
    @DisplayName("UTC Timestamp")
    @Test
    void TestUtcTimestamp() {
        Assertions.assertDoesNotThrow(() -> {
            Object o = c.k(".z.p");
            Instant instant = (Instant) o;
            ZonedDateTime dateTime = ZonedDateTime.ofInstant(instant, TimeZone.getTimeZone("UTC").toZoneId());
            Assertions.assertNotNull(dateTime);
            Assertions.assertEquals(instant.toEpochMilli(), dateTime.toInstant().toEpochMilli());
        });
    }

    @Order(3)
    @DisplayName("Validation Query")
    @Test
    void TestValidationQuery() {
        Assertions.assertDoesNotThrow(() -> {
            Object o = c.k("1");
            Long valid = (Long) o;
            Assertions.assertNotNull(valid);
            Assertions.assertEquals(1L, valid);
        });
    }
}
