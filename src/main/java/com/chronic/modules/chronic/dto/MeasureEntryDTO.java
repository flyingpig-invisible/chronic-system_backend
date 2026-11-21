package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 一次测量中的单个指标录入项
 *
 * @author chronic
 */
@Data
@Schema(description = "单次测量中的一项指标")
public class MeasureEntryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择指标")
    @Schema(description = "指标编码，必须来自指标字典")
    private String indicatorCode;

    @NotNull(message = "请输入数值")
    @Schema(description = "指标数值")
    private BigDecimal value;
}