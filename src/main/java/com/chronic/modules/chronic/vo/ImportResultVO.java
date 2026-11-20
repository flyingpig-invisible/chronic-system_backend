package com.chronic.modules.chronic.vo;

import com.chronic.modules.chronic.support.huawei.ImportIssue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 数据导入结果
 *
 * @author chronic
 */
@Data
@Schema(description = "数据导入结果")
public class ImportResultVO {

    private Long importId;

    @Schema(description = "是否仅预览（未落库）")
    private Boolean dryRun;

    @Schema(description = "该文件之前是否已导入过（预览时仅供参考，入库时会被拦截）")
    private Boolean duplicateFile;

    private String fileName;

    private String fileType;

    @Schema(description = "解析的 json 文件数")
    private Integer fileCount;

    @Schema(description = "原始记录数")
    private Integer rawRecords;

    @Schema(description = "展开后的采样点数（聚合前）")
    private Integer parsedPoints;

    @Schema(description = "各指标成功入库条数：hr / spo2 / sleep")
    private Map<String, Integer> saved;

    private Integer successRows;

    private Integer skippedUnsupported;

    private Integer skippedInvalid;

    private Integer skippedDuplicate;

    @Schema(description = "被丢弃的睡眠夜数（时长过短或过长）")
    private Integer droppedSleepNights;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    @Schema(description = "涉及的华为设备码")
    private Set<String> deviceCodes;

    @Schema(description = "问题明细（最多 200 条）")
    private List<ImportIssue> issues;

    private Boolean warningTriggered;

    private String maxWarningLevel;

    private Integer scoreBefore;

    private Integer scoreAfter;
}
