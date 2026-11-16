package com.chronic.modules.chronic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.chronic.entity.CdAiChat;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话记录 Mapper
 *
 * @author chronic
 */
@Mapper
public interface CdAiChatMapper extends BaseMapper<CdAiChat> {
}
