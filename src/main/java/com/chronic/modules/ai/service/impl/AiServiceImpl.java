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
import com.chronic.modules.chronic.service.impl.CdScoreServiceImpl;
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
 * AI 健康助手服务实现
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

    /** 指标中文名 */
    private static final Map<String, String> INDICATOR_CN = Map.of(
            "sbp", "收缩压", "dbp", "舒张压", "hr", "心率",
            "spo2", "血氧饱和度", "glu", "血糖", "rr", "呼吸频率");

    /** 病种中文名 */
    private static final Map<String, String> DISEASE_CN = Map.of(
            "1", "高血压", "2", "2型糖尿病", "3", "冠心病", "4", "慢阻肺");

    @Override
    public AiChatVO doctorChat(AiChatDTO dto) {
        LoginUser user = SecurityUtils.getLoginUser();
        String systemPrompt = "你是一名专业的慢病管理医生助手，服务于医生用户。"
                + "请基于提供的患者数据，给出专业、简洁、可操作的临床建议。"
                + "回答要结构清晰，必要时分点说明；不确定的信息不要编造。"
                + "如涉及用药，请提醒遵医嘱。";

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

        String roleName = "1".equals(user.getMpRole()) ? "患者本人" : "患者家属";
        String systemPrompt = "你是一名慢病健康管理助手，正在为" + roleName + "提供健康咨询服务。"
                + "请用通俗易懂的语言回答，避免过多医学术语；"
                + "给出具体可执行的建议（饮食、运动、用药、复诊等）；"
                + "若患者出现严重异常指标或高危预警，必须明确提醒及时就医，必要时拨打 120。"
                + "你不能提供诊断结论，不能开处方，用药请遵医嘱。";

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
            throw new ServiceException("请先选择患者");
        }
        String systemPrompt = "你是一名专业的慢病管理医生助手。请基于提供的患者专属档案与健康数据，"
                + "输出结构化的风险分析与干预建议，包含：①关键风险点（过敏、用药、心理、生活方式等）；"
                + "②需重点关注的指标；③个体化干预建议（饮食、运动、用药、复诊）；④需补充的档案信息。"
                + "档案缺失的字段请明确指出，不要编造；用药请遵医嘱。";
        PatientContext ctx = buildContext(patientId);
        if (ctx.patientId == null) {
            throw new ServiceException("患者不存在");
        }
        String question = "请对该患者的专属档案进行分析，给出风险提示与干预建议。";
        return doChat(user, "doctor", patientId, ctx, systemPrompt, question);
    }

    @Override
    public AiChatVO analyzeProfileForFamily(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUser();
        Long pid = resolveMpPatientId(user, patientId);
        String systemPrompt = "你是一名慢病健康管理助手，正在为患者家属分析患者的专属档案。"
                + "请用通俗易懂的语言，总结家属需要关注的重点（饮食、作息、情绪、在家观察重点），"
                + "并提醒哪些情况需及时就医。不提供诊断结论与处方，用药请遵医嘱。";
        PatientContext ctx = buildContext(pid);
        String question = "请对该患者的专属档案进行分析，告诉家属日常需要注意什么。";
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

    /** 校验并解析小程序端的患者ID */
    private Long resolveMpPatientId(LoginUser user, Long requestPatientId) {
        if ("1".equals(user.getMpRole())) {
            CdPatient self = patientMapper.selectOne(new LambdaQueryWrapper<CdPatient>()
                    .eq(CdPatient::getMpUserId, user.getUserId()).last("limit 1"));
            if (self == null) {
                throw new ServiceException("未匹配到患者档案");
            }
            // 患者只能访问本人数据
            if (requestPatientId != null && !requestPatientId.equals(self.getPatientId())) {
                throw new ServiceException(403, "无权查看该患者数据");
            }
            return self.getPatientId();
        }
        Long patientId = requestPatientId;
        if (patientId == null) {
            List<MpFamilyPatient> rels = familyPatientMapper.selectList(new LambdaQueryWrapper<MpFamilyPatient>()
                    .eq(MpFamilyPatient::getFamilyUserId, user.getUserId()));
            if (rels.isEmpty()) {
                throw new ServiceException("未绑定患者");
            }
            patientId = rels.get(0).getPatientId();
        } else {
            Long count = familyPatientMapper.selectCount(new LambdaQueryWrapper<MpFamilyPatient>()
                    .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                    .eq(MpFamilyPatient::getPatientId, patientId));
            if (count == null || count == 0) {
                throw new ServiceException(403, "无权查看该患者数据");
            }
        }
        return patientId;
    }

    /** 执行对话并记录 */
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

    /** 构建患者健康上下文 */
    private PatientContext buildContext(Long patientId) {
        PatientContext ctx = new PatientContext();
        if (patientId == null) {
            ctx.patientId = null;
            ctx.patientName = null;
            ctx.detail = "当前无特定患者上下文。";
            return ctx;
        }
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            ctx.patientId = null;
            ctx.detail = "当前无特定患者上下文。";
            return ctx;
        }
        ctx.patientId = patientId;
        ctx.patientName = patient.getName();

        StringBuilder sb = new StringBuilder();
        sb.append("【患者基本信息】\n");
        sb.append("姓名：").append(patient.getName());
        sb.append("，性别：").append("1".equals(patient.getGender()) ? "男" : "女");
        if (patient.getBirthday() != null) {
            sb.append("，出生日期：").append(patient.getBirthday());
        }
        sb.append("，健康等级：").append(healthCn(CdScoreServiceImpl.healthLevel(patient.getHealthScore())));
        sb.append("，健康评分：").append(patient.getHealthScore() == null ? "-" : patient.getHealthScore());
        sb.append("，待处理预警数：").append(patient.getPendingWarningCount() == null ? 0 : patient.getPendingWarningCount());
        sb.append("\n");

        // 病种
        List<CdPatientDisease> diseases = diseaseMapper.selectList(new LambdaQueryWrapper<CdPatientDisease>()
                .eq(CdPatientDisease::getPatientId, patientId));
        for (CdPatientDisease d : diseases) {
            if (d.getDiseaseCode() != null) {
                ctx.diseaseCodes.add(d.getDiseaseCode());
            }
        }
        if (!diseases.isEmpty()) {
            sb.append("\n【患者病种】\n");
            for (CdPatientDisease d : diseases) {
                sb.append("- ").append(d.getDiseaseName() == null ? DISEASE_CN.getOrDefault(d.getDiseaseCode(), d.getDiseaseCode()) : d.getDiseaseName());
                if (d.getDiagnoseDate() != null) {
                    sb.append("（诊断日期：").append(d.getDiagnoseDate()).append("）");
                }
                sb.append("\n");
            }
        }

        // 最近监测
        List<CdMeasure> measures = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit 12"));
        if (!measures.isEmpty()) {
            sb.append("\n【最近监测数据】\n");
            for (CdMeasure m : measures) {
                sb.append("- ").append(m.getMeasureTime() == null ? "" : m.getMeasureTime().format(DTF)).append(" ");
                sb.append(m.getIndicatorName() == null ? INDICATOR_CN.getOrDefault(m.getIndicatorCode(), m.getIndicatorCode()) : m.getIndicatorName());
                sb.append("：").append(m.getValue()).append(m.getUnit() == null ? "" : m.getUnit());
                sb.append("1".equals(m.getAbnormalFlag()) ? "（异常）" : "");
                sb.append("\n");
            }
        }

        // 预警
        List<CdWarning> warnings = warningMapper.selectList(new LambdaQueryWrapper<CdWarning>()
                .eq(CdWarning::getPatientId, patientId)
                .orderByDesc(CdWarning::getTriggerTime)
                .last("limit 8"));
        if (!warnings.isEmpty()) {
            sb.append("\n【最近预警】\n");
            for (CdWarning w : warnings) {
                sb.append("- ").append(w.getTriggerTime() == null ? "" : w.getTriggerTime().format(DTF)).append(" ");
                sb.append(levelCn(w.getWarningLevel())).append("预警：");
                sb.append(w.getIndicatorName()).append(" ").append(w.getIndicatorValue()).append(w.getUnit() == null ? "" : w.getUnit());
                sb.append("，阈值：").append(w.getThresholdDesc() == null ? "-" : w.getThresholdDesc());
                sb.append("，状态：").append("2".equals(w.getHandleStatus()) ? "已处理" : "未处理");
                sb.append("\n");
            }
        }

        // 用药提醒
        List<CdRemind> reminds = remindMapper.selectList(new LambdaQueryWrapper<CdRemind>()
                .eq(CdRemind::getPatientId, patientId)
                .last("limit 10"));
        if (!reminds.isEmpty()) {
            sb.append("\n【用药方案】\n");
            for (CdRemind r : reminds) {
                sb.append("- ").append(r.getMedicineName());
                sb.append("，剂量：").append(r.getDosage() == null ? "-" : r.getDosage());
                sb.append("，频次：").append(freqCn(r.getFrequency()));
                sb.append("，用药时间：").append(r.getRemindTimes() == null ? "-" : r.getRemindTimes());
                sb.append("\n");
            }
        }

        // 医生指导
        List<CdGuide> guides = guideMapper.selectList(new LambdaQueryWrapper<CdGuide>()
                .eq(CdGuide::getPatientId, patientId)
                .orderByDesc(CdGuide::getGuideTime)
                .last("limit 6"));
        if (!guides.isEmpty()) {
            sb.append("\n【医生最新指导】\n");
            for (CdGuide g : guides) {
                sb.append("- ").append(g.getGuideTime() == null ? "" : g.getGuideTime().format(DTF)).append(" ");
                sb.append("[").append(guideTypeCn(g.getGuideType())).append("] ");
                sb.append(g.getContent()).append("\n");
            }
        }

        // 健康计划
        List<CdPlan> plans = planMapper.selectList(new LambdaQueryWrapper<CdPlan>()
                .eq(CdPlan::getPatientId, patientId)
                .orderByDesc(CdPlan::getCreateTime)
                .last("limit 3"));
        if (!plans.isEmpty()) {
            sb.append("\n【健康管理计划】\n");
            for (CdPlan p : plans) {
                sb.append("- ").append(p.getPlanName());
                sb.append("，目标：").append(p.getTargetDesc() == null ? "-" : p.getTargetDesc());
                sb.append("，执行率：").append(p.getExecuteRate() == null ? 0 : p.getExecuteRate()).append("%\n");
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

    private String healthCn(String level) {
        if (level == null) return "未评估";
        return switch (level) {
            case "3" -> "良好";
            case "2" -> "一般";
            case "1" -> "较差";
            default -> "未评估";
        };
    }

    private String levelCn(String level) {
        if (level == null) return "";
        return switch (level) {
            case "3" -> "高危";
            case "2" -> "中危";
            case "1" -> "低危";
            default -> "";
        };
    }

    private String freqCn(String f) {
        if (f == null) return "-";
        return switch (f) {
            case "1" -> "每日1次";
            case "2" -> "每日2次";
            case "3" -> "每日3次";
            default -> f;
        };
    }

    private String guideTypeCn(String t) {
        if (t == null) return "指导";
        return switch (t) {
            case "1" -> "调药";
            case "2" -> "用药提醒";
            case "3" -> "饮食推荐";
            case "4" -> "运动建议";
            default -> "指导";
        };
    }

    /** 患者上下文 */
    private static class PatientContext {
        Long patientId;
        String patientName;
        String detail;
        /** 患者病种编码集合，用于知识库检索 */
        java.util.List<String> diseaseCodes = new java.util.ArrayList<>();

        String toPrompt() {
            return "以下是需要参考的患者健康数据：\n" + detail;
        }
    }
}
