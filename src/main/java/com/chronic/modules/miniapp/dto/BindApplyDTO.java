package com.chronic.modules.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "家属发起绑定申请")
public class BindApplyDTO {

    @Schema(description = "患者ID")
    @NotNull(message = "患者ID不能为空")
    private Long patientId;

    @Schema(description = "与患者关系")
    private String relation;

    @Schema(description = "申请备注")
    private String applyRemark;
}
