package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdArticle;
import com.chronic.modules.chronic.entity.CdArticleCategory;
import com.chronic.modules.chronic.mapper.CdArticleCategoryMapper;
import com.chronic.modules.chronic.mapper.CdArticleMapper;
import com.chronic.modules.chronic.query.ArticleQuery;
import com.chronic.modules.chronic.service.CdArticleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CdArticleServiceImpl extends ServiceImpl<CdArticleMapper, CdArticle> implements CdArticleService {

    @Autowired
    private CdArticleCategoryMapper categoryMapper;

    @Override
    public PageResult<CdArticle> queryPage(ArticleQuery query) {
        LambdaQueryWrapper<CdArticle> w = new LambdaQueryWrapper<>();
        w.eq(query.getCategoryId() != null, CdArticle::getCategoryId, query.getCategoryId())
         .eq(StringUtils.hasText(query.getDiseaseCode()), CdArticle::getDiseaseCode, query.getDiseaseCode())
         .eq(StringUtils.hasText(query.getPublishStatus()), CdArticle::getPublishStatus, query.getPublishStatus())
         .and(StringUtils.hasText(query.getKeyword()), q -> q
                 .like(CdArticle::getTitle, query.getKeyword())
                 .or().like(CdArticle::getSummary, query.getKeyword()))
         .orderByDesc(CdArticle::getPublishTime)
         .orderByDesc(CdArticle::getCreateTime);
        Page<CdArticle> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public CdArticle getDetail(Long articleId) {
        CdArticle article = getById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        article.setViewCount(article.getViewCount() == null ? 1 : article.getViewCount() + 1);
        updateById(article);
        return article;
    }

    @Override
    public void saveArticle(CdArticle article) {
        if (!StringUtils.hasText(article.getTitle())) {
            throw new ServiceException("标题不能为空");
        }
        if (article.getCategoryId() != null) {
            CdArticleCategory category = categoryMapper.selectById(article.getCategoryId());
            if (category != null) {
                article.setCategoryName(category.getCategoryName());
            }
        }
        if (!StringUtils.hasText(article.getPublishStatus())) {
            article.setPublishStatus("0");
        }
        if ("1".equals(article.getPublishStatus()) && article.getPublishTime() == null) {
            article.setPublishTime(LocalDateTime.now());
        }
        if (article.getViewCount() == null) {
            article.setViewCount(0);
        }
        saveOrUpdate(article);
    }

    @Override
    public void publish(Long articleId, String publishStatus) {
        CdArticle article = getById(articleId);
        if (article == null) {
            throw new ServiceException("文章不存在");
        }
        article.setPublishStatus(publishStatus);
        if ("1".equals(publishStatus)) {
            article.setPublishTime(LocalDateTime.now());
        }
        updateById(article);
    }

    @Override
    public List<CdArticleCategory> categoryList() {
        return categoryMapper.selectList(new LambdaQueryWrapper<CdArticleCategory>()
                .eq(CdArticleCategory::getStatus, "0")
                .orderByAsc(CdArticleCategory::getOrderNum));
    }
}
