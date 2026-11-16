package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 急救联动请求
 *
 * @author chronic
 */
@Data
@Schema(description = "急救联动请求")
public class EmergencyLinkDTO {

    @NotBlank(message = "联动渠道不能为空")
    @Schema(description = "联动渠道：120/hospital/family/community")
    private String channel;

    @NotBlank(message = "接收方不能为空")
    private String receiver;

    private String linkResult;

    private String remark;
}
