package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "转诊审核提交")
public class ReferralAuditDTO {

    @Schema(description = "审核结果：1通过 2驳回")
    @NotBlank(message = "审核结果不能为空")
    private String referralStatus;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "接收医院")
    private String receiveHospital;

    @Schema(description = "接收科室")
    private String receiveDept;
}
