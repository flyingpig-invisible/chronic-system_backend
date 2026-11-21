package com.chronic.modules.miniapp.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家属-患者关联
 *
 * @author chronic
 */
@Data
@TableName("mp_family_patient")
public class MpFamilyPatient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long familyUserId;

    private Long patientId;

    private String relation;

    private LocalDateTime bindTime;
}