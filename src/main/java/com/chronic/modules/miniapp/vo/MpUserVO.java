package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 小程序登录用户信息
 *
 * @author chronic
 */
@Data
public class MpUserVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    private String username;

    private String realName;

    private String phone;

    private String avatar;

    private String gender;

    /** 1患者 2家属 */
    private String userRole;

    private String userRoleName;

    /** 患者本人档案ID（家属为空） */
    private Long patientId;

    private String patientNo;

    /** 是否已关联医生端患者档案 */
    private Boolean patientLinked;
}