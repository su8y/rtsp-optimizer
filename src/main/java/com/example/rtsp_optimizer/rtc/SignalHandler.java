package com.example.rtsp_optimizer.rtc;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.IceCandidate;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.kurento.client.PlayerEndpoint;
import org.kurento.client.WebRtcEndpoint;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class SignalHandler extends TextWebSocketHandler{
    private final KurentoClient kurentoClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentMap<String, UserSession> users = new ConcurrentHashMap<>();

    public SignalHandler(KurentoClient kurentoClient) {
        this.kurentoClient = kurentoClient;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.debug("WebSocket Connected {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("메시지 수신 - 세션 ID: " + session.getId() + ", 내용: " + payload);

        JsonNode jsonMessage = objectMapper.readTree(payload);
        String id = jsonMessage.get("id").asText();

        UserSession userSession = users.get(session.getId());

        switch (id) {
            case "start":
                if (userSession == null) {
                    start(session, jsonMessage);
                } else {
                    sendError(session, "Already started.");
                }
                break;
            case "onIceCandidate":
                if (userSession != null) {
                    onIceCandidate(userSession, jsonMessage);
                } else {
                    sendError(session, "Not started. Send 'start' first.");
                }
                break;
            case "stop":
                stop(session.getId());
                break;
            default:
                sendError(session, "Invalid message ID: " + id);
                break;
        }
    }

    private void start(WebSocketSession session, JsonNode message) throws IOException {
        String sessionId = session.getId();
        String cctvRTSPUrl = message.get("cctvUrl").asText();

        try {
            MediaPipeline pipeline = kurentoClient.createMediaPipeline();
            UserSession userSession = new UserSession(pipeline);
            users.put(sessionId, userSession);

            PlayerEndpoint player = new PlayerEndpoint.Builder(pipeline, cctvRTSPUrl).withNetworkCache(2000).build();
            userSession.setPlayerEndpoint(player);

            WebRtcEndpoint webRtcEndpoint = new WebRtcEndpoint.Builder(pipeline).build();
            userSession.setWebRtcEndpoint(webRtcEndpoint);

            // ICE Candidate 이벤트 리스너 등록
            webRtcEndpoint.addIceCandidateFoundListener(event -> {
                JsonNode response = objectMapper.createObjectNode()
                        .put("id", "iceCandidate")
                        .set("candidate", objectMapper.valueToTree(event.getCandidate()));
                try {
                    synchronized(session){
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    System.err.println("ICE Candidate 전송 오류: " + e.getMessage());
                }
            });

            player.connect(webRtcEndpoint);

            String sdpOffer = message.get("sdpOffer").asText();
            String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

            JsonNode response = objectMapper.createObjectNode()
                    .put("id", "startResponse")
                    .put("sdpAnswer", sdpAnswer);
            session.sendMessage(new TextMessage(response.toString()));

            webRtcEndpoint.gatherCandidates();
            player.play();

            System.out.println("스트림 시작 성공: " + sessionId);

        } catch (Exception e) {
            System.err.println("스트림 시작 중 오류: " + e.getMessage());
            sendError(session, "스트림 시작 실패: " + e.getMessage());
            stop(sessionId);
        }
    }

    private void onIceCandidate(UserSession userSession, JsonNode message) {
        // ... (이전과 동일)
        JsonNode candidateNode = message.get("candidate");
        if (candidateNode != null) {
            IceCandidate candidate = new IceCandidate(
                    candidateNode.get("candidate").asText(),
                    candidateNode.get("sdpMid").asText(),
                    candidateNode.get("sdpMLineIndex").asInt());
            userSession.getWebRtcEndpoint().addIceCandidate(candidate);
        }
    }

    private void stop(String sessionId) {
        UserSession userSession = users.remove(sessionId);
        if (userSession != null) {
            System.out.println("스트림 종료: " + sessionId);
            userSession.release();
        }
    }

    private void sendError(WebSocketSession session, String errorMessage) throws IOException {
        JsonNode errorResponse = objectMapper.createObjectNode()
                .put("id", "error")
                .put("message", errorMessage);
        session.sendMessage(new TextMessage(errorResponse.toString()));
        System.err.println("에러 전송: " + errorMessage);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("WebSocket 전송 에러 - 세션 ID: " + session.getId() + ", 에러: " + exception.getMessage());
        stop(session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        System.out.println("WebSocket 연결 종료: " + session.getId() + ", 상태: " + status.getCode());
        stop(session.getId());
    }

    // UserSession 내부 클래스는 동일
    @Data
    private static class UserSession {
        private final MediaPipeline pipeline;
        private PlayerEndpoint playerEndpoint;
        private WebRtcEndpoint webRtcEndpoint;

        public UserSession(MediaPipeline pipeline) {
            this.pipeline = pipeline;
        }
        public void release() { if (pipeline != null) { pipeline.release(); } }
    }
}
