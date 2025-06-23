package com.example.rtsp_optimizer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "cctv")
public class CctvConfig {
    private List<CctvStream> streams;

    public List<CctvStream> getStreams() {
        return streams;
    }

    public void setStreams(List<CctvStream> streams) {
        this.streams = streams;
    }
}