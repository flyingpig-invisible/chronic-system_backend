package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "绑定审核")
public class BindAuditDTO {

    @Schema(description = "审核结果：1同意 2拒绝")
    @NotBlank(message = "审核结果不能为空")
    private String result;

    @Schema(description = "审核意见")
    private String auditOpinion;
}
