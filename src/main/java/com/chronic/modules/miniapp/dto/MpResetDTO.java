package com.chronic.modules.miniapp.dto;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

/**
 * 忘记密码重置参数：校验手机号后直接设置新密码
 *
 * @author chronic
 */
@Data
public class MpResetDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "请输入注册手机号")
    @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确")
    private String phone;

    @NotBlank(message = "请输入新密码")
    private String password;
}