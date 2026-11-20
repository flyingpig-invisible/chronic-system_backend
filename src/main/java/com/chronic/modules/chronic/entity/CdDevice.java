package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldFill;
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

    @Schema(description = "设备编号，绑定场景下存蓝牙广播名，如 HUAWEI Band 10-4A7")
    private String deviceNo;

    private String deviceName;

    @Schema(description = "MAC 地址（仅安卓可获取，可空）")
    private String deviceMac;

    @Schema(description = "设备类型：1智能手表/手环 2血压计 3血氧仪 4血糖仪")
    private String deviceType;

    private String vendor;

    @Schema(description = "型号编码，如 HUAWEI Band 10")
    private String model;

    @Schema(description = "型号中文名，如 华为手环 10")
    private String modelName;

    @Schema(description = "蓝牙名后缀（MAC 后 3 位），如 4A7")
    private String snSuffix;

    @Schema(description = "用户自定义别名，如 爸爸的手环")
    private String alias;

    @Schema(description = "华为导出包 deviceCode，导入时用于关联设备")
    private String hwDeviceCode;

    private Long patientId;

    private String patientName;

    @Schema(description = "绑定操作人（小程序用户ID）")
    private Long bindUserId;

    @Schema(description = "绑定时间")
    private LocalDateTime bindTime;

    @Schema(description = "绑定来源：1模拟扫描 2手动输入 3医生后台")
    private String bindSource;

    @Schema(description = "在线状态：0离线 1在线")
    private String onlineStatus;

    private Integer battery;

    private LocalDateTime lastOnline;

    @Schema(description = "最近一次数据导入时间")
    private LocalDateTime lastSyncTime;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
