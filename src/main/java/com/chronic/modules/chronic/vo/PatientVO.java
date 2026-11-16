package com.chronic.modules.chronic.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 患者展示对象
 *
 * @author chronic
 */
@Data
@Schema(description = "患者信息")
public class PatientVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long patientId;

    private String patientNo;

    private String name;

    private String gender;

    private LocalDate birthday;

    @Schema(description = "年龄")
    private Integer age;

    private String phone;

    private String idCard;

    private String address;

    private Long doctorId;

    private String doctorName;

    private Long deptId;

    private String deptName;

    @Schema(description = "风险等级：1低危 2中危 3高危")
    private String riskLevel;

    private Integer riskScore;

    private Integer pendingWarningCount;

    private LocalDateTime lastMeasureTime;

    private String status;

    private String remark;

    @Schema(description = "病种名称集合")
    private List<String> diseases;

    @Schema(description = "病种编码集合")
    private List<String> diseaseCodes;
}
