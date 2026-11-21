package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者端建档申请（小程序注册后由医生端审核建档）
 *
 * @author chronic
 */
@Data
@TableName("mp_patient_apply")
@Schema(description = "患者端建档案申请")
public class MpPatientApply implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long applyId;

    @Schema(description = "小程序用户ID")
    private Long mpUserId;

    private String userName;

    private String userPhone;

    private String userGender;

    private LocalDate birthday;

    private String idCard;

    private String address;

    @Schema(description = "自述病种，逗号分隔的疾病编码")
    private String diseaseCodes;

    private String applyRemark;

    @Schema(description = "审核状态：0待审核 1已通过 2已驳回")
    private String applyStatus;

    @Schema(description = "审核通过后创建的档案ID")
    private Long patientId;

    private Long doctorId;

    private String doctorName;

    private String auditOpinion;

    private LocalDateTime applyTime;

    private LocalDateTime auditTime;

    // ========== 非数据库字段 ==========

    @TableField(exist = false)
    private Integer age;

    @TableField(exist = false)
    private String diseaseNames;

    @TableField(exist = false)
    private String applyStatusName;
}