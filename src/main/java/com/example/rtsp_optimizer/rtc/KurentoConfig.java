package com.example.rtsp_optimizer.rtc;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoConfig {

    @Value("${kurento.url}")
    private String kurentoWsUrl;

    @Bean
    public KurentoClient kurentoClient() {
        return KurentoClient.create(kurentoWsUrl);
    }
}
