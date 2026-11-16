package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdArticle;
import com.chronic.modules.chronic.entity.CdArticleCategory;
import com.chronic.modules.chronic.query.ArticleQuery;
import com.chronic.modules.chronic.service.CdArticleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康科普
 *
 * @author chronic
 */
@Tag(name = "10-健康科普", description = "科普文章与分类管理")
@RestController
@RequestMapping("/chronic/article")
@RequiredArgsConstructor
public class CdArticleController {

    private final CdArticleService articleService;

    @Operation(summary = "分页查询文章列表")
    @PreAuthorize("@ss.hasPermi('chronic:article:list')")
    @GetMapping("/list")
    public Result<PageResult<CdArticle>> list(ArticleQuery query) {
        return Result.success(articleService.queryPage(query));
    }

    @Operation(summary = "获取文章详情")
    @PreAuthorize("@ss.hasPermi('chronic:article:list')")
    @GetMapping("/{articleId}")
    public Result<CdArticle> getInfo(@PathVariable Long articleId) {
        return Result.success(articleService.getDetail(articleId));
    }

    @Operation(summary = "新增或修改文章")
    @PreAuthorize("@ss.hasPermi('chronic:article:edit')")
    @PostMapping
    public Result<Void> save(@RequestBody CdArticle article) {
        articleService.saveArticle(article);
        return Result.success();
    }

    @Operation(summary = "发布或下架文章")
    @PreAuthorize("@ss.hasPermi('chronic:article:edit')")
    @PutMapping("/{articleId}/publish")
    public Result<Void> publish(@PathVariable Long articleId, @RequestParam String publishStatus) {
        articleService.publish(articleId, publishStatus);
        return Result.success();
    }

    @Operation(summary = "删除文章")
    @PreAuthorize("@ss.hasPermi('chronic:article:remove')")
    @DeleteMapping("/{articleIds}")
    public Result<Void> remove(@PathVariable Long[] articleIds) {
        articleService.removeByIds(java.util.Arrays.asList(articleIds));
        return Result.success();
    }

    @Operation(summary = "查询科普分类")
    @PreAuthorize("@ss.hasPermi('chronic:category:list')")
    @GetMapping("/category/list")
    public Result<List<CdArticleCategory>> categoryList() {
        return Result.success(articleService.categoryList());
    }
}
