package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 评分权重保存（按病种整体覆盖）
 */
@Data
@Schema(description = "评分权重保存")
public class ScoreWeightSaveDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "病种编码")
    @NotBlank(message = "请选择病种")
    private String diseaseCode;

    @Schema(description = "指标权重列表")
    @NotEmpty(message = "请至少配置一个指标")
    @Valid
    private List<Item> items;

    @Data
    @Schema(description = "单个指标权重")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "指标编码")
        @NotBlank(message = "指标编码不能为空")
        private String indicatorCode;

        @Schema(description = "指标名称")
        @NotBlank(message = "指标名称不能为空")
        private String indicatorName;

        @Schema(description = "权重(0-1)")
        private BigDecimal weight;
    }
}