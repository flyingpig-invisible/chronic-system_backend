package com.chronic.modules.kb.service;

import com.chronic.modules.kb.vo.KbSnippetVO;

import java.util.List;

/**
 * 知识库检索服务：为 AI 健康助手提供可溯源的知识片段
 *
 * <p>检索策略（按优先级）：病种匹配 + 关键词命中；标准来源库优先于辅助解释库；
 * 同类型按关键词命中数与浏览/更新时间排序。</p>
 *
 * @author chronic
 */
public interface KbSearchService {

    /**
     * 按病种与问题检索知识片段
     *
     * @param diseaseCodes 患者病种编码集合（如 ["1","2"]），可为空
     * @param question     用户提问（用于关键词提取）
     * @param topK         返回条数上限
     * @return 已排序的知识片段（标准来源库在前）
     */
    List<KbSnippetVO> search(List<String> diseaseCodes, String question, int topK);

    /**
     * 构建注入 AI 的知识上下文文本（含出处标注与引用规则）
     *
     * @return 可直接拼入 System Prompt 的文本；无命中时返回空串
     */
    String buildKnowledgeContext(List<String> diseaseCodes, String question);

    /** 统计：命中条数（用于前端展示"引用了几条知识"） */
    int countHits(List<String> diseaseCodes, String question);
}
