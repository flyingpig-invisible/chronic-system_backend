package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用药提醒查询条件")
public class RemindQuery extends PageQuery {
    private Long patientId;
    private String status;
    private String keyword;
}
