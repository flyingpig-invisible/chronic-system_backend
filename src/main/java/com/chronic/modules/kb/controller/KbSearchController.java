package com.chronic.modules.kb.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.kb.service.KbSearchService;
import com.chronic.modules.kb.vo.KbSnippetVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 知识库检索（知识来源预览）
 *
 * @author chronic
 */
@Tag(name = "26-知识库检索", description = "按病种与问题检索知识库片段，展示 AI 回答的知识依据")
@RestController
@RequestMapping("/chronic/kb/search")
@RequiredArgsConstructor
public class KbSearchController {

    private final KbSearchService searchService;

    @Operation(summary = "检索知识库片段（医生端预览 AI 知识依据）")
    @PreAuthorize("@ss.hasPermi('chronic:kb:search')")
    @GetMapping
    public Result<List<KbSnippetVO>> search(@RequestParam(required = false) String diseaseCode,
                                            @RequestParam(required = false) String question,
                                            @RequestParam(required = false, defaultValue = "3") Integer topK) {
        List<String> codes = diseaseCode == null || diseaseCode.isBlank()
                ? Collections.emptyList() : Arrays.asList(diseaseCode.split(","));
        return Result.success(searchService.search(codes, question, topK == null ? 3 : topK));
    }

    @Operation(summary = "预览将注入 AI 的知识上下文")
    @PreAuthorize("@ss.hasPermi('chronic:kb:search')")
    @GetMapping("/context")
    public Result<Map<String, Object>> context(@RequestParam(required = false) String diseaseCode,
                                               @RequestParam(required = false) String question) {
        List<String> codes = diseaseCode == null || diseaseCode.isBlank()
                ? Collections.emptyList() : Arrays.asList(diseaseCode.split(","));
        String ctx = searchService.buildKnowledgeContext(codes, question);
        return Result.success(Map.of(
                "hits", searchService.countHits(codes, question),
                "context", ctx));
    }
}
