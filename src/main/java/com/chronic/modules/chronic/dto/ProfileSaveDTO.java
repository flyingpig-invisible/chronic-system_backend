package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 患者专属档案保存请求
 *
 * @author chronic
 */
@Data
@Schema(description = "患者专属档案保存请求")
public class ProfileSaveDTO {

    @NotNull(message = "患者不能为空")
    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "血型")
    private String bloodType;

    @Schema(description = "过敏史（医疗类，仅医生可改）")
    private String allergyHistory;

    @Schema(description = "家族史（医疗类，仅医生可改）")
    private String familyHistory;

    @Schema(description = "既往病史（医疗类，仅医生可改）")
    private String pastHistory;

    @Schema(description = "既往手术史（医疗类，仅医生可改）")
    private String surgeryHistory;

    @Schema(description = "长期用药（医疗类，仅医生可改）")
    private String longMedication;

    @Schema(description = "残疾/行动不便说明（医疗类，仅医生可改）")
    private String disabilityNote;

    @Schema(description = "饮食习惯（生活类，医生与家属均可改）")
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
}
