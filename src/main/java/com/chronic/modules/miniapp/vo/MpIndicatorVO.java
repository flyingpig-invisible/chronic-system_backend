package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 最新指标快照
 *
 * @author chronic
 */
@Data
public class MpIndicatorVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String indicatorCode;

    private String indicatorName;

    private BigDecimal value;

    private String unit;

    private LocalDateTime measureTime;

    private String abnormalFlag;

    /** 参考范围描述（来自医生端预警规则） */
    private String referenceDesc;
}