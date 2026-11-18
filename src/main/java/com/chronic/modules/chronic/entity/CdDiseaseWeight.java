package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 病种综合权重配置：患者多病共存时，各病种评分在总分中的占比
 *
 * @author chronic
 */
@Data
@TableName("cd_disease_weight")
@Schema(description = "病种综合权重配置")
public class CdDiseaseWeight implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "病种编码：1高血压 2糖尿病 3冠心病 4慢阻肺")
    private String diseaseCode;

    @Schema(description = "病种名称")
    private String diseaseName;

    @Schema(description = "该病种在综合评分中的占比(0-1)")
    private BigDecimal weight;
}