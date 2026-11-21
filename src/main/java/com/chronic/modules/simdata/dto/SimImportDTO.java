package com.chronic.modules.simdata.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备模拟导入请求
 *
 * @author chronic
 */
@Data
@Schema(description = "设备模拟导入参数")
public class SimImportDTO {

    @Schema(description = "患者ID，可省略（默认取当前账号可访问的第一个患者）")
    private Long patientId;

    @NotNull(message = "请选择设备")
    @Schema(description = "设备ID")
    private Long deviceId;

    @Schema(description = "要导入的指标编码，均为所选设备支持的指标")
    private List<String> indicators;

    @NotNull(message = "请选择开始时间")
    @Schema(description = "时间范围（含），需为整点")
    private LocalDateTime startTime;

    @NotNull(message = "请选择结束时间")
    @Schema(description = "时间范围（含），需为整点")
    private LocalDateTime endTime;
}