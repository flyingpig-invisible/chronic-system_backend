package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用药打卡记录
 *
 * @author chronic
 */
@Data
@TableName("cd_remind_log")
@Schema(description = "用药打卡记录")
public class CdRemindLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long logId;

    private Long remindId;

    private Long patientId;

    private LocalDate planDate;

    private String planTime;

    private LocalDateTime checkTime;

    @Schema(description = "打卡状态：0未打卡 1已打卡 2漏服")
    private String checkStatus;
}
