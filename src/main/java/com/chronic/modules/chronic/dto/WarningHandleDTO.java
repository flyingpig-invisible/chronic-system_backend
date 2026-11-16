package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 预警处置请求
 *
 * @author chronic
 */
@Data
@Schema(description = "预警处置请求")
public class WarningHandleDTO {

    @NotBlank(message = "处置状态不能为空")
    @Schema(description = "处置状态：1处理中 2已处理")
    private String handleStatus;

    @Schema(description = "处置方式：1电话 2问诊 3调药 4转诊")
    private String handleType;

    @Schema(description = "处置意见")
    private String handleContent;

    @Schema(description = "是否联动急救")
    private Boolean linkEmergency;

    private String remark;
}
