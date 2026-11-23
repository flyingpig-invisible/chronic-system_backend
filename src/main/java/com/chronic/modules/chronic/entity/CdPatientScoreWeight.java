package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 患者健康评分权重：w1 指标达标率 / w2 用药依从率 / w3 预警密度(反向) / w4 检测完成率
 *
 * @author chronic
 */
@Data
@TableName("cd_patient_score_weight")
@Schema(description = "患者健康评分权重配置")
public class CdPatientScoreWeight implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.INPUT)
    private Long patientId;

    @Schema(description = "指标达标率权重 w1")
    private BigDecimal wRisk;

    @Schema(description = "用药依从率权重 w2")
    private BigDecimal wMed;

    @Schema(description = "预警密度权重 w3")
    private BigDecimal wAlert;

    @Schema(description = "检测完成率权重 w4")
    private BigDecimal wMonitor;

    private LocalDateTime updateTime;
}