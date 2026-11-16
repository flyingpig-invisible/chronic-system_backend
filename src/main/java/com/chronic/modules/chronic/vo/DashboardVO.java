package com.chronic.modules.chronic.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 患者看板概览
 *
 * @author chronic
 */
@Data
@Schema(description = "患者看板概览")
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "患者基本信息")
    private PatientVO patient;

    @Schema(description = "最新监测指标")
    private List<Map<String, Object>> latestMeasures;

    @Schema(description = "预警统计")
    private Map<String, Object> warningStat;

    @Schema(description = "当前风险评分")
    private Integer riskScore;

    @Schema(description = "当前风险等级")
    private String riskLevel;
}
