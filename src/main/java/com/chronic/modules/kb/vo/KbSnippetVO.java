package com.chronic.modules.kb.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 知识库检索片段（供 AI 引用与前端展示）
 *
 * @author chronic
 */
@Data
@Schema(description = "知识库检索片段")
public class KbSnippetVO {

    @Schema(description = "文档ID")
    private Long docId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "知识类型：1标准来源库 2辅助解释库")
    private String kbType;

    @Schema(description = "病种编码")
    private String diseaseCode;

    @Schema(description = "原文位置（出处）")
    private String sourceLocation;

    @Schema(description = "发布单位")
    private String publisher;

    @Schema(description = "版本")
    private String version;

    @Schema(description = "片段正文（已按长度截断）")
    private String content;

    @Schema(description = "相关度得分")
    private Integer score;
}
