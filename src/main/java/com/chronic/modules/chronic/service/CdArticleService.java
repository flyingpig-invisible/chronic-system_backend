package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdArticle;
import com.chronic.modules.chronic.entity.CdArticleCategory;
import com.chronic.modules.chronic.query.ArticleQuery;

import java.util.List;

public interface CdArticleService extends IService<CdArticle> {

    PageResult<CdArticle> queryPage(ArticleQuery query);

    CdArticle getDetail(Long articleId);

    void saveArticle(CdArticle article);

    void publish(Long articleId, String publishStatus);

    List<CdArticleCategory> categoryList();
}
