package com.gamematcher.dto.report;

import com.gamematcher.constant.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ReportRequestDto {

    @NotNull(message = "피신고자 ID는 필수입니다.")
    private Long reportedUserId;

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 500)
    private String description;
}
