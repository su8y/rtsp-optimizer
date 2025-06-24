package com.example.rtsp_optimizer.rtc;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer{
    private final SignalHandler signalHandler;

    public WebSocketConfig(SignalHandler signalHandler) {
        this.signalHandler = signalHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry
        .addHandler(signalHandler, "/signaling")
        .setAllowedOriginPatterns("*");
    }

    
    
}
