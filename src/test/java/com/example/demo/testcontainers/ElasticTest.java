package com.example.demo.testcontainers;


import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.core.MainResponse;
import org.elasticsearch.client.indices.*;
import org.elasticsearch.cluster.metadata.ComposableIndexTemplate;
import org.elasticsearch.cluster.metadata.MappingMetadata;
import org.elasticsearch.cluster.metadata.Template;
import org.elasticsearch.common.compress.CompressedXContent;
import org.elasticsearch.common.settings.Settings;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.elasticsearch.DataElasticsearchTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.StreamUtils;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.shaded.com.google.common.collect.MapDifference;
import org.testcontainers.shaded.com.google.common.collect.Maps;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@DisplayName("엘라스틱서치 통합 테스트")
@TestMethodOrder(value = MethodOrderer.OrderAnnotation.class)
@DataElasticsearchTest
@Testcontainers
class ElasticTest {

    private static final String ELASTICSEARCH_VERSION = "7.17.8";
    private static final String ELASTICSEARCH_IMAGE = "docker.elastic.co/elasticsearch/elasticsearch:%s".formatted(ELASTICSEARCH_VERSION);

    @Container
    private static final ElasticsearchContainer elasticsearch =
            new ElasticsearchContainer(DockerImageName.parse(ELASTICSEARCH_IMAGE))
                    .withEnv("discovery.type", "single-node")
                    .withEnv("bootstrap.memory_lock", "true")
                    .withEnv("ELASTIC_PASSWORD", "elasticpass")
                    .withEnv("xpack.security.enabled", "true")
                    .withExposedPorts(9200, 9300);

    @DynamicPropertySource
    static void registerEsProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.elasticsearch.uris", elasticsearch::getHttpHostAddress);
        registry.add("spring.elasticsearch.username", () -> "elastic");
        registry.add("spring.elasticsearch.password", () -> "elasticpass");
    }

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Order(0)
    @DisplayName("버전 체크")
    @Test
    void TestVersion() {
        Assertions.assertDoesNotThrow(() -> {
            MainResponse response = restHighLevelClient.info(RequestOptions.DEFAULT);
            Assertions.assertNotNull(response);
            Assertions.assertEquals("docker-cluster", response.getClusterName());

            MainResponse.Version version = response.getVersion();
            Assertions.assertNotNull(version);
            Assertions.assertEquals(ELASTICSEARCH_VERSION, version.getNumber());
        });
    }

    @Order(1)
    @DisplayName("인덱스 템플릿 추가")
    @Test
    void TestPutIndexTemplate() {
        Assertions.assertDoesNotThrow(() -> {
            String mappingJson = StreamUtils.copyToString(new ClassPathResource("mappings/access_log.json").getInputStream(), StandardCharsets.UTF_8);
            PutComposableIndexTemplateRequest request = new PutComposableIndexTemplateRequest().name("access_log-template");

            Settings settings = Settings.builder()
                    .put("index.number_of_shards", 3)
                    .put("index.number_of_replicas", 0)
                    .build();
            Template template = new Template(settings, new CompressedXContent(mappingJson), null);
            ComposableIndexTemplate composableIndexTemplate = new ComposableIndexTemplate(List.of("access_log-*"), template
                    , null, null, null, null);
            request.indexTemplate(composableIndexTemplate);

            AcknowledgedResponse response = restHighLevelClient.indices().putIndexTemplate(request, RequestOptions.DEFAULT);
            Assertions.assertTrue(response.isAcknowledged());
        });
    }

    @Order(2)
    @DisplayName("인덱스 생성")
    @Test
    void TestCreateIndex() {
        Assertions.assertDoesNotThrow(() -> {
            CreateIndexRequest indexRequest = new CreateIndexRequest("access_log-" + DateTimeFormatter.ofPattern("yyyyMM").format(ZonedDateTime.now()));
            CreateIndexResponse indexResponse = restHighLevelClient.indices().create(indexRequest, RequestOptions.DEFAULT);

            Assertions.assertTrue(indexResponse.isAcknowledged());
            log.info("Created index: {}", indexResponse.index());
        });
    }

    @Order(3)
    @DisplayName("인덱스 매핑 정보 확인")
    @Test
    void TestCheckIndexMappings() {
        Assertions.assertDoesNotThrow(() -> {
            GetMappingsRequest request = new GetMappingsRequest();
            request.indices("access_log-*");
            GetMappingsResponse response = restHighLevelClient.indices().getMapping(request, RequestOptions.DEFAULT);
            Assertions.assertFalse(response.mappings().isEmpty());

            // when: if exists access_log-yyyyMM index
            Set<String> indices = response.mappings().keySet();
            Optional<String> index = indices.stream().findAny();
            Assertions.assertTrue(index.isPresent());

            MappingMetadata mappingMetadata = response.mappings().get(index.get());
            String jsonStr = StreamUtils.copyToString(new ClassPathResource("mappings/access_log.json").getInputStream(), StandardCharsets.UTF_8);
            MapDifference mapDiff = Maps.difference(mappingMetadata.sourceAsMap(), new ObjectMapper().readValue(jsonStr, new TypeReference<Map<String, Object>>() {
            }));

            Assertions.assertTrue(mapDiff.areEqual());
        });
    }


}
