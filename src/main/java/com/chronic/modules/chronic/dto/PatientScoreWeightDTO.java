package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 患者健康评分权重保存
 *
 * @author chronic
 */
@Data
@Schema(description = "患者健康评分权重保存")
public class PatientScoreWeightDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "患者ID")
    @NotNull(message = "请选择患者")
    private Long patientId;

    @Schema(description = "指标达标率权重 w1")
    @NotNull(message = "请填写指标达标率权重")
    @DecimalMin(value = "0", message = "权重不能小于 0")
    @DecimalMax(value = "1", message = "权重不能大于 1")
    private BigDecimal wRisk;

    @Schema(description = "用药依从率权重 w2")
    @NotNull(message = "请填写用药依从率权重")
    @DecimalMin(value = "0", message = "权重不能小于 0")
    @DecimalMax(value = "1", message = "权重不能大于 1")
    private BigDecimal wMed;

    @Schema(description = "预警密度权重 w3")
    @NotNull(message = "请填写预警密度权重")
    @DecimalMin(value = "0", message = "权重不能小于 0")
    @DecimalMax(value = "1", message = "权重不能大于 1")
    private BigDecimal wAlert;

    @Schema(description = "检测完成率权重 w4")
    @NotNull(message = "请填写检测完成率权重")
    @DecimalMin(value = "0", message = "权重不能小于 0")
    @DecimalMax(value = "1", message = "权重不能大于 1")
    private BigDecimal wMonitor;
}