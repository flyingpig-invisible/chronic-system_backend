package com.chronic.modules.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "小程序登录结果")
public class MpLoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String token;
    private Long userId;
    private String username;
    private String realName;
    private String phone;
    private String avatar;
    private String userRole;
    private String userRoleName;
}
