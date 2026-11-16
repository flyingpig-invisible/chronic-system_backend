package com.chronic.modules.ai.service;

import com.chronic.modules.ai.dto.AiChatDTO;
import com.chronic.modules.ai.vo.AiChatVO;
import com.chronic.modules.chronic.entity.CdAiChat;

import java.util.List;

/**
 * AI 健康助手服务
 *
 * @author chronic
 */
public interface AiService {

    /** 医生端对话（可携带患者上下文） */
    AiChatVO doctorChat(AiChatDTO dto);

    /** 小程序端对话（自动注入当前患者上下文） */
    AiChatVO mpChat(AiChatDTO dto);

    /** 医生端：基于患者专属档案 + 健康数据生成 AI 分析 */
    AiChatVO analyzeProfile(Long patientId);

    /** 家属端：基于患者专属档案生成 AI 分析（校验绑定） */
    AiChatVO analyzeProfileForFamily(Long patientId);

    /** 我的历史会话 */
    List<CdAiChat> myHistory(Integer limit);
}
