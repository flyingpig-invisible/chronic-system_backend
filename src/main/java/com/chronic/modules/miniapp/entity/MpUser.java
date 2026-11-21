package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序用户
 *
 * @author chronic
 */
@Data
@TableName("mp_user")
public class MpUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String openid;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String avatar;

    /** 性别：0男 1女 2未知 */
    private String gender;

    /** 用户角色：1患者 2家属 */
    private String userRole;

    private String status;

    @TableLogic
    private String delFlag;

    @TableField(insertStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER,
            updateStrategy = com.baomidou.mybatisplus.annotation.FieldStrategy.NEVER)
    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}