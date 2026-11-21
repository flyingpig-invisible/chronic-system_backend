package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 患者预警（字段与医生端 cd_warning 对齐）
 *
 * @author chronic
 */
@Data
public class MpWarningVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long warningId;

    private Long patientId;

    private String patientName;

    private String diseaseCode;

    private String indicatorCode;

    private String indicatorName;

    private BigDecimal indicatorValue;

    private String unit;

    private String thresholdDesc;

    private String warningLevel;

    private String warningLevelName;

    private String handleStatus;

    private String handleStatusName;

    private String handleType;

    private String handleContent;

    private String handleBy;

    private LocalDateTime handleTime;

    private String emergencyLinked;

    private LocalDateTime triggerTime;

    /** 来自预警规则的处置建议 */
    private String actionDesc;

    private String dateLabel;

    private String timeLabel;
}