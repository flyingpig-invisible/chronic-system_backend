package com.chronic.modules.chronic.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 一次测量（多指标）录入参数
 *
 * @author chronic
 */
@Data
@Schema(description = "一次测量录入多个指标")
public class MeasureOnceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "请选择患者")
    private Long patientId;

    @Schema(description = "病种编码，可空")
    private String diseaseCode;

    @Schema(description = "测量时刻，空则取当前时间")
    private LocalDateTime measureTime;

    @Schema(description = "数据来源：1设备 2手动，默认 2")
    private String dataSource;

    @Valid
    @NotEmpty(message = "请至少录入一项指标")
    private List<MeasureEntryDTO> entries;
}