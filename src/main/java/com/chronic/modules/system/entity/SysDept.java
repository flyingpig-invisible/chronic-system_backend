package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 科室
 *
 * @author chronic
 */
@Data
@TableName("sys_dept")
@Schema(description = "科室")
public class SysDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long deptId;

    private Long parentId;

    private String deptName;

    private Integer orderNum;

    private String status;

    @TableLogic
    private String delFlag;

    private java.time.LocalDateTime createTime;

    /** 子科室（非数据库字段） */
    @TableField(exist = false)
    private List<SysDept> children = new ArrayList<>();
}
