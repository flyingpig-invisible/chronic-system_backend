package com.chronic.modules.kb.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库文档查询条件
 *
 * @author chronic
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识库文档查询条件")
public class KbDocumentQuery extends PageQuery {

    @Schema(description = "标题关键字")
    private String keyword;

    @Schema(description = "知识类型：1标准来源库 2辅助解释库")
    private String kbType;

    @Schema(description = "病种编码")
    private String diseaseCode;

    @Schema(description = "文档分类")
    private String category;

    @Schema(description = "状态：0草稿 1已发布")
    private String status;
}
