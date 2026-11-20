package com.chronic.modules.chronic.support.huawei;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 华为导出包解析结果
 *
 * @author chronic
 */
@Data
public class ParseResult {

    /** 文件类型：ZIP / JSON */
    private String fileType;

    /** 解析的 json 文件数 */
    private int fileCount;

    /** 原始记录（record）数 */
    private int rawRecords;

    /** 展开后的采样点数（聚合前） */
    private int parsedPoints;

    /** 不支持的类型（压力、情绪、锻炼强度等）采样点数 */
    private int unsupportedPoints;

    /** 无效数据点数（空值、越界、时间异常） */
    private int invalidPoints;

    /** 睡眠片段被丢弃数（时长过短或过长） */
    private int droppedSleepNights;

    /** 数据起始日 */
    private LocalDate dateFrom;

    /** 数据结束日 */
    private LocalDate dateTo;

    /** 涉及的设备码 */
    private Set<String> deviceCodes = new LinkedHashSet<>();

    /** 最终待入库的测量点（聚合后） */
    private List<HealthPoint> points = new ArrayList<>();

    /** 各指标最终条数：hr / spo2 / sleep */
    private Map<String, Integer> saved = new LinkedHashMap<>();

    /** 问题明细（最多保留 200 条，避免日志过大） */
    private List<ImportIssue> issues = new ArrayList<>();

    /** 最近一次测量时间（任意指标） */
    private LocalDateTime latestTime;

    public void addIssue(int seq, String reason) {
        if (issues.size() < 200) {
            issues.add(ImportIssue.of(seq, reason));
        }
    }

    public boolean isEmpty() {
        return points.isEmpty();
    }
}
