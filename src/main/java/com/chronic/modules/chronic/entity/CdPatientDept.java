package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 患者可见科室：只有这些科室的医生能查看该患者信息
 *
 * @author chronic
 */
@Data
@TableName("cd_patient_dept")
public class CdPatientDept implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long patientId;

    private Long deptId;

    /** 来源：1患者自选 2医生代选 */
    private String source;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}