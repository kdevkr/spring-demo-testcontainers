package com.example.demo.testcontainers;


import com.example.demo.mqtt.MqttProperties;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;
import java.util.stream.IntStream;

@Slf4j
@DisplayName("MQTT 통합 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class MqttTest {

    private static final int MOSQUITTO_PORT = 1883;

    @Container
    private static final GenericContainer<?> mosquitto =
            new GenericContainer<>(DockerImageName.parse("eclipse-mosquitto:2.0.10"))
                    .withClasspathResourceMapping("conf/mosquitto.conf", "/mosquitto/config/mosquitto.conf", BindMode.READ_ONLY)
                    .withExposedPorts(MOSQUITTO_PORT);

    @DynamicPropertySource
    static void registerMqttProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.mqtt.host", mosquitto::getHost);
        registry.add("spring.mqtt.port", () -> mosquitto.getMappedPort(MOSQUITTO_PORT));
    }

    @Autowired
    private MqttProperties mqttProperties;

    @DisplayName("Mqtt Broker 버전")
    @Test
    void TestBrokerVersion() {

        String host = mqttProperties.getHost();
        Integer port = mqttProperties.getPort();

        String topic        = "test";
        String content      = "Simple Message";
        String broker       = "tcp://%s:%s".formatted(host, port);

        Assertions.assertDoesNotThrow(() -> {
            DefaultMqttPahoClientFactory clientFactory = new DefaultMqttPahoClientFactory();
            MqttConnectOptions connectOptions = new MqttConnectOptions();
            connectOptions.setCleanSession(true);
            // [NOTE] Not require authentication for test
//            connectOptions.setUserName(mqttProperties.getUsername());
//            connectOptions.setPassword(mqttProperties.getPassword().toCharArray());

            clientFactory.setConnectionOptions(connectOptions);
            clientFactory.setPersistence(new MemoryPersistence());
            MqttClient mqttClient = (MqttClient) clientFactory.getClientInstance(broker, UUID.randomUUID().toString());
            mqttClient.connect(clientFactory.getConnectionOptions());

            IntStream.range(0, 10).forEach(value -> {
                MqttMessage message = new MqttMessage((content + " - " +  value).getBytes());
                message.setQos(mqttProperties.getQos());
                try {
                    mqttClient.publish(topic, message);
                } catch (MqttException e) {
                    log.error(e.getMessage());
                }
            });
        });
    }

}
