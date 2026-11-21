package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 小程序首页概览
 *
 * @author chronic
 */
@Data
public class MpDashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private MpPatientVO patient;

    private List<MpIndicatorVO> indicators;

    private MpHealthScoreVO healthScore;

    private Map<String, Object> warningStat;

    /** 今日用药任务 */
    private Map<String, Object> medication;

    /** 进行中的管理计划数量 */
    private Integer activePlanCount;

    /** 未读医生指导数量 */
    private Integer unreadGuideCount;

    private LocalDateTime serverTime;
}