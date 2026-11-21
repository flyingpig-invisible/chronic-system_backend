package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家属绑定申请（患者确认 + 医生审核）
 *
 * @author chronic
 */
@Data
@TableName("mp_bind_apply")
public class MpBindApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long applyId;

    private Long familyUserId;

    private String familyName;

    private Long patientId;

    private String patientName;

    private String relation;

    private String applyRemark;

    /** 患者确认：0待确认 1已同意 2已拒绝 */
    private String patientStatus;

    /** 医生审核：0待审核 1已通过 2已驳回 */
    private String doctorStatus;

    private Long doctorId;

    private String doctorName;

    private String auditOpinion;

    /** 最终状态：0申请中 1绑定成功 2已拒绝 */
    private String bindStatus;

    private LocalDateTime applyTime;

    private LocalDateTime patientTime;

    private LocalDateTime auditTime;

    // ========== 非数据库字段 ==========
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String familyPhone;

    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String doctorRealName;
}