package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 患者病种
 *
 * @author chronic
 */
@Data
public class MpDiseaseVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String diseaseCode;

    private String diseaseName;

    private LocalDate diagnoseDate;

    /** 1轻度 2中度 3重度 */
    private String severity;

    private String severityName;
}