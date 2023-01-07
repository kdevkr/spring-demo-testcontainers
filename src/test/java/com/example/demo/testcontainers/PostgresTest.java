package com.example.demo.testcontainers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

@Slf4j
@DisplayName("포스트그레스 통합 테스트")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PostgresTest {

    private static final String TEST_CONTAINER_JDBC_DRIVER = "org.testcontainers.jdbc.ContainerDatabaseDriver";
    private static final int POSTGRES_PORT = 5432;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.6-alpine"))
            .withDatabaseName("test")
            .withUsername("test_user")
            .withPassword(UUID.randomUUID().toString())
            .withExposedPorts(POSTGRES_PORT);

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> TEST_CONTAINER_JDBC_DRIVER);
        registry.add("spring.datasource.url",
                () -> "jdbc:tc:postgresql:14.6:///%s:%s/%s?TC_INITSCRIPT=db/init_postgres.sql".formatted(
                        postgres.getHost(),
                        postgres.getMappedPort(POSTGRES_PORT),
                        postgres.getDatabaseName()));
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
    }


    @Autowired
    private DataSourceProperties dataSourceProperties;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Order(0)
    @DisplayName("JDBC 드라이버 체크")
    @Test
    void TestJdbcDriver() {
        Assertions.assertEquals(TEST_CONTAINER_JDBC_DRIVER, dataSourceProperties.getDriverClassName());
    }

    @Order(1)
    @DisplayName("버전 체크")
    @Test
    void TestVersion() {
        String version = jdbcTemplate.queryForObject("SELECT version()", String.class);
        Assertions.assertNotNull(version);
        Assertions.assertTrue(version.startsWith("PostgreSQL 14.6"));
    }

}
