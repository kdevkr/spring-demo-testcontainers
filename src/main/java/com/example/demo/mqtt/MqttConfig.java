package com.example.demo.mqtt;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

import java.util.UUID;

@Slf4j
@Configuration
public class MqttConfig {
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel();
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler handler() {
        return message -> log.info("{}", message.getPayload());
    }

    @Bean
    public MessageProducer inboundChannel(MqttProperties mqttProperties) {

        String host = mqttProperties.getHost();
        Integer port = mqttProperties.getPort();

        String topic        = "test";
        String broker       = "tcp://%s:%s".formatted(host, port);

        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(broker, UUID.randomUUID().toString(), topic);
        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter(mqttProperties.getQos(), false));
        adapter.setQos(mqttProperties.getQos());
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }
}
