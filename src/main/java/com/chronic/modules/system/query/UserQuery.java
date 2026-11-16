package com.chronic.modules.system.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "用户查询条件")
public class UserQuery extends PageQuery {
    private String username;
    private String realName;
    private String phone;
    private Long deptId;
    private String userType;
    private String status;
}
