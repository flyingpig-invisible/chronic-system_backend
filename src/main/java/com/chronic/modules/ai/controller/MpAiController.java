package com.chronic.modules.ai.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.ai.dto.AiChatDTO;
import com.chronic.modules.ai.service.AiService;
import com.chronic.modules.ai.vo.AiChatVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 健康助手（小程序端）
 *
 * @author chronic
 */
@Tag(name = "25-小程序AI助手", description = "患者/家属 AI 对话")
@RestController
@RequestMapping("/mp/ai")
@RequiredArgsConstructor
public class MpAiController {

    private final AiService aiService;

    @Operation(summary = "AI分析患者专属档案（家属端）")
    @PostMapping("/analyze/profile/{patientId}")
    public Result<AiChatVO> analyzeProfile(@PathVariable Long patientId) {
        return Result.success(aiService.analyzeProfileForFamily(patientId));
    }

    @Operation(summary = "AI健康咨询（自动注入当前患者上下文）")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiService.mpChat(dto));
    }
}
