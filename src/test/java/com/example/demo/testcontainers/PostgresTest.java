package com.example.demo.testcontainers;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@ActiveProfiles("test")
@Slf4j
@DisplayName("포스트그레스 통합 테스트")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class PostgresTest {

    private static final String TEST_CONTAINER_JDBC_DRIVER = org.postgresql.Driver.class.getName();
    private static final int POSTGRES_PORT = 5432;

    @Container
    private static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:14.6-alpine"))
            .withDatabaseName("test")
            .withUsername("test_user")
            .withPassword("84z$Vw8&")
            .withClasspathResourceMapping("db/parameters.sql", "/docker-entrypoint-initdb.d/parameters.sql", BindMode.READ_ONLY)
            .withInitScript("db/init_postgres.sql")
            .withExposedPorts(POSTGRES_PORT);

    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", () -> TEST_CONTAINER_JDBC_DRIVER);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
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

    @Order(2)
    @DisplayName("옵션 설정 체크")
    @Test
    void TestConfig() {
        Integer maxConnections = jdbcTemplate.queryForObject("SELECT current_setting('max_connections')", Integer.class);
        Assertions.assertNotNull(maxConnections);
        Assertions.assertEquals(500, maxConnections);

        String fsync = jdbcTemplate.queryForObject("SELECT current_setting('fsync')", String.class);
        Assertions.assertEquals("off", fsync);
    }

    @Order(3)
    @DisplayName("초기 스크립트 검증")
    @Test
    void TestInitScript() {
        String extname = jdbcTemplate.queryForObject("SELECT extname FROM pg_extension where extname = 'pg_stat_statements'", String.class);
        Assertions.assertNotNull(extname);
        Assertions.assertEquals("pg_stat_statements", extname);
    }

}
