package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警记录
 *
 * @author chronic
 */
@Data
@TableName("cd_warning")
@Schema(description = "预警记录")
public class CdWarning implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long warningId;

    private Long patientId;

    private String patientName;

    private String diseaseCode;

    private String indicatorCode;

    private String indicatorName;

    @Schema(description = "触发时指标值")
    private BigDecimal indicatorValue;

    private String unit;

    @Schema(description = "触发的阈值描述")
    private String thresholdDesc;

    private Long ruleId;

    @Schema(description = "预警级别：1低危 2中危 3高危")
    private String warningLevel;

    @Schema(description = "处置状态：0待处理 1处理中 2已处理")
    private String handleStatus;

    @Schema(description = "处置方式：1电话 2问诊 3调药 4转诊")
    private String handleType;

    private String handleContent;

    private String handleBy;

    private LocalDateTime handleTime;

    @Schema(description = "是否联动急救：0否 1是")
    private String emergencyLinked;

    private LocalDateTime triggerTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // ========== 非数据库字段 ==========
    @TableField(exist = false)
    private String beginTime;

    @TableField(exist = false)
    private String endTime;

    @TableField(exist = false)
    private String actionDesc;
}
