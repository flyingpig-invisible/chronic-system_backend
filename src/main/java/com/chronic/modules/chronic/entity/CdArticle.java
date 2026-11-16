package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 科普文章
 *
 * @author chronic
 */
@Data
@TableName("cd_article")
@Schema(description = "科普文章")
public class CdArticle implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long articleId;

    private String title;

    private Long categoryId;

    private String categoryName;

    private String diseaseCode;

    private String summary;

    private String content;

    private String coverImage;

    @Schema(description = "发布状态：0草稿 1已发布")
    private String publishStatus;

    private String author;

    private Integer viewCount;

    private LocalDateTime publishTime;

    @TableLogic
    private String delFlag;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
