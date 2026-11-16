package com.chronic.modules.ai.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.ai.dto.AiChatDTO;
import com.chronic.modules.ai.service.AiService;
import com.chronic.modules.ai.vo.AiChatVO;
import com.chronic.modules.chronic.entity.CdAiChat;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 健康助手（医生端）
 *
 * @author chronic
 */
@Tag(name = "24-AI健康助手", description = "医生端 AI 对话")
@RestController
@RequestMapping("/chronic/ai")
@RequiredArgsConstructor
public class AiChatController {

    private final AiService aiService;

    @Operation(summary = "AI对话（可携带患者上下文）")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiService.doctorChat(dto));
    }

    @Operation(summary = "AI分析患者专属档案")
    @PostMapping("/analyze/profile/{patientId}")
    public Result<AiChatVO> analyzeProfile(@PathVariable Long patientId) {
        return Result.success(aiService.analyzeProfile(patientId));
    }

    @Operation(summary = "我的历史会话")
    @GetMapping("/history")
    public Result<List<CdAiChat>> history(@RequestParam(required = false) Integer limit) {
        return Result.success(aiService.myHistory(limit));
    }
}
