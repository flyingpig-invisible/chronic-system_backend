package com.chronic.modules.kb.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.kb.entity.KbDocument;
import com.chronic.modules.kb.query.KbDocumentQuery;

import java.util.Map;

/**
 * 知识库文档服务
 *
 * @author chronic
 */
public interface KbDocumentService {

    PageResult<KbDocument> pageDocument(KbDocumentQuery query);

    KbDocument getDocument(Long docId);

    Long saveDocument(KbDocument doc);

    void deleteDocument(Long[] docIds);

    /** 首页统计：按类型/病种分组数量 */
    Map<String, Object> stats();
}
