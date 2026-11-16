package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统用户
 *
 * @author chronic
 */
@Data
@TableName("sys_user")
@Schema(description = "系统用户")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long userId;

    private String username;

    private String password;

    private String realName;

    private String phone;

    private String avatar;

    private String gender;

    private Long deptId;

    private String title;

    @Schema(description = "用户类型：1医生 2管理员")
    private String userType;

    private String status;

    @TableLogic
    private String delFlag;

    private LocalDateTime loginDate;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    /** 科室名称（非数据库字段） */
    @TableField(exist = false)
    private String deptName;

    /** 角色ID列表（非数据库字段） */
    @TableField(exist = false)
    private List<Long> roleIds;

    /** 角色名称（非数据库字段） */
    @TableField(exist = false)
    private String roleNames;
}
