package com.gamematcher.dto.community;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PageResponseDto<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public static <T> PageResponseDto<T> of(Page<T> page) {
        PageResponseDto<T> dto = new PageResponseDto<>();
        dto.content = page.getContent();
        dto.page = page.getNumber();
        dto.size = page.getSize();
        dto.totalElements = page.getTotalElements();
        dto.totalPages = page.getTotalPages();
        dto.first = page.isFirst();
        dto.last = page.isLast();
        return dto;
    }
}
