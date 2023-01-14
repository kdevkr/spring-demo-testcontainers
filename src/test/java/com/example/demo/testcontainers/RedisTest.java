package com.example.demo.testcontainers;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisCommandExecutionException;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

@Slf4j
@DisplayName("레디스 통합 테스트")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class RedisTest {

    private static final int REDIS_PORT = 6379;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:6.2-alpine"))
            .withClasspathResourceMapping("conf/redis.conf", "/usr/local/etc/redis/redis.conf", BindMode.READ_ONLY)
            .withCommand("redis-server /usr/local/etc/redis/redis.conf")
            .withExposedPorts(REDIS_PORT);

    @Autowired
    private RedisProperties redisProperties;

    private RedisClient redisClient;

    @BeforeEach
    void prepareRedisClient() {
        redisClient = RedisClient.create(RedisURI.builder()
                .withHost(redisProperties.getHost())
                .withPort(redisProperties.getPort())
                .withPassword("redispass".toCharArray())
                .withDatabase(0)
                .build());
    }

    @DynamicPropertySource
    static void registerRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.redis.host", () -> redis.getHost());
        registry.add("spring.redis.port", () -> redis.getMappedPort(REDIS_PORT));
    }

    @Order(0)
    @DisplayName("버전 체크")
    @Test
    void TestVersion() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            String server = connection.sync().info("server");
            Assertions.assertNotNull(server);
            Assertions.assertTrue(server.contains("redis_version:6.2"));

            // [NOTE] 로그 확인 시 주석을 해제하세요.
//            log.info("[INFO]\n{}", connection.sync().info());
        }
    }

    @Order(1)
    @DisplayName("비활성화 명령어 체크")
    @Test
    void TestCallDisabledCommand() {
        try (StatefulRedisConnection<String, String> connection = redisClient.connect()) {
            Assertions.assertThrows(RedisCommandExecutionException.class, () -> {
                List<String> allKeys = connection.sync().keys("*");
                Assertions.assertNull(allKeys);
            });
        }
    }
}
