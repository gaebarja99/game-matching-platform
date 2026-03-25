package com.gamematcher.config.community;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 커뮤니티 업로드 파일 정적 리소스 제공
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${community.upload.path:./uploads/community}")
    private String uploadPath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path path = Paths.get(uploadPath).toAbsolutePath().normalize();
        String location = "file:" + path + "/";
        registry.addResourceHandler("/uploads/community/**")
                .addResourceLocations(location);
    }
}
