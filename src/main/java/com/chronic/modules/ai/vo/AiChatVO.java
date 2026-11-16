package com.chronic.modules.ai.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * AI 对话响应
 *
 * @author chronic
 */
@Data
@Schema(description = "AI对话响应")
public class AiChatVO {

    @Schema(description = "会话ID")
    private Long chatId;

    @Schema(description = "AI回答内容")
    private String answer;

    @Schema(description = "关联患者ID")
    private Long patientId;

    @Schema(description = "关联患者姓名")
    private String patientName;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "耗时(毫秒)")
    private Long costTime;

    @Schema(description = "本次回答引用的知识库条目数")
    private Integer kbRefs;
}
