package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 角色
 *
 * @author chronic
 */
@Data
@TableName("sys_role")
@Schema(description = "角色")
public class SysRole implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long roleId;

    private String roleName;

    private String roleKey;

    private Integer orderNum;

    private String status;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String remark;
}
