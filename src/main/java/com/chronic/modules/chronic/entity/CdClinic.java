package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 线上就诊
 *
 * @author chronic
 */
@Data
@TableName("cd_clinic")
@Schema(description = "线上就诊")
public class CdClinic implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long visitId;

    private Long patientId;

    private String patientName;

    private Long doctorId;

    private String doctorName;

    @Schema(description = "就诊方式：1图文 2视频")
    private String visitType;

    @Schema(description = "状态：0待接诊 1问诊中 2已完成 3已取消")
    private String visitStatus;

    private String chiefComplaint;

    @Schema(description = "主诉附图，逗号分隔的相对路径")
    private String imageUrls;

    private LocalDateTime reserveTime;

    private String diagnosis;

    private String advice;

    private String prescription;

    @Schema(description = "医生诊断附图，逗号分隔的相对路径")
    private String adviceImages;

    private LocalDateTime applyTime;

    private LocalDateTime finishTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
