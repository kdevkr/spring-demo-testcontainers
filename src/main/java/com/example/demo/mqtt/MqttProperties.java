package com.example.demo.mqtt;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@ConfigurationProperties("spring.mqtt")
@Component
@Getter
@Setter
public class MqttProperties {
    private String username;
    private String password;
    private String host;
    private Integer port;
    private Integer qos = 1;
}
