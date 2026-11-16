package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 预警查询条件
 *
 * @author chronic
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "预警查询条件")
public class WarningQuery extends PageQuery {

    private Long patientId;

    private String patientName;

    private String diseaseCode;

    @Schema(description = "预警级别：1低危 2中危 3高危")
    private String warningLevel;

    @Schema(description = "处置状态：0待处理 1处理中 2已处理")
    private String handleStatus;

    private String indicatorCode;

    private String beginTime;

    private String endTime;
}
