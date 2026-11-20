package com.chronic.modules.chronic.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 设备卡片（小程序/医生端展示用）
 *
 * @author chronic
 */
@Data
@Schema(description = "设备卡片")
public class DeviceCardVO {

    private Long deviceId;

    @Schema(description = "蓝牙广播名，如 HUAWEI Band 10-4A7")
    private String btName;

    @Schema(description = "展示名：有别名用别名，否则用型号中文名")
    private String displayName;

    @Schema(description = "型号中文名，如 华为手环 10")
    private String modelName;

    @Schema(description = "型号编码，如 HUAWEI Band 10")
    private String modelCode;

    private String snSuffix;

    private String alias;

    private String deviceType;

    private String vendor;

    /** 0离线 1在线 */
    private String onlineStatus;

    private Integer battery;

    private LocalDateTime lastOnline;

    private LocalDateTime lastSyncTime;

    private LocalDateTime bindTime;

    private Long patientId;

    private String patientName;

    /** 是否已被绑定 */
    private Boolean bound;

    @Schema(description = "模拟蓝牙信号强度")
    private Integer rssi;
}
