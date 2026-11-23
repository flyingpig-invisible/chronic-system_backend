package com.chronic.modules.chronic.support;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康评分计算结果：分项原始值 + 权重 + 合成得分
 *
 * @author chronic
 */
@Data
public class HealthScoreResult {

    private Long patientId;

    /** 健康评分(0-100，越高越健康) */
    private Integer healthScore;

    /** 加权合成前的每日得分(0-1) */
    private Double dailyScore;

    private LocalDateTime windowStart;

    private LocalDateTime windowEnd;

    /** 权重：wRisk/wMed/wAlert/wMonitor */
    private Map<String, Object> weights = new LinkedHashMap<>();

    /** 指标达标率(0-1)：Σ各批次达标指标数 / Σ各批次录入指标数 */
    private Double indicatorRate;

    /** 达标指标次数 */
    private Integer indicatorMeasured;

    /** 录入指标次数 */
    private Integer indicatorTotal;

    /** 窗口内录入批次数（同一次录入的多条指标算一批） */
    private Integer indicatorBatches;

    /** 各指标明细：indicatorCode/indicatorName/qualifiedCount/measuredCount/lastValue/lastQualified */
    private Map<String, Object> indicatorItems = new LinkedHashMap<>();

    /** 窗口内各批次达标情况：batchTime/count/qualified */
    private List<Map<String, Object>> indicatorBatchItems;

    /** 用药依从率(0-1) */
    private Double medicationRate;

    private Integer medicationOnTime;

    private Integer medicationLate;

    private Integer medicationMissed;

    /** 尚未到判定时点的打卡计划（不计入依从率分母） */
    private Integer medicationPending;

    /** 预警密度(0-1) */
    private Double alertDensity;

    private Integer alertDays;

    private Integer measuredDays;

    /** 检测完成率(0-1) */
    private Double monitorRate;

    private Integer monitorMeasuredTimes;

    private Integer monitorTargetTimes;

    /** 落库用的明细 JSON */
    private String detailJson;
}