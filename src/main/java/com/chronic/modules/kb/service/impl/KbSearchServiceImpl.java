package com.chronic.modules.kb.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.modules.kb.entity.KbDocument;
import com.chronic.modules.kb.mapper.KbDocumentMapper;
import com.chronic.modules.kb.service.KbSearchService;
import com.chronic.modules.kb.vo.KbSnippetVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 知识库检索服务实现
 *
 * <p>检索流程：取该患者病种范围（含通用）的已发布文档 → 按关键词命中数打分 →
 * 标准来源库优先 → 取 TopK → 生成带出处的上下文文本。</p>
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KbSearchServiceImpl implements KbSearchService {

    private final KbDocumentMapper documentMapper;

    /** 片段最大长度（避免 Prompt 过长） */
    private static final int MAX_SNIPPET_LEN = 320;
    /** 默认返回条数 */
    private static final int DEFAULT_TOPK = 3;

    /** 医疗领域关键词表：用于从提问中提取检索词 */
    private static final String[] MEDICAL_KEYWORDS = {
            "血压", "高血压", "收缩压", "舒张压", "降压", "降压药", "头晕", "头痛",
            "血糖", "糖尿病", "空腹", "餐后", "糖化", "胰岛素", "低血糖", "酮症",
            "胸痛", "心绞痛", "心梗", "心肌", "胸闷", "心悸", "硝酸甘油", "冠心病",
            "慢阻肺", "呼吸困难", "咳痰", "咳嗽", "气促", "喘", "肺功能", "氧疗", "吸氧",
            "饮食", "吃饭", "盐", "钠", "运动", "锻炼", "抽烟", "吸烟", "戒烟", "酒",
            "用药", "吃药", "停药", "剂量", "副作用", "依从性", "打卡", "漏服",
            "复查", "复诊", "就诊", "就医", "急诊", "急救", "120", "疫苗",
            "体重", "肥胖", "血脂", "胆固醇", "尿酸", "肾", "蛋白",
            "头晕", "恶心", "呕吐", "乏力", "发绀", "昏迷", "意识",
            "睡觉", "失眠", "情绪", "焦虑", "冬天", "保暖", "感染", "感冒"
    };

    @Override
    public List<KbSnippetVO> search(List<String> diseaseCodes, String question, int topK) {
        if (topK <= 0) {
            topK = DEFAULT_TOPK;
        }
        // 1. 取病种范围内的已发布文档（含通用病种 0）
        List<String> scope = new ArrayList<>();
        scope.add("0");
        if (diseaseCodes != null) {
            for (String d : diseaseCodes) {
                if (StringUtils.hasText(d) && !scope.contains(d)) {
                    scope.add(d);
                }
            }
        }
        LambdaQueryWrapper<KbDocument> w = Wrappers.<KbDocument>lambdaQuery()
                .eq(KbDocument::getStatus, "1")
                .in(KbDocument::getDiseaseCode, scope);
        List<KbDocument> docs = documentMapper.selectList(w);
        if (docs.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 提取提问关键词
        Set<String> terms = extractTerms(question);

        // 3. 打分
        List<KbSnippetVO> scored = new ArrayList<>();
        for (KbDocument d : docs) {
            int score = score(d, terms);
            // 无关（无任何关键词命中且提问有明确关键词）则跳过
            if (!terms.isEmpty() && score <= 0) {
                continue;
            }
            scored.add(toSnippet(d, score));
        }

        // 若提问未命中任何内容，降级返回该病种下的标准库条目（保证有依据可引）
        if (scored.isEmpty()) {
            for (KbDocument d : docs) {
                if ("1".equals(d.getKbType())) {
                    scored.add(toSnippet(d, 0));
                }
            }
        }

        // 4. 排序：标准来源库优先 → 得分高者优先 → 浏览/更新时间新者优先
        scored.sort((a, b) -> {
            boolean sa = "1".equals(a.getKbType());
            boolean sb = "1".equals(b.getKbType());
            if (sa != sb) {
                return sa ? -1 : 1;
            }
            int cmp = Integer.compare(
                    b.getScore() == null ? 0 : b.getScore(),
                    a.getScore() == null ? 0 : a.getScore());
            return cmp;
        });

        return scored.size() > topK ? new ArrayList<>(scored.subList(0, topK)) : scored;
    }

    @Override
    public String buildKnowledgeContext(List<String> diseaseCodes, String question) {
        List<KbSnippetVO> hits = search(diseaseCodes, question, DEFAULT_TOPK);
        if (hits.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n【知识库依据】\n");
        sb.append("以下为系统知识库中与本次提问相关的内容，请优先依据这些内容回答，")
          .append("并在引用时注明出处（格式：【来源：发布单位 版本 原文位置】）。\n");
        int idx = 1;
        for (KbSnippetVO h : hits) {
            boolean standard = "1".equals(h.getKbType());
            sb.append("\n[").append(idx++).append("] ")
              .append(standard ? "【标准来源库】" : "【辅助解释库·仅供参考，非权威结论】")
              .append(h.getTitle()).append("\n");
            sb.append("内容：").append(h.getContent()).append("\n");
            sb.append("出处：").append(
                    StringUtils.hasText(h.getPublisher()) ? h.getPublisher() : "系统知识库");
            if (StringUtils.hasText(h.getVersion())) {
                sb.append(" ").append(h.getVersion());
            }
            if (StringUtils.hasText(h.getSourceLocation())) {
                sb.append(" ").append(h.getSourceLocation());
            }
            sb.append("\n");
        }
        sb.append("\n引用要求：\n");
        sb.append("1. 上述【标准来源库】内容可直接作为依据引用，并标注出处。\n");
        sb.append("2. 【辅助解释库】内容仅作通俗解释，必须注明“仅供参考，非权威结论”。\n");
        sb.append("3. 若知识库内容不足以回答该问题，必须明确说明“知识库暂无相关依据，请咨询医生”，")
          .append("严禁凭猜测编造医学结论、用药剂量或诊断意见。\n");
        return sb.toString();
    }

    @Override
    public int countHits(List<String> diseaseCodes, String question) {
        return search(diseaseCodes, question, DEFAULT_TOPK).size();
    }

    /** 从提问中提取检索词 */
    private Set<String> extractTerms(String question) {
        Set<String> terms = new LinkedHashSet<>();
        if (!StringUtils.hasText(question)) {
            return terms;
        }
        for (String kw : MEDICAL_KEYWORDS) {
            if (question.contains(kw)) {
                terms.add(kw);
            }
        }
        return terms;
    }

    /** 计算文档与检索词的匹配得分 */
    private int score(KbDocument d, Set<String> terms) {
        if (terms.isEmpty()) {
            return 0;
        }
        String title = nvl(d.getTitle());
        String keywords = nvl(d.getKeywords());
        String tags = nvl(d.getTags());
        String summary = nvl(d.getSummary());
        String content = nvl(d.getContent());
        int score = 0;
        for (String t : terms) {
            if (title.contains(t)) {
                score += 10;
            }
            if (keywords.contains(t)) {
                score += 6;
            }
            if (tags.contains(t)) {
                score += 4;
            }
            if (summary.contains(t)) {
                score += 3;
            }
            if (content.contains(t)) {
                score += 1;
            }
        }
        return score;
    }

    private KbSnippetVO toSnippet(KbDocument d, int score) {
        KbSnippetVO vo = new KbSnippetVO();
        vo.setDocId(d.getDocId());
        vo.setTitle(d.getTitle());
        vo.setKbType(d.getKbType());
        vo.setDiseaseCode(d.getDiseaseCode());
        vo.setSourceLocation(d.getSourceLocation());
        vo.setPublisher(d.getPublisher());
        vo.setVersion(d.getVersion());
        vo.setScore(score);
        String content = nvl(d.getContent());
        vo.setContent(content.length() > MAX_SNIPPET_LEN
                ? content.substring(0, MAX_SNIPPET_LEN) + "…" : content);
        return vo;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}
