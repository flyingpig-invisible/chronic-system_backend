package com.chronic.modules.system.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统指标字典（与病种无关的指标主数据）
 *
 * @author chronic
 */
@Data
@TableName("sys_indicator")
@Schema(description = "系统指标")
public class SysIndicator implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "指标ID")
    private Long indicatorId;

    @Schema(description = "指标编码（全局唯一）")
    private String indicatorCode;

    @Schema(description = "指标名称")
    private String indicatorName;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "显示顺序")
    private Integer sort;

    @Schema(description = "状态(0正常 1停用)")
    private String status;

    @Schema(description = "备注")
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}