package com.chronic.modules.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.kb.entity.KbDocument;
import com.chronic.modules.kb.mapper.KbDocumentMapper;
import com.chronic.modules.kb.query.KbDocumentQuery;
import com.chronic.modules.kb.service.KbDocumentService;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库文档服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbDocumentServiceImpl implements KbDocumentService {

    private final KbDocumentMapper documentMapper;

    @Override
    public PageResult<KbDocument> pageDocument(KbDocumentQuery query) {
        LambdaQueryWrapper<KbDocument> w = Wrappers.<KbDocument>lambdaQuery()
                .and(StringUtils.hasText(query.getKeyword()), q -> q
                        .like(KbDocument::getTitle, query.getKeyword())
                        .or().like(KbDocument::getSummary, query.getKeyword())
                        .or().like(KbDocument::getTags, query.getKeyword()))
                .eq(StringUtils.hasText(query.getKbType()), KbDocument::getKbType, query.getKbType())
                .eq(StringUtils.hasText(query.getDiseaseCode()), KbDocument::getDiseaseCode, query.getDiseaseCode())
                .eq(StringUtils.hasText(query.getCategory()), KbDocument::getCategory, query.getCategory())
                .eq(StringUtils.hasText(query.getStatus()), KbDocument::getStatus, query.getStatus())
                .orderByDesc(KbDocument::getUpdateTime)
                .orderByDesc(KbDocument::getCreateTime);
        Page<KbDocument> page = documentMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page);
    }

    @Override
    public KbDocument getDocument(Long docId) {
        KbDocument doc = documentMapper.selectById(docId);
        if (doc == null) {
            throw new ServiceException("知识文档不存在或已删除");
        }
        // 浏览计数（不阻塞主流程）
        try {
            KbDocument up = new KbDocument();
            up.setDocId(docId);
            up.setViewCount((doc.getViewCount() == null ? 0 : doc.getViewCount()) + 1);
            documentMapper.updateById(up);
        } catch (Exception e) {
            log.warn("知识文档浏览计数失败: {}", e.getMessage());
        }
        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDocument(KbDocument doc) {
        if (!StringUtils.hasText(doc.getTitle())) {
            throw new ServiceException("标题不能为空");
        }
        if (!StringUtils.hasText(doc.getContent())) {
            throw new ServiceException("正文内容不能为空");
        }
        String username = SecurityUtils.getUsername();
        if (doc.getDocId() == null) {
            doc.setStatus(StringUtils.hasText(doc.getStatus()) ? doc.getStatus() : "1");
            doc.setViewCount(0);
            doc.setCreateBy(username);
            doc.setUpdateBy(username);
            documentMapper.insert(doc);
        } else {
            KbDocument exists = documentMapper.selectById(doc.getDocId());
            if (exists == null) {
                throw new ServiceException("知识文档不存在或已删除");
            }
            doc.setUpdateBy(username);
            doc.setUpdateTime(LocalDateTime.now());
            documentMapper.updateById(doc);
        }
        return doc.getDocId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long[] docIds) {
        if (docIds == null || docIds.length == 0) {
            throw new ServiceException("请选择要删除的文档");
        }
        for (Long id : docIds) {
            documentMapper.deleteById(id);
        }
    }

    @Override
    public Map<String, Object> stats() {
        List<KbDocument> all = documentMapper.selectList(Wrappers.<KbDocument>lambdaQuery());
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", all.size());

        Map<String, Integer> byType = new LinkedHashMap<>();
        Map<String, Integer> byDisease = new LinkedHashMap<>();
        List<String> categories = new ArrayList<>();
        int published = 0;
        for (KbDocument d : all) {
            String t = d.getKbType() == null ? "0" : d.getKbType();
            byType.put(t, byType.getOrDefault(t, 0) + 1);
            String dis = d.getDiseaseCode() == null ? "0" : d.getDiseaseCode();
            byDisease.put(dis, byDisease.getOrDefault(dis, 0) + 1);
            if (StringUtils.hasText(d.getCategory()) && !categories.contains(d.getCategory())) {
                categories.add(d.getCategory());
            }
            if ("1".equals(d.getStatus())) {
                published++;
            }
        }
        result.put("byType", byType);
        result.put("byDisease", byDisease);
        result.put("categories", categories);
        result.put("published", published);
        result.put("draft", all.size() - published);
        return result;
    }
}
