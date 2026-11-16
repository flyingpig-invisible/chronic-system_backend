package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 患者查询条件
 *
 * @author chronic
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "患者查询条件")
public class PatientQuery extends PageQuery {

    @Schema(description = "患者编号")
    private String patientNo;

    @Schema(description = "姓名")
    private String name;

    @Schema(description = "关键字（姓名或手机号，任一模糊匹配）")
    private String keyword;

    @Schema(description = "联系电话")
    private String phone;

    @Schema(description = "风险等级：1低危 2中危 3高危")
    private String riskLevel;

    @Schema(description = "状态：0管理中 1已结案")
    private String status;

    @Schema(description = "责任医生ID")
    private Long doctorId;

    @Schema(description = "病种编码列表")
    private List<String> diseaseCodes;
}
