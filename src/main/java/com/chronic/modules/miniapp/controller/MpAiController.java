package com.chronic.modules.miniapp.controller;

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
 * 小程序 AI 健康助手（与医生端共用同一套 AI 服务与患者数据）
 *
 * @author chronic
 */
@Tag(name = "35-小程序-AI健康助手", description = "AI 问答 / 历史会话")
@RestController
@RequestMapping("/mp/ai")
@RequiredArgsConstructor
public class MpAiController {

    private final AiService aiService;

    @Operation(summary = "AI 对话")
    @PostMapping("/chat")
    public Result<AiChatVO> chat(@Valid @RequestBody AiChatDTO dto) {
        return Result.success(aiService.mpChat(dto));
    }

    @Operation(summary = "历史会话")
    @GetMapping("/history")
    public Result<List<CdAiChat>> history(@RequestParam(required = false) Integer limit) {
        return Result.success(aiService.myHistory(limit));
    }
}