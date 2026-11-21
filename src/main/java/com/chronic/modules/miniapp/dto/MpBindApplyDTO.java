package com.chronic.modules.miniapp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

/**
 * 家属申请绑定患者
 *
 * @author chronic
 */
@Data
public class MpBindApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 患者手机号或患者编号 */
    @NotBlank(message = "请输入患者手机号或患者编号")
    private String patientKeyword;

    @NotBlank(message = "请选择与患者关系")
    private String relation;

    private String applyRemark;
}