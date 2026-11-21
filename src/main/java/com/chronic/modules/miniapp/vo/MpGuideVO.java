package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医生指导
 *
 * @author chronic
 */
@Data
public class MpGuideVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long guideId;

    private Long patientId;

    private String patientName;

    private Long warningId;

    private String guideType;

    private String guideTypeName;

    private String content;

    private String doctorName;

    private LocalDateTime guideTime;

    private String readStatus;

    /** 关联预警的处置建议 */
    private String actionDesc;
}