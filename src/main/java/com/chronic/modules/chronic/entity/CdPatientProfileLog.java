package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者专属档案 · 填写留痕
 *
 * @author chronic
 */
@Data
@TableName("cd_patient_profile_log")
@Schema(description = "患者专属档案填写留痕")
public class CdPatientProfileLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long profileId;

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "填写人")
    private String editor;

    @Schema(description = "填写人角色(1医生 2家属)")
    private String editorRole;

    @Schema(description = "字段标识")
    private String fieldKey;

    @Schema(description = "字段名称")
    private String fieldName;

    @Schema(description = "修改前")
    private String oldValue;

    @Schema(description = "修改后")
    private String newValue;

    @Schema(description = "填写时间")
    private LocalDateTime editTime;
}
