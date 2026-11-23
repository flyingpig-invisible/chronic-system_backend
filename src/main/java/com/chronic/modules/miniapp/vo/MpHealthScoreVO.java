package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 健康评分
 *
 * @author chronic
 */
@Data
public class MpHealthScoreVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 健康评分(0-100，越高越健康) */
    private Integer healthScore;

    private String riskLevel;

    /** 健康等级(3良好 2一般 1较差) */
    private String healthLevel;

    private String healthLevelName;

    private LocalDateTime calcTime;

    private String triggerType;
}