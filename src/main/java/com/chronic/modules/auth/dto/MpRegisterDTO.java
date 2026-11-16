package com.chronic.modules.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 小程序注册请求
 *
 * @author chronic
 */
@Data
@Schema(description = "小程序注册请求")
public class MpRegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Schema(description = "登录账号")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Schema(description = "密码")
    private String password;

    @NotBlank(message = "姓名不能为空")
    @Schema(description = "真实姓名")
    private String realName;

    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别：0男 1女 2未知")
    private String gender;

    @NotBlank(message = "请选择用户角色")
    @Schema(description = "用户角色：1患者 2家属")
    private String userRole;
}
