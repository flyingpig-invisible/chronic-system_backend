package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 字典数据
 *
 * @author chronic
 */
@Data
@TableName("sys_dict_data")
@Schema(description = "字典数据")
public class SysDictData implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long dictCode;

    private Integer dictSort;

    private String dictLabel;

    private String dictValue;

    private String dictType;

    private String cssClass;

    private String listClass;

    private String isDefault;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String remark;
}
