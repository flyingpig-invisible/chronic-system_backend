package com.chronic.modules.miniapp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 小程序端一组指标录入结果
 *
 * @author chronic
 */
@Data
@Schema(description = "小程序端一组指标录入结果")
public class MpMeasureVO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "本次写入的记录数")
    private int savedCount;

    @Schema(description = "跳过的空值指标数")
    private int skippedCount;

    @Schema(description = "是否触发预警")
    private boolean warningTriggered;

    @Schema(description = "最高预警级别：1低危 2中危 3高危")
    private String maxWarningLevel;

    @Schema(description = "各项回显")
    private List<Item> items = new ArrayList<>();

    /**
     * 单项结果
     */
    @Data
    @Schema(description = "单项录入结果")
    public static class Item implements Serializable {

        private static final long serialVersionUID = 1L;

        @Schema(description = "监测记录ID")
        private Long measureId;

        @Schema(description = "指标编码")
        private String indicatorCode;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "数值")
        private String value;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "是否异常：0正常 1异常")
        private String abnormalFlag;

        @Schema(description = "预警级别：1低危 2中危 3高危，未触发时为空")
        private String warningLevel;

        @Schema(description = "异常提示")
        private String warningTip;

        @Schema(description = "是否触发预警")
        private boolean warningTriggered;
    }
}
