package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家属患者关联
 *
 * @author chronic
 */
@Data
@TableName("mp_family_patient")
@Schema(description = "家属患者关联")
public class MpFamilyPatient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyUserId;

    private Long patientId;

    private String relation;

    private LocalDateTime bindTime;

    /** 患者姓名（非表字段，仅用于小程序展示） */
    @TableField(exist = false)
    private String patientName;
}
