# CCTV 모니터링 대시보드

이 프로젝트는 공공데이터로 제공되는 CCTV 영상을 웹 환경에서 실시간으로 모니터링하기 위한 간단한 대시보드 애플리케이션입니다. Spring Boot와 Thymeleaf를 사용하여 백엔드와 프론트엔드를 구성하며, 여러 개의 CCTV 스트림을 한 화면에서 동시에 확인할 수 있도록 합니다.

**주의:** 공공데이터에서 CCTV 영상이 RTSP로만 제공될 경우, 웹 브라우저에서 직접 RTSP를 재생할 수 없으므로 **별도의 미디어 서버를 통해 RTSP 스트림을 HLS(HTTP Live Streaming) 또는 DASH(Dynamic Adaptive Streaming over HTTP)와 같은 HTTP 기반 스트림으로 변환하여 제공해야 합니다.** 본 프로젝트는 변환된 HTTP 스트림 URL을 가정하고 있습니다.

##  주요 기술 스택

* **백엔드:** Spring Boot 3.2.x, Java 17
* **프론트엔드:** Thymeleaf, HTML5 Video, Video.js
* **빌드 도구:** Maven


## 기능

* **다중 CCTV 모니터링:** 설정된 여러 CCTV 스트림을 한 페이지에서 그리드 형태로 동시에 표시합니다.
* **HLS/DASH 지원:** Video.js 라이브러리를 통해 HTTP Live Streaming (HLS) 및 MPEG-DASH 스트림 재생을 지원합니다. (MP4 재생도 가능)


```mermaid
stateDiagram-v2
    direction LR

    state Client as 웹 브라우저
    state SpringBoot as Spring Boot 백엔드
    state SRS as SRS 미디어 서버
    state CCTV as CCTV 카메라

    [*] --> Client: 1. 웹 대시보드 접근

    subgraph Client 상태
        state HLS_VIEWING as HLS 영상 시청
        state WEBRTC_CONNECTING as WebRTC 연결 중
        state WEBRTC_STREAMING_PTZ as WebRTC 스트리밍 & PTZ 제어
    end

    subgraph SpringBoot 상태
        state SB_IDLE as 유휴 상태
        state SB_SIGNALING as 시그널링 중개
        state SB_PTZ_PROXY as PTZ 명령 중개
    end

    subgraph SRS 상태
        state SRS_IDLE as 유휴 상태
        state SRS_RTSP_INGESTING as RTSP 수신 중
        state SRS_HLS_SERVING as HLS 서비스 중
        state SRS_WEBRTC_PEERING as WebRTC 피어링 중
        state SRS_WEBRTC_STREAMING as WebRTC 스트리밍 중
    end

    subgraph CCTV 상태
        state CCTV_STREAMING as 영상 스트리밍 중
        state CCTV_PTZ_COMMAND as PTZ 명령 처리 중
    end

    Client --> HLS_VIEWING: 1. 초기 웹 페이지 로드 & HLS 플레이어 초기화
    HLS_VIEWING --> SRS_RTSP_INGESTING: 2. 클라이언트 HLS 요청 (HTTP GET .m3u8)
    SRS_RTSP_INGESTING --> SRS_HLS_SERVING: 3. HLS 변환 및 서비스 시작
    SRS_RTSP_INGESTING --> CCTV_STREAMING: 4. RTSP Pull 시작
    CCTV_STREAMING --> SRS_RTSP_INGESTING: 5. RTSP 스트림 전송
    SRS_HLS_SERVING --> HLS_VIEWING: 6. HLS 영상 재생

    HLS_VIEWING --> WEBRTC_CONNECTING: 7. 사용자 PTZ 제어 시도 (클릭)
    WEBRTC_CONNECTING --> SB_SIGNALING: 8. WebSocket 시그널링 연결 요청
    SB_SIGNALING --> WEBRTC_CONNECTING: 9. WebSocket 연결 성공

    WEBRTC_CONNECTING --> SB_SIGNALING: 10. SDP Offer 전송
    SB_SIGNALING --> SRS_WEBRTC_PEERING: 11. Offer를 SRS API로 전달
    SRS_WEBRTC_PEERING --> STUN_TURN: 12. ICE Candidate 요청
    STUN_TURN --> SRS_WEBRTC_PEERING: 13. ICE Candidate 응답
    SRS_WEBRTC_PEERING --> SB_SIGNALING: 14. SDP Answer 및 ICE Candidate 생성/전송
    SB_SIGNALING --> WEBRTC_CONNECTING: 15. Answer 및 ICE Candidate 수신
    WEBRTC_CONNECTING --> STUN_TURN: 16. ICE Candidate 요청
    STUN_TURN --> WEBRTC_CONNECTING: 17. ICE Candidate 응답

    WEBRTC_CONNECTING --> WEBRTC_STREAMING_PTZ: 18. WebRTC 연결 성공 (초저지연 스트림 & DataChannel 활성화)
    WEBRTC_STREAMING_PTZ --> SRS_WEBRTC_STREAMING: 19. WebRTC 스트림 수신 시작
    SRS_WEBRTC_STREAMING --> WEBRTC_STREAMING_PTZ: 20. WebRTC 영상 스트림 전송

    WEBRTC_STREAMING_PTZ --> SB_PTZ_PROXY: 21. PTZ 제어 명령 전송 (DataChannel 통해 SRS->SpringBoot)
    SB_PTZ_PROXY --> CCTV_PTZ_COMMAND: 22. ONVIF/HTTP API 호출
    CCTV_PTZ_COMMAND --> CCTV_STREAMING: 23. PTZ 동작 수행 (카메라 이동/줌)
    CCTV_PTZ_COMMAND --> SB_PTZ_PROXY: 24. PTZ 명령 처리 완료 응답
    SB_PTZ_PROXY --> WEBRTC_STREAMING_PTZ: 25. PTZ 제어 피드백 (선택적)

    WEBRTC_STREAMING_PTZ --> HLS_VIEWING: 26. 사용자 PTZ 제어 중단 / WebRTC 연결 해제
    HLS_VIEWING --> [*]: 27. 브라우저 닫기 / 대시보드 종료
    WEBRTC_STREAMING_PTZ --> [*]: 28. 브라우저 닫기 / 대시보드 종료

    SB_SIGNALING --> SB_IDLE: WebRTC 연결 종료 시
    SB_PTZ_PROXY --> SB_IDLE: PTZ 명령 없을 시
    SRS_HLS_SERVING --> SRS_IDLE: HLS 요청 없을 시 / 서버 종료
    SRS_WEBRTC_STREAMING --> SRS_IDLE: 모든 WebRTC 클라이언트 연결 종료 시
    CCTV_STREAMING --> [*]: 카메라 전원 오프 / 문제 발생
```