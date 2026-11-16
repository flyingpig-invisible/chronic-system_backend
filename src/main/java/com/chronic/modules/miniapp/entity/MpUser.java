package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序用户（患者/家属）
 *
 * @author chronic
 */
@Data
@TableName("mp_user")
@Schema(description = "小程序用户")
public class MpUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long userId;

    @Schema(description = "微信openid")
    private String openid;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String avatar;

    private String gender;

    @Schema(description = "用户角色：1患者 2家属")
    private String userRole;

    private String status;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
