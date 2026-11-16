package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 监测数据查询条件
 *
 * @author chronic
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "监测数据查询条件")
public class MeasureQuery extends PageQuery {

    private Long patientId;

    private String patientName;

    private Long deviceId;

    @Schema(description = "指标编码")
    private String indicatorCode;

    @Schema(description = "状态：1 只看异常，0 只看正常，空为全部")
    private String abnormalFlag;

    private String beginTime;

    private String endTime;
}
