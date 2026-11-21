package com.chronic.modules.miniapp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 患者端选择可见科室参数
 *
 * @author chronic
 */
@Data
@Schema(description = "患者可见科室")
public class MpDeptSelectDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "科室ID集合，至少一个")
    private List<Long> deptIds;
}