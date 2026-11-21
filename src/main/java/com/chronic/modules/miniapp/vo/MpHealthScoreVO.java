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

    private Integer totalScore;

    private String riskLevel;

    private String riskLevelName;

    private LocalDateTime calcTime;

    private String triggerType;
}