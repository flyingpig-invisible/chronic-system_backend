package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "就诊完成提交")
public class ClinicFinishDTO {

    @Schema(description = "诊断")
    @NotBlank(message = "诊断不能为空")
    private String diagnosis;

    @Schema(description = "医嘱建议")
    private String advice;

    @Schema(description = "处方")
    private String prescription;

    @Schema(description = "诊断附图，逗号分隔的 /upload/ 相对路径，最多 9 张")
    private String adviceImages;
}
