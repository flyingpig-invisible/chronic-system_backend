package com.chronic.modules.kb.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 知识库文档（医学知识库栏目）
 *
 * @author chronic
 */
@Data
@TableName("kb_document")
@Schema(description = "知识库文档")
public class KbDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long docId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "知识类型：1标准来源库 2辅助解释库")
    private String kbType;

    @Schema(description = "病种：1高血压 2糖尿病 3冠心病 4慢阻肺")
    private String diseaseCode;

    @Schema(description = "文档分类：指南/规范/科普/FAQ/专家说明")
    private String category;

    @Schema(description = "摘要")
    private String summary;

    @Schema(description = "正文内容")
    private String content;

    @Schema(description = "发布单位/来源机构")
    private String publisher;

    @Schema(description = "发布日期")
    private String publishDate;

    @Schema(description = "生效日期")
    private String effectiveDate;

    @Schema(description = "适用人群")
    private String applicableScope;

    @Schema(description = "文件版本，如 2024版")
    private String version;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "原始文件地址")
    private String fileUrl;

    @Schema(description = "文件哈希（SHA-256）")
    private String fileHash;

    @Schema(description = "原文位置，如 第3.2节")
    private String sourceLocation;

    @Schema(description = "标签，逗号分隔")
    private String tags;

    @Schema(description = "检索关键词，逗号分隔")
    private String keywords;

    @Schema(description = "状态：0草稿 1已发布")
    private String status;

    @Schema(description = "浏览次数")
    private Integer viewCount;

    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    private String updateBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private String delFlag;
}
