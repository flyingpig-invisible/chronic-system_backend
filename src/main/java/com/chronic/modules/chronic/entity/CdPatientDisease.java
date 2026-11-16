package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者病种关联
 *
 * @author chronic
 */
@Data
@TableName("cd_patient_disease")
@Schema(description = "患者病种关联")
public class CdPatientDisease implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    @Schema(description = "病种编码：1高血压 2糖尿病 3冠心病 4慢阻肺")
    private String diseaseCode;

    @Schema(description = "病种名称")
    private String diseaseName;

    private LocalDate diagnoseDate;

    @Schema(description = "严重程度：1轻度 2中度 3重度")
    private String severity;

    private LocalDateTime createTime;
}
