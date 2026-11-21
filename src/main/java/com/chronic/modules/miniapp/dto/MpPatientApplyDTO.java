package com.chronic.modules.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

/**
 * 患者端建档申请提交参数
 *
 * @author chronic
 */
@Data
@Schema(description = "患者端建档申请")
public class MpPatientApplyDTO {

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "性别：1男 0女")
    private String gender;

    private LocalDate birthday;

    private String idCard;

    private String address;

    @Schema(description = "自述病种，逗号分隔的疾病编码，如 1,2")
    private String diseaseCodes;

    private String applyRemark;
}