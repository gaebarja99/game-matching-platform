package com.gamematcher.dto.community;

import com.gamematcher.entity.community.PostAttachment;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AttachmentResponseDto {

    private Long id;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;

    public static AttachmentResponseDto from(PostAttachment attachment) {
        AttachmentResponseDto dto = new AttachmentResponseDto();
        dto.id = attachment.getId();
        dto.fileName = attachment.getFileName();
        dto.filePath = attachment.getFilePath();
        dto.fileSize = attachment.getFileSize();
        dto.contentType = attachment.getContentType();
        return dto;
    }
}
