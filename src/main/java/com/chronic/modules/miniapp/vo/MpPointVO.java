package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 趋势点
 *
 * @author chronic
 */
@Data
public class MpPointVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private LocalDateTime measureTime;

    private BigDecimal value;

    private String abnormalFlag;
}