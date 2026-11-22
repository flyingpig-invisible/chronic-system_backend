package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 小程序高危指标提醒项：某一项指标最近一次测量命中「高危」预警规则
 *
 * @author chronic
 */
@Data
public class MpHighRiskItemVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 患者ID */
    private Long patientId;

    /** 患者姓名 */
    private String patientName;

    /** 指标编码 */
    private String indicatorCode;

    /** 指标名称 */
    private String indicatorName;

    /** 最近一次测量值 */
    private BigDecimal value;

    /** 单位 */
    private String unit;

    /** 测量时间 */
    private LocalDateTime measureTime;

    /** 命中的高危规则名称 */
    private String ruleName;

    /** 触发条件描述，如「收缩压 ≥ 180 mmHg」 */
    private String thresholdDesc;

    /** 处置建议 */
    private String actionDesc;
}
