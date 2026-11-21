package com.chronic.modules.chronic.controller;

import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传（问诊主诉附图等），医生端与小程序共用
 *
 * @author chronic
 */
@Tag(name = "24-图片上传", description = "问诊主诉附图上传")
@RestController
@RequestMapping("/chronic/upload")
public class UploadController {

    private static final List<String> ALLOWED_EXT = List.of("jpg", "jpeg", "png", "gif", "webp", "bmp");
    private static final long MAX_SIZE = 5 * 1024 * 1024L;

    @Value("${chronic.upload-path}")
    private String uploadPath;

    @Operation(summary = "上传图片，返回可访问的相对地址")
    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的图片");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("图片不能超过 5MB");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String ext = original.contains(".")
                ? original.substring(original.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ServiceException("仅支持 jpg / png / gif / webp / bmp 格式");
        }
        String dir = "clinic/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path target = Paths.get(uploadPath, dir).toAbsolutePath().normalize();
            Files.createDirectories(target);
            file.transferTo(target.resolve(name).toFile());
        } catch (IOException e) {
            throw new ServiceException("图片保存失败，请重试");
        }
        Map<String, String> data = new LinkedHashMap<>();
        data.put("url", "/upload/" + dir + "/" + name);
        return Result.success(data);
    }
}