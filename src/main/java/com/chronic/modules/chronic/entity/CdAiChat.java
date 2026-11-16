package com.chronic.modules.chronic.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * AI 健康助手会话记录
 *
 * @author chronic
 */
@Data
@TableName("cd_ai_chat")
@Schema(description = "AI健康助手会话记录")
public class CdAiChat implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long chatId;

    @Schema(description = "发起人ID")
    private Long userId;

    @Schema(description = "用户类型：1系统用户 2小程序用户")
    private String userType;

    @Schema(description = "角色：doctor/patient/family")
    private String role;

    @Schema(description = "关联患者ID")
    private Long patientId;

    @Schema(description = "关联患者姓名")
    private String patientName;

    @Schema(description = "用户提问")
    private String question;

    @Schema(description = "AI回答")
    private String answer;

    @Schema(description = "模型名称")
    private String model;

    @Schema(description = "输入tokens")
    private Integer promptTokens;

    @Schema(description = "输出tokens")
    private Integer completionTokens;

    @Schema(description = "耗时(毫秒)")
    private Long costTime;

    @Schema(description = "本次回答引用的知识库条目数")
    private Integer kbRefs;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
