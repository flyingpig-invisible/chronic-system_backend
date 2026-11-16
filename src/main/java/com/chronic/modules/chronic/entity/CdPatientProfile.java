package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者专属档案（仅医生端与家属端可见，患者本人不可见）
 *
 * @author chronic
 */
@Data
@TableName("cd_patient_profile")
@Schema(description = "患者专属档案")
public class CdPatientProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long profileId;

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "血型")
    private String bloodType;

    @Schema(description = "过敏史（医疗类）")
    private String allergyHistory;

    @Schema(description = "家族史（医疗类）")
    private String familyHistory;

    @Schema(description = "既往病史（医疗类）")
    private String pastHistory;

    @Schema(description = "既往手术史（医疗类）")
    private String surgeryHistory;

    @Schema(description = "长期用药（医疗类）")
    private String longMedication;

    @Schema(description = "残疾/行动不便说明（医疗类）")
    private String disabilityNote;

    @Schema(description = "饮食习惯（生活类）")
    private String dietHabit;

    @Schema(description = "作息与睡眠（生活类）")
    private String sleepHabit;

    @Schema(description = "情绪与心理状态（生活类）")
    private String moodStatus;

    @Schema(description = "运动习惯（生活类）")
    private String exerciseHabit;

    @Schema(description = "家属在家观察记录（生活类）")
    private String homeObservation;

    @Schema(description = "补充说明")
    private String extraNote;

    @Schema(description = "最后修改人")
    private String lastEditor;

    @Schema(description = "最后修改人角色(1医生 2家属)")
    private String lastEditorRole;

    @Schema(description = "最后修改时间")
    private LocalDateTime lastEditTime;

    @TableLogic
    private String delFlag;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
