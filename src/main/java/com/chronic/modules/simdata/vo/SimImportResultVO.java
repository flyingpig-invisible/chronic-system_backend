package com.chronic.modules.simdata.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 设备模拟导入结果
 *
 * @author chronic
 */
@Data
public class SimImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deviceId;

    private String deviceName;

    /** 实际覆盖的整点个数 */
    private Integer hours;

    /** 尝试写入的记录数 */
    private Integer totalRows;

    /** 成功写入条数 */
    private Integer successRows;

    /** 因同一时间已有该指标数据而跳过的条数 */
    private Integer skippedRows;

    private LocalDateTime rangeStart;

    private LocalDateTime rangeEnd;

    private List<String> indicatorNames;

    /** 本次导入触发的预警条数 */
    private Integer warningCount;

    private Integer scoreBefore;

    private Integer scoreAfter;
}