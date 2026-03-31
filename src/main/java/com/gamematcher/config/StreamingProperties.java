package com.gamematcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OBS 스트리밍 연동 설정.
 * RTMP 서버(예: nginx-rtmp) 주소와 HLS 재생 베이스 URL.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.streaming")
public class StreamingProperties {

    /** OBS 서버 URL (예: rtmp://localhost/live) - 포트 제외 기본 1935 */
    private String rtmpServerUrl = "rtmp://localhost/live";

    /** HLS 재생 베이스 URL (예: http://127.0.0.1:8000/hls) - .m3u8 경로 앞까지 */
    private String hlsBaseUrl = "http://127.0.0.1:8000/hls";

    /** HLS 파일이 기록되는 로컬 경로 (nginx-rtmp hls_path 상위). 비어 있으면 /hls 미서빙 */
    private String hlsFilePath = "";

    /** FFmpeg 실행 파일 경로 */
    private String ffmpegPath = "";
}
