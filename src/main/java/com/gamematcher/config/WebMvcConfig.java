package com.gamematcher.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * /hls/** 요청을 로컬 HLS 출력 디렉터리로 매핑.
 * app.streaming.hls-file-path 가 설정된 경우에만 활성화.
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final StreamingProperties streamingProperties;
    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String hlsPath = streamingProperties.getHlsFilePath();
        if (hlsPath != null && !hlsPath.isBlank()) {
            String path = Paths.get(hlsPath).toAbsolutePath().toString().replace("\\", "/");
            if (!path.endsWith("/")) path += "/";
            registry.addResourceHandler("/hls/**").addResourceLocations("file:" + path);
        }
        String uploadPath = uploadProperties.getPath();
        if (uploadPath != null && !uploadPath.isBlank()) {
            String path = Paths.get(uploadPath).toAbsolutePath().toString().replace("\\", "/");
            if (!path.endsWith("/")) path += "/";
            registry.addResourceHandler("/uploads/**").addResourceLocations("file:" + path);
        }
    }
}
