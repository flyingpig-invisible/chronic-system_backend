package com.chronic.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 登录用户信息
 *
 * @author chronic
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;

    /** 用户名 */
    private String username;

    /** 真实姓名 */
    private String realName;

    /** 用户类型：1 医生/普通用户、2 系统管理员、mp 小程序用户（存 JWT 原值，勿归一化） */
    private String userType;

    /** 小程序角色：1患者 2家属（userType=mp 时有值） */
    private String mpRole;

    /** 科室ID（系统用户） */
    private Long deptId;
}
