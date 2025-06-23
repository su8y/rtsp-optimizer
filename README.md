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
