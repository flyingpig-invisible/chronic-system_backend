package com.chronic.modules.chronic.query;

import com.chronic.common.page.PageQuery;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "科普文章查询条件")
public class ArticleQuery extends PageQuery {
    private Long categoryId;
    private String diseaseCode;
    private String publishStatus;
    private String keyword;
}
