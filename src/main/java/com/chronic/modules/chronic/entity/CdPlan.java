package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康管理计划
 *
 * @author chronic
 */
@Data
@TableName("cd_plan")
@Schema(description = "健康管理计划")
public class CdPlan implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long planId;

    private Long patientId;

    private String patientName;

    private String diseaseCode;

    private String planName;

    private LocalDate beginDate;

    private LocalDate endDate;

    private String targetDesc;

    @Schema(description = "计划状态：0未开始 1执行中 2已完成")
    private String planStatus;

    @Schema(description = "执行率(%)")
    private Integer executeRate;

    private Long doctorId;

    private String doctorName;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "计划项列表")
    @TableField(exist = false)
    private List<CdPlanItem> items;
}
