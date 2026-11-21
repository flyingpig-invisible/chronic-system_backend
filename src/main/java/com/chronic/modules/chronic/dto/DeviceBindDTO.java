package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 设备绑定入参
 *
 * @author chronic
 */
@Data
@Schema(description = "设备绑定入参")
public class DeviceBindDTO {

    @NotNull(message = "患者ID不能为空")
    @Schema(description = "绑定到哪个患者")
    private Long patientId;

    @NotBlank(message = "设备蓝牙名不能为空")
    @Schema(description = "蓝牙广播名，如 HUAWEI Band 10-4A7", example = "HUAWEI Band 10-4A7")
    private String btName;

    @Schema(description = "自定义别名，可空，如 爸爸的手环")
    private String alias;
}
