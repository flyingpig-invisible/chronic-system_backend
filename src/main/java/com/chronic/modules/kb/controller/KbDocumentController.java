package com.chronic.modules.kb.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.kb.entity.KbDocument;
import com.chronic.modules.kb.query.KbDocumentQuery;
import com.chronic.modules.kb.service.KbDocumentService;
import com.chronic.modules.kb.service.KbFileService;
import com.chronic.modules.kb.vo.KbFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 医学知识库
 *
 * @author chronic
 */
@Tag(name = "25-医学知识库", description = "知识库文档阅读与维护")
@RestController
@RequestMapping("/chronic/kb")
@RequiredArgsConstructor
public class KbDocumentController {

    private final KbDocumentService documentService;
    private final KbFileService fileService;

    @Operation(summary = "分页查询知识文档")
    @PreAuthorize("@ss.hasPermi('chronic:kb:list')")
    @GetMapping("/list")
    public Result<PageResult<KbDocument>> list(KbDocumentQuery query) {
        return Result.success(documentService.pageDocument(query));
    }

    @Operation(summary = "获取知识文档详情")
    @PreAuthorize("@ss.hasPermi('chronic:kb:query')")
    @GetMapping("/{docId:\\d+}")
    public Result<KbDocument> getInfo(@PathVariable Long docId) {
        return Result.success(documentService.getDocument(docId));
    }

    @Operation(summary = "上传知识库文件（服务端自动计算 SHA-256）")
    @PreAuthorize("@ss.hasPermi('chronic:kb:edit')")
    @PostMapping("/upload")
    public Result<KbFileVO> upload(@RequestParam("file") MultipartFile file) {
        return Result.success("上传成功", fileService.upload(file));
    }

    @Operation(summary = "新增或修改知识文档")
    @PreAuthorize("@ss.hasPermi('chronic:kb:edit')")
    @PostMapping
    public Result<Long> save(@RequestBody KbDocument doc) {
        return Result.success("保存成功", documentService.saveDocument(doc));
    }

    @Operation(summary = "删除知识文档")
    @PreAuthorize("@ss.hasPermi('chronic:kb:remove')")
    @DeleteMapping("/{docIds:\\d+(,\\d+)*}")
    public Result<Void> remove(@PathVariable Long[] docIds) {
        documentService.deleteDocument(docIds);
        return Result.success();
    }

    @Operation(summary = "知识库统计")
    @PreAuthorize("@ss.hasPermi('chronic:kb:list')")
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.success(documentService.stats());
    }
}
