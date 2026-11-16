package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "就诊查询条件")
public class ClinicQuery extends PageQuery {
    private Long patientId;
    private String visitType;
    private String visitStatus;
    private String keyword;
}
