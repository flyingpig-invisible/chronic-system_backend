package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 健康计划项
 *
 * @author chronic
 */
@Data
@TableName("cd_plan_item")
@Schema(description = "健康计划项")
public class CdPlanItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long planId;

    @Schema(description = "类型：1饮食 2运动 3监测 4用药")
    private String itemType;

    private String content;

    private String frequency;

    private String targetValue;
}
