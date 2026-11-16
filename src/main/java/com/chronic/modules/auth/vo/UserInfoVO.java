package com.chronic.modules.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户信息（含权限和菜单）
 *
 * @author chronic
 */
@Data
@Schema(description = "用户信息")
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String realName;

    private String avatar;

    private String phone;

    private String userType;

    private Long deptId;

    private String deptName;

    @Schema(description = "角色标识列表")
    private List<String> roles;

    @Schema(description = "权限标识列表")
    private List<String> permissions;
}
