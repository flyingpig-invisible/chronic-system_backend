package com.chronic.modules.chronic.support.huawei;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 解析后的一条测量点（指标 + 数值 + 时间）
 *
 * @author chronic
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthPoint {

    /** 指标编码：hr / spo2 / sleep */
    private String indicatorCode;

    /** 指标名称：心率 / 血氧 / 睡眠时长 */
    private String indicatorName;

    /** 单位 */
    private String unit;

    /** 数值 */
    private BigDecimal value;

    /** 测量时间（已按导出包的 timeZone 转换） */
    private LocalDateTime measureTime;

    /** 华为设备码（deviceCode），可为空 */
    private String deviceCode;
}
