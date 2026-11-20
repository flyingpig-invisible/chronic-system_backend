package com.chronic.modules.system.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "指标查询条件")
public class IndicatorQuery extends PageQuery {

    @Schema(description = "指标名称/编码关键字")
    private String keyword;

    @Schema(description = "状态(0正常 1停用)")
    private String status;
}