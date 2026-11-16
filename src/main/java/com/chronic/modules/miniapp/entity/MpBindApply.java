package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家属-患者绑定申请
 *
 * @author chronic
 */
@Data
@TableName("mp_bind_apply")
@Schema(description = "家属绑定申请")
public class MpBindApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long applyId;

    private Long familyUserId;

    private String familyName;

    private Long patientId;

    private String patientName;

    @Schema(description = "与患者关系")
    private String relation;

    private String applyRemark;

    @Schema(description = "患者确认状态：0待确认 1已同意 2已拒绝")
    private String patientStatus;

    @Schema(description = "医生审核状态：0待审核 1已通过 2已驳回")
    private String doctorStatus;

    private Long doctorId;

    private String doctorName;

    private String auditOpinion;

    @Schema(description = "最终绑定状态：0申请中 1绑定成功 2已拒绝")
    private String bindStatus;

    private LocalDateTime applyTime;

    private LocalDateTime patientTime;

    private LocalDateTime auditTime;
}
