package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药提醒
 *
 * @author chronic
 */
@Data
@TableName("cd_remind")
@Schema(description = "用药提醒")
public class CdRemind implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long remindId;

    private Long patientId;

    private String medicineName;

    private String dosage;

    @Schema(description = "频次：1每日一次 2每日两次 3每日三次")
    private String frequency;

    @Schema(description = "提醒时间点，逗号分隔，如 08:00,20:00")
    private String remindTimes;

    private LocalDate beginDate;

    private LocalDate endDate;

    @Schema(description = "状态：0停用 1启用")
    private String status;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 患者姓名（非数据库字段） */
    @TableField(exist = false)
    private String patientName;
}
