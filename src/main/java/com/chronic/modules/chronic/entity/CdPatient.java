package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 慢病患者档案
 *
 * @author chronic
 */
@Data
@TableName("cd_patient")
@Schema(description = "慢病患者档案")
public class CdPatient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long patientId;

    @Schema(description = "患者编号")
    private String patientNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "性别：1男 0女 2未知")
    private String gender;

    @Schema(description = "出生日期")
    private LocalDate birthday;

    private String phone;

    private String idCard;

    private String address;

    @Schema(description = "关联小程序用户ID")
    private Long mpUserId;

    @Schema(description = "责任医生ID")
    private Long doctorId;

    private Long deptId;
    @Schema(description = "负责医师姓名，物理列不存在，按责任医生实时填充" )
    @TableField(exist = false)
    private String physicianName;

    @Schema(description = "风险等级：1低危 2中危 3高危")
    private String riskLevel;

    @Schema(description = "风险评分")
    private Integer riskScore;

    /** 健康评分(0-100，越高越健康) */
    private Integer healthScore;

    @Schema(description = "待处理预警数")
    private Integer pendingWarningCount;

    private LocalDateTime lastMeasureTime;

    @Schema(description = "状态：0管理中 1已结案")
    private String status;

    @TableLogic
    private String delFlag;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    private String remark;

    // ========== 非数据库字段 ==========

    @Schema(description = "年龄（由出生日期计算）")
    @TableField(exist = false)
    private Integer age;

    @Schema(description = "责任医生姓名")
    @TableField(exist = false)
    private String doctorName;

    @Schema(description = "科室名称")
    @TableField(exist = false)
    private String deptName;

    @Schema(description = "病种名称集合")
    @TableField(exist = false)
    private List<String> diseases;

    @Schema(description = "病种编码集合（提交用）")
    @TableField(exist = false)
    private List<String> diseaseCodes;
    @Schema(description = "患者可见科室ID（提交用，只有这些科室的医生能查看该患者）")
    @TableField(exist = false)
    private List<Long> deptIds;
}
