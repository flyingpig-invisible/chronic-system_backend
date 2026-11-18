package com.gzu.chronic.controller;

import com.gzu.chronic.common.BizException;
import com.gzu.chronic.common.R;
import com.gzu.chronic.common.Repo;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/common")
public class CommonController {

    private final Repo repo;

    public CommonController(Repo repo) {
        this.repo = repo;
    }

    /** 文件上传：保存到 ./uploads，返回可直接访问的 url */
    @PostMapping("/upload")
    public R upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BizException(1001, "文件为空");
        }
        String origin = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String name = System.currentTimeMillis() + "_" + origin.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads").toAbsolutePath();
        Files.createDirectories(dir);
        file.transferTo(dir.resolve(name).toFile());

        return R.ok(Repo.of(
                "url", "/api/files/" + name,
                "name", origin,
                "size", file.getSize()));
    }

    @GetMapping("/dict/{type}")
    public R dict(@PathVariable("type") String type) {
        switch (type) {
            case "disease":
                return R.ok(List.of("高血压", "2型糖尿病", "冠心病", "慢阻肺"));
            case "relation":
                return R.ok(List.of("父亲", "母亲", "儿子", "女儿", "配偶", "其他"));
            case "dept":
                return R.ok(repo.list("select distinct dept from t_doctor order by dept"));
            default:
                throw new BizException(1004, "未知字典类型：" + type);
        }
    }
}
