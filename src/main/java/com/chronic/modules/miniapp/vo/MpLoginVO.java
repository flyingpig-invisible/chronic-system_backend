package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 小程序登录结果
 *
 * @author chronic
 */
@Data
public class MpLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;

    private MpUserVO user;
}