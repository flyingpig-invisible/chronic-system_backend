package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 单个指标趋势
 *
 * @author chronic
 */
@Data
public class MpTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String indicatorCode;

    private String indicatorName;

    private String unit;

    private List<MpPointVO> points;

    private BigDecimal avgValue;

    private BigDecimal maxValue;

    private BigDecimal minValue;

    private BigDecimal latestValue;

    /** 达标率（异常点占比的反向值，按医生端 abnormal_flag 统计） */
    private Integer normalRate;

    private Integer abnormalCount;

    private Integer totalCount;
}