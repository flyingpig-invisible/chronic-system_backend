package com.chronic.modules.miniapp.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 小程序登录参数
 *
 * @author chronic
 */
@Data
public class MpLoginDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 手机号或账号 */
    private String username;

    private String password;

    /** wx.login 返回的 code；配置了小程序密钥时可免密登录 */
    private String code;
}