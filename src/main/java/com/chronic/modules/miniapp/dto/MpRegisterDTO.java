package com.chronic.modules.miniapp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 小程序注册参数（仅开放患者/家属注册，医生由管理端创建）
 *
 * @author chronic
 */
@Data
public class MpRegisterDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请输入密码")
    private String password;

    private String realName;

    /** 1患者 2家属 */
    private String userRole;

    private String gender;

    /** 患者端自带患者编号时可填写，用于关联医生端已有档案 */
    private String patientNo;

    /** 患者注册时自选可见科室：只有这些科室的医生能查看其信息 */
    private java.util.List<Long> deptIds;
}
