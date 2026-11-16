package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 设备档案
 *
 * @author chronic
 */
@Data
@TableName("cd_device")
@Schema(description = "设备档案")
public class CdDevice implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long deviceId;

    private String deviceNo;

    private String deviceName;

    @Schema(description = "设备类型：1智能手表 2血压计 3血氧仪 4血糖仪")
    private String deviceType;

    private String vendor;

    private String model;

    private Long patientId;

    private String patientName;

    @Schema(description = "在线状态：0离线 1在线")
    private String onlineStatus;

    private Integer battery;

    private LocalDateTime lastOnline;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
