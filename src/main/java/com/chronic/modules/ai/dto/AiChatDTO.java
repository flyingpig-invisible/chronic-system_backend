package com.chronic.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 对话请求
 *
 * @author chronic
 */
@Data
@Schema(description = "AI对话请求")
public class AiChatDTO {

    @NotBlank(message = "提问内容不能为空")
    @Schema(description = "用户提问", requiredMode = Schema.RequiredMode.REQUIRED)
    private String question;

    @Schema(description = "关联患者ID（医生端可选，小程序端自动注入）")
    private Long patientId;

    @Schema(description = "历史会话（JSON数组，可选，用于多轮对话）")
    private String history;
}
