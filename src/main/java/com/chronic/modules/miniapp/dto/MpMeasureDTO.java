package com.chronic.modules.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 小程序端一次性录入一组健康指标（允许部分留空）
 *
 * @author chronic
 */
@Data
@Schema(description = "小程序端一次性录入一组健康指标")
public class MpMeasureDTO {

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "收缩压 mmHg")
    private BigDecimal sbp;

    @Schema(description = "舒张压 mmHg")
    private BigDecimal dbp;

    @Schema(description = "心率 次/分")
    private BigDecimal hr;

    @Schema(description = "血氧 %")
    private BigDecimal spo2;

    @Schema(description = "血糖 mmol/L")
    private BigDecimal glu;

    @Schema(description = "呼吸频率 次/分")
    private BigDecimal rr;

    @Schema(description = "睡眠时长 小时")
    private BigDecimal sleep;

    @Schema(description = "测量时间 yyyy-MM-dd HH:mm:ss，为空取当前时间")
    private String measureTime;

    @Schema(description = "备注")
    private String remark;
}
