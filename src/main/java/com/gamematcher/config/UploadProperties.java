package com.gamematcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.upload")
public class UploadProperties {

    /** 프로필 사진 등 업로드 파일 저장 경로 */
    private String path = "./uploads";
}
