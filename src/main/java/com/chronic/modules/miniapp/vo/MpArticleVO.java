package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 健康科普文章
 *
 * @author chronic
 */
@Data
public class MpArticleVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long articleId;

    private String title;

    private String categoryName;

    private String diseaseCode;

    private String summary;

    private String content;

    private String coverImage;

    private String author;

    private Integer viewCount;

    private LocalDateTime publishTime;
}