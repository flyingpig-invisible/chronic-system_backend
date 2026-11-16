package com.chronic.modules.kb.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.kb.entity.KbDocument;
import org.apache.ibatis.annotations.Mapper;

/**
 * 知识库文档 Mapper
 *
 * @author chronic
 */
@Mapper
public interface KbDocumentMapper extends BaseMapper<KbDocument> {
}
