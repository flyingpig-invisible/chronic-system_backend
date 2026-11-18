package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import com.gzu.chronic.common.Views;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ContentController {

    private final Repo repo;

    public ContentController(Repo repo) {
        this.repo = repo;
    }

    /* ---------------- 食谱 ---------------- */

    @GetMapping("/recipe/list")
    public R recipes(@RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                     @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
                     @RequestParam(name = "tag", required = false) String tag) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;

        List<Map<String, Object>> rows;
        long total;
        if (tag == null || tag.isBlank()) {
            total = repo.count("select count(*) from t_recipe");
            rows = repo.list("select * from t_recipe order by id limit ? offset ?", size, offset);
        } else {
            total = repo.count("select count(*) from t_recipe where tags like ?", "%" + tag + "%");
            rows = repo.list("select * from t_recipe where tags like ? order by id limit ? offset ?", "%" + tag + "%", size, offset);
        }
        rows.replaceAll(Views::recipe);
        return R.ok(Repo.page(rows, total));
    }

    @GetMapping("/recipe/{id}")
    public R recipe(@PathVariable("id") String id) {
        Map<String, Object> row = repo.one("select * from t_recipe where id=?", id);
        if (row == null) {
            throw new BizException(1004, "食谱不存在");
        }
        return R.ok(Views.recipe(row));
    }

    /* ---------------- 健康指导 ---------------- */

    @GetMapping("/guide/categories")
    public R categories() {
        return R.ok(repo.list("select name from t_guide_category order by sort_no"));
    }

    @GetMapping("/guide/list")
    public R guides(@RequestParam(name = "category", required = false) String category) {
        List<Map<String, Object>> rows = (category == null || category.isBlank())
                ? repo.list("select * from t_guide order by id")
                : repo.list("select * from t_guide where category=? order by id", category);
        rows.replaceAll(Views::guide);
        return R.ok(rows);
    }

    /* ---------------- 文章 ---------------- */

    @GetMapping("/article/list")
    public R articles(@RequestParam(name = "pageNum", defaultValue = "1") int pageNum,
                      @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int size = Math.min(Math.max(pageSize, 1), 50);
        int offset = (Math.max(pageNum, 1) - 1) * size;
        List<Map<String, Object>> rows = repo.list("select * from t_article order by id limit ? offset ?", size, offset);
        long total = repo.count("select count(*) from t_article");
        rows.replaceAll(Views::article);
        return R.ok(Repo.page(rows, total));
    }

    @GetMapping("/article/{id}")
    public R article(@PathVariable("id") String id) {
        Map<String, Object> row = repo.one("select * from t_article where id=?", id);
        if (row == null) {
            throw new BizException(1004, "文章不存在");
        }
        return R.ok(Views.article(row));
    }
}
