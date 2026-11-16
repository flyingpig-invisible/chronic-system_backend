package com.chronic.modules.chronic.vo;

import com.chronic.modules.chronic.entity.CdPatientProfile;
import com.chronic.modules.chronic.entity.CdPatientProfileLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 患者专属档案响应
 *
 * @author chronic
 */
@Data
@Schema(description = "患者专属档案响应")
public class ProfileVO {

    @Schema(description = "档案主体（不存在时为 null）")
    private CdPatientProfile profile;

    @Schema(description = "当前用户可编辑的字段标识集合")
    private List<String> editableFields;

    @Schema(description = "填写留痕（最近若干条）")
    private List<CdPatientProfileLog> logs;

    @Schema(description = "当前用户角色：doctor/family")
    private String viewerRole;
}
