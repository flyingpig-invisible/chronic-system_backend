package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 健康评分记录
 *
 * @author chronic
 */
@Data
@TableName("cd_health_score")
@Schema(description = "健康评分记录")
public class CdHealthScore implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long scoreId;

    private Long patientId;

    @Schema(description = "加权风险评分")
    private Integer totalScore;

    /** 健康评分(0-100，越高越健康) */
    private Integer healthScore;

    @Schema(description = "风险等级：1低危 2中危 3高危")
    private String riskLevel;

    @Schema(description = "各指标评分明细(JSON)")
    private String detailJson;

    @Schema(description = "触发来源：1监测 2干预 3定时")
    private String triggerType;

    private LocalDateTime calcTime;

    /** 患者姓名（非数据库字段） */
    @TableField(exist = false)
    private String patientName;
}
