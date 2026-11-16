package com.chronic.modules.chronic.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 一次测量（同一患者同一时刻录入的一组指标）
 *
 * @author chronic
 */
@Data
@Schema(description = "一次测量记录")
public class MeasureGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "分组键：患者ID_测量时间")
    private String groupKey;

    @Schema(description = "患者ID")
    private Long patientId;

    @Schema(description = "患者姓名")
    private String patientName;

    @Schema(description = "测量时间")
    private String measureTime;

    @Schema(description = "数据来源：1设备 2手动")
    private String dataSource;

    @Schema(description = "是否含异常指标")
    private boolean hasAbnormal;

    @Schema(description = "异常项数量")
    private int abnormalCount;

    @Schema(description = "本组记录ID集合（用于删除）")
    private List<Long> measureIds = new ArrayList<>();

    @Schema(description = "全部指标（含未录入项，未录入时 value 为空）")
    private List<Item> items = new ArrayList<>();

    /**
     * 表格内一行
     */
    @Data
    @Schema(description = "指标行")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "指标编码")
        private String indicatorCode;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "数值，未录入为 null")
        private String value;

        @Schema(description = "是否异常：0正常 1异常，未录入为空")
        private String abnormalFlag;

        @Schema(description = "记录ID，未录入为 null")
        private Long measureId;
    }
}
