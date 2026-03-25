package com.gamematcher.dto.community;

import com.gamematcher.constant.ReportReason;
import com.gamematcher.constant.community.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CommunityReportRequestDto {

    @NotNull(message = "대상 유형은 필수입니다.")
    private ReportTargetType targetType;

    @NotNull(message = "게시글 ID는 필수입니다.")
    private Long postId;

    private Long commentId; // 댓글 신고 시

    @NotNull(message = "신고 사유는 필수입니다.")
    private ReportReason reason;

    @Size(max = 500)
    private String description;
}
