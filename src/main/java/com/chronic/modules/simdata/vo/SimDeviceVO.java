package com.chronic.modules.simdata.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 设备可导入指标
 *
 * @author chronic
 */
@Data
public class SimDeviceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String deviceName;

    private String deviceType;

    private String deviceTypeName;

    private List<Option> indicators;

    /** 可导入指标项 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option implements Serializable {

        private static final long serialVersionUID = 1L;

        private String code;

        private String name;

        private String unit;
    }
}