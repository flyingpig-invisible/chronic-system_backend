package com.chronic.modules.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chronic.common.exception.ServiceException;
import com.chronic.config.AiProperties;
import com.chronic.modules.ai.client.DeepSeekClient;
import com.chronic.modules.ai.dto.AiChatDTO;
import com.chronic.modules.ai.service.AiService;
import com.chronic.modules.ai.vo.AiChatVO;
import com.chronic.modules.kb.service.KbSearchService;
import com.chronic.modules.chronic.service.CdPatientProfileService;
import com.chronic.modules.chronic.entity.*;
import com.chronic.modules.chronic.mapper.*;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AI \u5065\u5eb7\u52a9\u624b\u670d\u52a1\u5b9e\u73b0
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final DeepSeekClient deepSeekClient;
    private final AiProperties aiProperties;
    private final CdAiChatMapper aiChatMapper;
    private final CdPatientMapper patientMapper;
    private final CdMeasureMapper measureMapper;
    private final CdWarningMapper warningMapper;
    private final CdPlanMapper planMapper;
    private final CdRemindMapper remindMapper;
    private final CdGuideMapper guideMapper;
    private final CdHealthScoreMapper scoreMapper;
    private final CdPatientDiseaseMapper diseaseMapper;
    private final MpFamilyPatientMapper familyPatientMapper;
    private final KbSearchService kbSearchService;
    private final CdPatientProfileService patientProfileService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /** \u6307\u6807\u4e2d\u6587\u540d */
    private static final Map<String, String> INDICATOR_CN = Map.of(
            "sbp", "\u6536\u7f29\u538b", "dbp", "\u8212\u5f20\u538b", "hr", "\u5fc3\u7387",
            "spo2", "\u8840\u6c27\u9971\u548c\u5ea6", "glu", "\u8840\u7cd6", "rr", "\u547c\u5438\u9891\u7387");

    /** \u75c5\u79cd\u4e2d\u6587\u540d */
    private static final Map<String, String> DISEASE_CN = Map.of(
            "1", "\u9ad8\u8840\u538b", "2", "2\u578b\u7cd6\u5c3f\u75c5", "3", "\u51a0\u5fc3\u75c5", "4", "\u6162\u963b\u80ba");

    @Override
    public AiChatVO doctorChat(AiChatDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        String systemPrompt = "\u4f60\u662f\u4e00\u540d\u4e13\u4e1a\u7684\u6162\u75c5\u7ba1\u7406\u533b\u751f\u52a9\u624b\uff0c\u670d\u52a1\u4e8e\u533b\u751f\u7528\u6237\u3002"
                + "\u8bf7\u57fa\u4e8e\u63d0\u4f9b\u7684\u60a3\u8005\u6570\u636e\uff0c\u7ed9\u51fa\u4e13\u4e1a\u3001\u7b80\u6d01\u3001\u53ef\u64cd\u4f5c\u7684\u4e34\u5e8a\u5efa\u8bae\u3002"
                + "\u56de\u7b54\u8981\u7ed3\u6784\u6e05\u6670\uff0c\u5fc5\u8981\u65f6\u5206\u70b9\u8bf4\u660e\uff1b\u4e0d\u786e\u5b9a\u7684\u4fe1\u606f\u4e0d\u8981\u7f16\u9020\u3002"
                + "\u5982\u6d89\u53ca\u7528\u836f\uff0c\u8bf7\u63d0\u9192\u9075\u533b\u5631\u3002";

        PatientContext ctx = buildContext(dto.getPatientId());
        String question = dto.getQuestion();
        // 注入知识库依据（按患者病种 + 提问检索，标准来源库优先）
        String kbContext = kbSearchService.buildKnowledgeContext(ctx.diseaseCodes, question);
        return doChat(user, "doctor", dto.getPatientId(), ctx, systemPrompt + kbContext, question);
    }

    @Override
    public AiChatVO mpChat(AiChatDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long patientId = resolveMpPatientId(user, dto.getPatientId());

        String roleName = "1".equals(user.getMpRole()) ? "\u60a3\u8005\u672c\u4eba" : "\u60a3\u8005\u5bb6\u5c5e";
        String systemPrompt = "\u4f60\u662f\u4e00\u540d\u6162\u75c5\u5065\u5eb7\u7ba1\u7406\u52a9\u624b\uff0c\u6b63\u5728\u4e3a" + roleName + "\u63d0\u4f9b\u5065\u5eb7\u54a8\u8be2\u670d\u52a1\u3002"
                + "\u8bf7\u7528\u901a\u4fd7\u6613\u61c2\u7684\u8bed\u8a00\u56de\u7b54\uff0c\u907f\u514d\u8fc7\u591a\u533b\u5b66\u672f\u8bed\uff1b"
                + "\u7ed9\u51fa\u5177\u4f53\u53ef\u6267\u884c\u7684\u5efa\u8bae\uff08\u996e\u98df\u3001\u8fd0\u52a8\u3001\u7528\u836f\u3001\u590d\u8bca\u7b49\uff09\uff1b"
                + "\u82e5\u60a3\u8005\u51fa\u73b0\u4e25\u91cd\u5f02\u5e38\u6307\u6807\u6216\u9ad8\u5371\u9884\u8b66\uff0c\u5fc5\u987b\u660e\u786e\u63d0\u9192\u53ca\u65f6\u5c31\u533b\uff0c\u5fc5\u8981\u65f6\u62e8\u6253 120\u3002"
                + "\u4f60\u4e0d\u80fd\u63d0\u4f9b\u8bca\u65ad\u7ed3\u8bba\uff0c\u4e0d\u80fd\u5f00\u5904\u65b9\uff0c\u7528\u836f\u8bf7\u9075\u533b\u5631\u3002";

        PatientContext ctx = buildContext(patientId);
        String question = dto.getQuestion();
        // 注入知识库依据（患者/家属端同样引用，确保回答有医学出处）
        String kbContext = kbSearchService.buildKnowledgeContext(ctx.diseaseCodes, question);
        return doChat(user, "1".equals(user.getMpRole()) ? "patient" : "family",
                patientId, ctx, systemPrompt + kbContext, question);
    }

    @Override
    public AiChatVO analyzeProfile(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (patientId == null) {
            throw new ServiceException("\u8bf7\u5148\u9009\u62e9\u60a3\u8005");
        }
        String systemPrompt = "\u4f60\u662f\u4e00\u540d\u4e13\u4e1a\u7684\u6162\u75c5\u7ba1\u7406\u533b\u751f\u52a9\u624b\u3002\u8bf7\u57fa\u4e8e\u63d0\u4f9b\u7684\u60a3\u8005\u4e13\u5c5e\u6863\u6848\u4e0e\u5065\u5eb7\u6570\u636e\uff0c"
                + "\u8f93\u51fa\u7ed3\u6784\u5316\u7684\u98ce\u9669\u5206\u6790\u4e0e\u5e72\u9884\u5efa\u8bae\uff0c\u5305\u542b\uff1a\u2460\u5173\u952e\u98ce\u9669\u70b9\uff08\u8fc7\u654f\u3001\u7528\u836f\u3001\u5fc3\u7406\u3001\u751f\u6d3b\u65b9\u5f0f\u7b49\uff09\uff1b"
                + "\u2461\u9700\u91cd\u70b9\u5173\u6ce8\u7684\u6307\u6807\uff1b\u2462\u4e2a\u4f53\u5316\u5e72\u9884\u5efa\u8bae\uff08\u996e\u98df\u3001\u8fd0\u52a8\u3001\u7528\u836f\u3001\u590d\u8bca\uff09\uff1b\u2463\u9700\u8865\u5145\u7684\u6863\u6848\u4fe1\u606f\u3002"
                + "\u6863\u6848\u7f3a\u5931\u7684\u5b57\u6bb5\u8bf7\u660e\u786e\u6307\u51fa\uff0c\u4e0d\u8981\u7f16\u9020\uff1b\u7528\u836f\u8bf7\u9075\u533b\u5631\u3002";
        PatientContext ctx = buildContext(patientId);
        if (ctx.patientId == null) {
            throw new ServiceException("\u60a3\u8005\u4e0d\u5b58\u5728");
        }
        String question = "\u8bf7\u5bf9\u8be5\u60a3\u8005\u7684\u4e13\u5c5e\u6863\u6848\u8fdb\u884c\u5206\u6790\uff0c\u7ed9\u51fa\u98ce\u9669\u63d0\u793a\u4e0e\u5e72\u9884\u5efa\u8bae\u3002";
        return doChat(user, "doctor", patientId, ctx, systemPrompt, question);
    }

    @Override
    public AiChatVO analyzeProfileForFamily(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long pid = resolveMpPatientId(user, patientId);
        String systemPrompt = "\u4f60\u662f\u4e00\u540d\u6162\u75c5\u5065\u5eb7\u7ba1\u7406\u52a9\u624b\uff0c\u6b63\u5728\u4e3a\u60a3\u8005\u5bb6\u5c5e\u5206\u6790\u60a3\u8005\u7684\u4e13\u5c5e\u6863\u6848\u3002"
                + "\u8bf7\u7528\u901a\u4fd7\u6613\u61c2\u7684\u8bed\u8a00\uff0c\u603b\u7ed3\u5bb6\u5c5e\u9700\u8981\u5173\u6ce8\u7684\u91cd\u70b9\uff08\u996e\u98df\u3001\u4f5c\u606f\u3001\u60c5\u7eea\u3001\u5728\u5bb6\u89c2\u5bdf\u91cd\u70b9\uff09\uff0c"
                + "\u5e76\u63d0\u9192\u54ea\u4e9b\u60c5\u51b5\u9700\u53ca\u65f6\u5c31\u533b\u3002\u4e0d\u63d0\u4f9b\u8bca\u65ad\u7ed3\u8bba\u4e0e\u5904\u65b9\uff0c\u7528\u836f\u8bf7\u9075\u533b\u5631\u3002";
        PatientContext ctx = buildContext(pid);
        String question = "\u8bf7\u5bf9\u8be5\u60a3\u8005\u7684\u4e13\u5c5e\u6863\u6848\u8fdb\u884c\u5206\u6790\uff0c\u544a\u8bc9\u5bb6\u5c5e\u65e5\u5e38\u9700\u8981\u6ce8\u610f\u4ec0\u4e48\u3002";
        return doChat(user, "family", pid, ctx, systemPrompt, question);
    }

    @Override
    public List<CdAiChat> myHistory(Integer limit) {
        LoginUser user = SecurityUtils.getLoginUser();
        String userType = "mp".equals(user.getUserType()) ? "2" : "1";
        int size = (limit == null || limit <= 0) ? 20 : Math.min(limit, 100);
        return aiChatMapper.selectList(new LambdaQueryWrapper<CdAiChat>()
                .eq(CdAiChat::getUserId, user.getUserId())
                .eq(CdAiChat::getUserType, userType)
                .orderByDesc(CdAiChat::getChatId)
                .last("limit " + size));
    }

    /** \u6821\u9a8c\u5e76\u89e3\u6790\u5c0f\u7a0b\u5e8f\u7aef\u7684\u60a3\u8005ID */
    private Long resolveMpPatientId(LoginUser user, Long requestPatientId) {
        if ("1".equals(user.getMpRole())) {
            CdPatient self = patientMapper.selectOne(new LambdaQueryWrapper<CdPatient>()
                    .eq(CdPatient::getMpUserId, user.getUserId()).last("limit 1"));
            if (self == null) {
                throw new ServiceException("\u672a\u5339\u914d\u5230\u60a3\u8005\u6863\u6848");
            }
            // \u60a3\u8005\u53ea\u80fd\u8bbf\u95ee\u672c\u4eba\u6570\u636e
            if (requestPatientId != null && !requestPatientId.equals(self.getPatientId())) {
                throw new ServiceException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u60a3\u8005\u6570\u636e");
            }
            return self.getPatientId();
        }
        Long patientId = requestPatientId;
        if (patientId == null) {
            List<MpFamilyPatient> rels = familyPatientMapper.selectList(new LambdaQueryWrapper<MpFamilyPatient>()
                    .eq(MpFamilyPatient::getFamilyUserId, user.getUserId()));
            if (rels.isEmpty()) {
                throw new ServiceException("\u672a\u7ed1\u5b9a\u60a3\u8005");
            }
            patientId = rels.get(0).getPatientId();
        } else {
            Long count = familyPatientMapper.selectCount(new LambdaQueryWrapper<MpFamilyPatient>()
                    .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                    .eq(MpFamilyPatient::getPatientId, patientId));
            if (count == null || count == 0) {
                throw new ServiceException(403, "\u65e0\u6743\u67e5\u770b\u8be5\u60a3\u8005\u6570\u636e");
            }
        }
        return patientId;
    }

    /** \u6267\u884c\u5bf9\u8bdd\u5e76\u8bb0\u5f55 */
    private AiChatVO doChat(LoginUser user, String role, Long patientId,
                            PatientContext ctx, String systemPrompt, String question) {
        // 知识库引用条数（用于前端展示“本次回答依据了几条知识”）
        int kbHits = kbSearchService.countHits(ctx.diseaseCodes, question);
        String fullSystem = systemPrompt + "\n\n" + ctx.toPrompt();
        DeepSeekClient.ChatResult result = deepSeekClient.chat(fullSystem, question);

        CdAiChat record = new CdAiChat();
        record.setUserId(user.getUserId());
        record.setUserType("mp".equals(user.getUserType()) ? "2" : "1");
        record.setRole(role);
        record.setPatientId(ctx.patientId);
        record.setPatientName(ctx.patientName);
        record.setQuestion(question);
        record.setAnswer(result.getAnswer());
        record.setModel(aiProperties.getModel());
        record.setPromptTokens(result.getPromptTokens());
        record.setCompletionTokens(result.getCompletionTokens());
        record.setCostTime(result.getCostTime());
        record.setKbRefs(kbHits);
        record.setCreateTime(LocalDateTime.now());
        aiChatMapper.insert(record);

        AiChatVO vo = new AiChatVO();
        vo.setChatId(record.getChatId());
        vo.setAnswer(result.getAnswer());
        vo.setPatientId(ctx.patientId);
        vo.setPatientName(ctx.patientName);
        vo.setModel(aiProperties.getModel());
        vo.setCostTime(result.getCostTime());
        vo.setKbRefs(kbHits);
        return vo;
    }

    /** \u6784\u5efa\u60a3\u8005\u5065\u5eb7\u4e0a\u4e0b\u6587 */
    private PatientContext buildContext(Long patientId) {
        PatientContext ctx = new PatientContext();
        if (patientId == null) {
            ctx.patientId = null;
            ctx.patientName = null;
            ctx.detail = "\u5f53\u524d\u65e0\u7279\u5b9a\u60a3\u8005\u4e0a\u4e0b\u6587\u3002";
            return ctx;
        }
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            ctx.patientId = null;
            ctx.detail = "\u5f53\u524d\u65e0\u7279\u5b9a\u60a3\u8005\u4e0a\u4e0b\u6587\u3002";
            return ctx;
        }
        ctx.patientId = patientId;
        ctx.patientName = patient.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("\u3010\u60a3\u8005\u57fa\u672c\u4fe1\u606f\u3011\n");
        sb.append("\u59d3\u540d\uff1a").append(patient.getName());
        sb.append("\uff0c\u6027\u522b\uff1a").append("1".equals(patient.getGender()) ? "\u7537" : "\u5973");
        if (patient.getBirthday() != null) {
            sb.append("\uff0c\u51fa\u751f\u65e5\u671f\uff1a").append(patient.getBirthday());
        }
        sb.append("\uff0c\u98ce\u9669\u7b49\u7ea7\uff1a").append(riskCn(patient.getRiskLevel()));
        sb.append("\uff0c\u98ce\u9669\u8bc4\u5206\uff1a").append(patient.getRiskScore() == null ? "-" : patient.getRiskScore());
        sb.append("\uff0c\u5f85\u5904\u7406\u9884\u8b66\u6570\uff1a").append(patient.getPendingWarningCount() == null ? 0 : patient.getPendingWarningCount());
        sb.append("\n");

        // \u75c5\u79cd
        List<CdPatientDisease> diseases = diseaseMapper.selectList(new LambdaQueryWrapper<CdPatientDisease>()
                .eq(CdPatientDisease::getPatientId, patientId));
        for (CdPatientDisease d : diseases) {
            if (d.getDiseaseCode() != null) {
                ctx.diseaseCodes.add(d.getDiseaseCode());
            }
        }
        if (!diseases.isEmpty()) {
            sb.append("\n\u3010\u60a3\u8005\u75c5\u79cd\u3011\n");
            for (CdPatientDisease d : diseases) {
                sb.append("- ").append(d.getDiseaseName() == null ? DISEASE_CN.getOrDefault(d.getDiseaseCode(), d.getDiseaseCode()) : d.getDiseaseName());
                if (d.getDiagnoseDate() != null) {
                    sb.append("\uff08\u8bca\u65ad\u65e5\u671f\uff1a").append(d.getDiagnoseDate()).append("\uff09");
                }
                sb.append("\n");
            }
        }

        // \u6700\u8fd1\u76d1\u6d4b
        List<CdMeasure> measures = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit 12"));
        if (!measures.isEmpty()) {
            sb.append("\n\u3010\u6700\u8fd1\u76d1\u6d4b\u6570\u636e\u3011\n");
            for (CdMeasure m : measures) {
                sb.append("- ").append(m.getMeasureTime() == null ? "" : m.getMeasureTime().format(DTF)).append(" ");
                sb.append(m.getIndicatorName() == null ? INDICATOR_CN.getOrDefault(m.getIndicatorCode(), m.getIndicatorCode()) : m.getIndicatorName());
                sb.append("\uff1a").append(m.getValue()).append(m.getUnit() == null ? "" : m.getUnit());
                sb.append("1".equals(m.getAbnormalFlag()) ? "\uff08\u5f02\u5e38\uff09" : "");
                sb.append("\n");
            }
        }

        // \u9884\u8b66
        List<CdWarning> warnings = warningMapper.selectList(new LambdaQueryWrapper<CdWarning>()
                .eq(CdWarning::getPatientId, patientId)
                .orderByDesc(CdWarning::getTriggerTime)
                .last("limit 8"));
        if (!warnings.isEmpty()) {
            sb.append("\n\u3010\u6700\u8fd1\u9884\u8b66\u3011\n");
            for (CdWarning w : warnings) {
                sb.append("- ").append(w.getTriggerTime() == null ? "" : w.getTriggerTime().format(DTF)).append(" ");
                sb.append(levelCn(w.getWarningLevel())).append("\u9884\u8b66\uff1a");
                sb.append(w.getIndicatorName()).append(" ").append(w.getIndicatorValue()).append(w.getUnit() == null ? "" : w.getUnit());
                sb.append("\uff0c\u9608\u503c\uff1a").append(w.getThresholdDesc() == null ? "-" : w.getThresholdDesc());
                sb.append("\uff0c\u72b6\u6001\uff1a").append("2".equals(w.getHandleStatus()) ? "\u5df2\u5904\u7406" : "\u672a\u5904\u7406");
                sb.append("\n");
            }
        }

        // \u7528\u836f\u63d0\u9192
        List<CdRemind> reminds = remindMapper.selectList(new LambdaQueryWrapper<CdRemind>()
                .eq(CdRemind::getPatientId, patientId)
                .last("limit 10"));
        if (!reminds.isEmpty()) {
            sb.append("\n\u3010\u7528\u836f\u65b9\u6848\u3011\n");
            for (CdRemind r : reminds) {
                sb.append("- ").append(r.getMedicineName());
                sb.append("\uff0c\u5242\u91cf\uff1a").append(r.getDosage() == null ? "-" : r.getDosage());
                sb.append("\uff0c\u9891\u6b21\uff1a").append(freqCn(r.getFrequency()));
                sb.append("\uff0c\u7528\u836f\u65f6\u95f4\uff1a").append(r.getRemindTimes() == null ? "-" : r.getRemindTimes());
                sb.append("\n");
            }
        }

        // \u533b\u751f\u6307\u5bfc
        List<CdGuide> guides = guideMapper.selectList(new LambdaQueryWrapper<CdGuide>()
                .eq(CdGuide::getPatientId, patientId)
                .orderByDesc(CdGuide::getGuideTime)
                .last("limit 6"));
        if (!guides.isEmpty()) {
            sb.append("\n\u3010\u533b\u751f\u6700\u65b0\u6307\u5bfc\u3011\n");
            for (CdGuide g : guides) {
                sb.append("- ").append(g.getGuideTime() == null ? "" : g.getGuideTime().format(DTF)).append(" ");
                sb.append("[").append(guideTypeCn(g.getGuideType())).append("] ");
                sb.append(g.getContent()).append("\n");
            }
        }

        // \u5065\u5eb7\u8ba1\u5212
        List<CdPlan> plans = planMapper.selectList(new LambdaQueryWrapper<CdPlan>()
                .eq(CdPlan::getPatientId, patientId)
                .orderByDesc(CdPlan::getCreateTime)
                .last("limit 3"));
        if (!plans.isEmpty()) {
            sb.append("\n\u3010\u5065\u5eb7\u7ba1\u7406\u8ba1\u5212\u3011\n");
            for (CdPlan p : plans) {
                sb.append("- ").append(p.getPlanName());
                sb.append("\uff0c\u76ee\u6807\uff1a").append(p.getTargetDesc() == null ? "-" : p.getTargetDesc());
                sb.append("\uff0c\u6267\u884c\u7387\uff1a").append(p.getExecuteRate() == null ? 0 : p.getExecuteRate()).append("%\n");
            }
        }

        // 患者专属档案（医生与家属录入，供 AI 综合分析）
        String profileCtx = patientProfileService.buildProfileContext(patientId);
        if (profileCtx != null && !profileCtx.isEmpty()) {
            sb.append(profileCtx);
        }

        ctx.detail = sb.toString();
        return ctx;
    }

    private String riskCn(String level) {
        if (level == null) return "\u672a\u8bc4\u4f30";
        return switch (level) {
            case "3" -> "\u9ad8\u5371";
            case "2" -> "\u4e2d\u5371";
            case "1" -> "\u4f4e\u5371";
            default -> "\u672a\u8bc4\u4f30";
        };
    }

    private String levelCn(String level) {
        if (level == null) return "";
        return switch (level) {
            case "3" -> "\u9ad8\u5371";
            case "2" -> "\u4e2d\u5371";
            case "1" -> "\u4f4e\u5371";
            default -> "";
        };
    }

    private String freqCn(String f) {
        if (f == null) return "-";
        return switch (f) {
            case "1" -> "\u6bcf\u65e51\u6b21";
            case "2" -> "\u6bcf\u65e52\u6b21";
            case "3" -> "\u6bcf\u65e53\u6b21";
            default -> f;
        };
    }

    private String guideTypeCn(String t) {
        if (t == null) return "\u6307\u5bfc";
        return switch (t) {
            case "1" -> "\u8c03\u836f";
            case "2" -> "\u7528\u836f\u63d0\u9192";
            case "3" -> "\u996e\u98df\u63a8\u8350";
            case "4" -> "\u8fd0\u52a8\u5efa\u8bae";
            default -> "\u6307\u5bfc";
        };
    }

    /** \u60a3\u8005\u4e0a\u4e0b\u6587 */
    private static class PatientContext {
        Long patientId;
        String patientName;
        String detail;
        /** 患者病种编码集合，用于知识库检索 */
        java.util.List<String> diseaseCodes = new java.util.ArrayList<>();

        String toPrompt() {
            return "\u4ee5\u4e0b\u662f\u9700\u8981\u53c2\u8003\u7684\u60a3\u8005\u5065\u5eb7\u6570\u636e\uff1a\n" + detail;
        }
    }
}
