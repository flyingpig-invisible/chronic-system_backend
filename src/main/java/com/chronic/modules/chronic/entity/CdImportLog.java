package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 数据导入日志（华为运动健康导出包）
 *
 * @author chronic
 */
@Data
@TableName("cd_import_log")
@Schema(description = "数据导入日志")
public class CdImportLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long importId;

    private Long patientId;

    private String patientName;

    @Schema(description = "来源设备ID，可为空")
    private Long deviceId;

    private String fileName;

    @Schema(description = "ZIP / JSON")
    private String fileType;

    @Schema(description = "文件MD5，用于幂等去重")
    private String fileHash;

    @Schema(description = "解析的 json 文件数")
    private Integer fileCount;

    @Schema(description = "原始记录数")
    private Integer rawRecords;

    @Schema(description = "展开后的采样点数（聚合前）")
    private Integer parsedPoints;

    @Schema(description = "文件总行数（兼容旧字段，等于 rawRecords）")
    private Integer totalRows;

    @Schema(description = "成功入库条数")
    private Integer successRows;

    @Schema(description = "失败/跳过条数")
    private Integer failedRows;

    @Schema(description = "其中因重复跳过条数")
    private Integer skipDupRows;

    @Schema(description = "失败明细 JSON")
    private String errorDetail;

    @Schema(description = "数据来源：1设备同步 2手动录入 3文件导入")
    private String dataSource;

    @Schema(description = "1全部成功 2部分成功 3失败")
    private String status;

    private Long operatorUserId;

    private String operatorName;

    private LocalDate dateFrom;

    private LocalDate dateTo;

    private LocalDateTime importTime;
}
