package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单权限
 *
 * @author chronic
 */
@Data
@TableName("sys_menu")
@Schema(description = "菜单")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long menuId;

    private String menuName;

    private Long parentId;

    private Integer orderNum;

    private String path;

    private String component;

    @Schema(description = "是否外链：0是 1否")
    private Integer isFrame;

    @Schema(description = "是否缓存：0缓存 1不缓存")
    private Integer isCache;

    @Schema(description = "菜单类型：M目录 C菜单 F按钮")
    private String menuType;

    private String visible;

    private String status;

    private String perms;

    private String icon;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    private LocalDateTime updateTime;

    private String remark;

    /** 子菜单（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
