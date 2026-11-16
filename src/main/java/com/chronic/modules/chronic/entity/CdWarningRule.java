package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则
 *
 * @author chronic
 */
@Data
@TableName("cd_warning_rule")
@Schema(description = "预警规则")
public class CdWarningRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long ruleId;

    private String ruleName;

    private String diseaseCode;

    private String indicatorCode;

    private String indicatorName;

    @Schema(description = "比较符：>= <= > < =")
    private String operator;

    @Schema(description = "阈值")
    private BigDecimal thresholdValue;

    private String thresholdDesc;

    @Schema(description = "预警级别：1低危 2中危 3高危")
    private String warningLevel;

    @Schema(description = "持续时长（分钟）")
    private Integer durationMinutes;

    private String actionDesc;

    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
