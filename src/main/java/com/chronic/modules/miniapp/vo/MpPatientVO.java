package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者档案（字段与医生端 cd_patient 一致，患者端按需脱敏）
 *
 * @author chronic
 */
@Data
public class MpPatientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long patientId;

    private String patientNo;

    private String name;

    /** 1男 0女 2未知（与医生端一致） */
    private String gender;

    private LocalDate birthday;

    private Integer age;

    private String phone;

    /** 身份证号，患者端仅展示脱敏值 */
    private String idCard;

    private String address;

    private Long doctorId;

    private String doctorName;

    private Long deptId;

    private String deptName;

    private String riskLevel;

    private String riskLevelName;

    /** 健康评分(0-100，越高越健康) */
    private Integer healthScore;

    private Integer riskScore;

    private Integer pendingWarningCount;

    private LocalDateTime lastMeasureTime;

    private String status;

    private String remark;

    private List<String> diseases;

    private List<String> diseaseCodes;

    /** 亲属关系，患者本人为空 */
    private String relation;
}