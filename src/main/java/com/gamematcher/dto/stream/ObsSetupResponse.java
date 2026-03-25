package com.gamematcher.dto.stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * OBS Studio 설정에 필요한 정보.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObsSetupResponse {

    /** 스트림 키 (OBS '스트림 키' 필드에 입력) */
    private String streamKey;

    /** 서버 URL (OBS '서버' 필드에 입력, 예: rtmp://localhost/live) */
    private String serverUrl;

    /** 한 줄 안내 문구 */
    private String instructions;
}
