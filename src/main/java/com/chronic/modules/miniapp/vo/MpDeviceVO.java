package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 已绑定设备
 *
 * @author chronic
 */
@Data
public class MpDeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String deviceNo;

    private String deviceName;

    private String deviceType;

    private String deviceTypeName;

    private String vendor;

    private String modelName;

    private String alias;

    private String onlineStatus;

    private Integer battery;

    private LocalDateTime lastOnline;

    private LocalDateTime lastSyncTime;

    private LocalDateTime bindTime;
}