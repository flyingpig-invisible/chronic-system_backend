package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典类型
 *
 * @author chronic
 */
@Data
@TableName("sys_dict_type")
@Schema(description = "字典类型")
public class SysDictType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long dictId;

    private String dictName;

    private String dictType;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String remark;
}
