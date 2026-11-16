package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 评分权重配置
 *
 * @author chronic
 */
@Data
@TableName("cd_score_weight")
@Schema(description = "评分权重配置")
public class CdScoreWeight implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long weightId;

    private String diseaseCode;

    private String indicatorCode;

    private String indicatorName;

    @Schema(description = "权重(0-1)")
    private BigDecimal weight;
}
