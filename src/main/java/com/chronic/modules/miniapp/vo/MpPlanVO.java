package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

/**
 * 健康管理计划
 *
 * @author chronic
 */
@Data
public class MpPlanVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long planId;

    private Long patientId;

    private String planName;

    private String diseaseCode;

    private LocalDate beginDate;

    private LocalDate endDate;

    private String targetDesc;

    private String planStatus;

    private String planStatusName;

    private Integer executeRate;

    private String doctorName;

    private List<MpPlanItemVO> items;
}