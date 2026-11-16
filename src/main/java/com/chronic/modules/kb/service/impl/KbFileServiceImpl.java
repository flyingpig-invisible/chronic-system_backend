package com.chronic.modules.kb.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.kb.entity.KbDocument;
import com.chronic.modules.kb.mapper.KbDocumentMapper;
import com.chronic.modules.kb.service.KbFileService;
import com.chronic.modules.kb.vo.KbFileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * 知识库文件服务实现
 *
 * <p>哈希由服务端对文件字节流计算 SHA-256，前端不参与，避免被篡改或填错。</p>
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbFileServiceImpl implements KbFileService {

    private final KbDocumentMapper documentMapper;

    @Value("${chronic.upload-path}")
    private String uploadPath;

    /** 允许上传的文件类型 */
    private static final List<String> ALLOWED_EXT = Arrays.asList("pdf", "txt", "md", "doc", "docx");
    /** 单文件最大 20MB */
    private static final long MAX_SIZE = 20L * 1024 * 1024;

    @Override
    public KbFileVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要上传的文件");
        }
        if (file.getSize() > MAX_SIZE) {
            throw new ServiceException("文件大小不能超过 20MB");
        }

        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            originalName = "unnamed";
        }
        String ext = extensionOf(originalName);
        if (!ALLOWED_EXT.contains(ext)) {
            throw new ServiceException("不支持的文件类型：" + ext + "，仅支持 " + String.join("/", ALLOWED_EXT));
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败：" + e.getMessage());
        }

        // 服务端计算 SHA-256，前端不传
        String hash = sha256(bytes);

        KbFileVO vo = new KbFileVO();
        vo.setFileName(originalName);
        vo.setFileHash(hash);
        vo.setFileSize((long) bytes.length);
        vo.setFileSizeText(readableSize(bytes.length));
        vo.setFileExt(ext);

        // 查重：同一份文件不重复入库
        KbDocument exists = documentMapper.selectOne(Wrappers.<KbDocument>lambdaQuery()
                .eq(KbDocument::getFileHash, hash).last("limit 1"));
        if (exists != null) {
            vo.setDuplicate(true);
            vo.setDuplicateDocId(exists.getDocId());
            vo.setDuplicateDocTitle(exists.getTitle());
            vo.setFileUrl(exists.getFileUrl());
            return vo;
        }

        // 落盘：upload/kb/2026-10-08/uuid.pdf
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String storedName = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(uploadPath, "kb", dateDir);
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(storedName);
            Files.write(target, bytes);
            vo.setFileUrl("/upload/kb/" + dateDir + "/" + storedName);
        } catch (IOException e) {
            throw new ServiceException("保存文件失败：" + e.getMessage());
        }

        // PDF / 纯文本自动提取正文，省去手工粘贴
        String content = extractText(bytes, ext);
        if (StringUtils.hasText(content)) {
            vo.setContent(content);
        }
        log.info("知识库文件上传成功：{}，hash={}，size={}", originalName, hash, bytes.length);
        return vo;
    }

    /** 计算 SHA-256 */
    private String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ServiceException("计算文件哈希失败：" + e.getMessage());
        }
    }

    /** 提取正文：PDF 用 PDFBox；txt/md 直接按 UTF-8 读 */
    private String extractText(byte[] bytes, String ext) {
        try {
            if ("txt".equals(ext) || "md".equals(ext)) {
                return new String(bytes, StandardCharsets.UTF_8);
            }
            if ("pdf".equals(ext)) {
                try (PDDocument doc = Loader.loadPDF(bytes)) {
                    PDFTextStripper stripper = new PDFTextStripper();
                    String text = stripper.getText(doc);
                    return text == null ? null : text.trim();
                }
            }
        } catch (Exception e) {
            // 提取失败不影响上传，正文可由人工补充
            log.warn("正文提取失败({})：{}", ext, e.getMessage());
        }
        return null;
    }

    private String extensionOf(String name) {
        int i = name.lastIndexOf('.');
        if (i < 0 || i == name.length() - 1) {
            return "";
        }
        return name.substring(i + 1).toLowerCase();
    }

    private String readableSize(long size) {
        if (size < 1024) {
            return size + " B";
        }
        if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        }
        return String.format("%.2f MB", size / 1024.0 / 1024.0);
    }
}
