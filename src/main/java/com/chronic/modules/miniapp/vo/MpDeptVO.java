package com.chronic.modules.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 患者端科室下拉选项
 *
 * @author chronic
 */
@Data
@Schema(description = "科室选项")
public class MpDeptVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long deptId;

    private String deptName;
}