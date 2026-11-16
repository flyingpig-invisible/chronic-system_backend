package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 转诊记录
 *
 * @author chronic
 */
@Data
@TableName("cd_referral")
@Schema(description = "转诊记录")
public class CdReferral implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long referralId;

    private Long patientId;

    private String patientName;

    private Long fromDeptId;

    private String fromDeptName;

    private String targetHospital;

    private String targetDept;

    private String reason;

    @Schema(description = "紧急程度：1一般 2紧急 3危重")
    private String urgency;

    @Schema(description = "状态：0待审批 1已通过 2已驳回 3已完成")
    private String referralStatus;

    private String auditOpinion;

    private String receiveHospital;

    private String receiveDept;

    private String referralResult;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
