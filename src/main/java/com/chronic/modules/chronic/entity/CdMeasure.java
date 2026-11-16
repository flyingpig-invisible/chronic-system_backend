package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 监测数据
 *
 * @author chronic
 */
@Data
@TableName("cd_measure")
@Schema(description = "监测数据")
public class CdMeasure implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long measureId;

    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    private Long deviceId;

    private String diseaseCode;

    @Schema(description = "指标编码：sbp/dbp/hr/spo2/glu/rr")
    private String indicatorCode;

    private String indicatorName;

    @Schema(description = "指标数值")
    private BigDecimal value;

    private String unit;

    private LocalDateTime measureTime;

    @Schema(description = "数据来源：1设备 2手动")
    private String dataSource;

    @Schema(description = "异常标记：0正常 1异常")
    private String abnormalFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String beginTime;

    @TableField(exist = false)
    private String endTime;
}
