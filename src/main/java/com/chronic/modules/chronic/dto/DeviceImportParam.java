package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 数据导入入参
 *
 * @author chronic
 */
@Data
@Schema(description = "数据导入入参")
public class DeviceImportParam {

    @Schema(description = "导入到哪个患者，必填", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long patientId;

    @Schema(description = "来源设备ID，可空；为空时按导出包 deviceCode 自动归档", example = "1")
    private Long deviceId;

    @Schema(description = "只解析不落库，用于导入前预览", example = "false")
    private Boolean dryRun;

    @Schema(description = "心率/血氧按小时聚合，默认 true", example = "true")
    private Boolean aggregate;

    @Schema(description = "同一文件已导入过时是否强制再导（配合 dryRun=false 使用）", example = "false")
    private Boolean force;
}
