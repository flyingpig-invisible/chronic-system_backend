package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.*;
import com.chronic.modules.chronic.mapper.*;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.modules.miniapp.service.MpDataService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * 小程序端数据查询（家属查看绑定患者）
 *
 * @author chronic
 */
@Service
public class MpDataServiceImpl implements MpDataService {

    @Autowired
    private MpFamilyPatientMapper familyPatientMapper;

    @Autowired
    private CdPatientMapper patientMapper;

    @Autowired
    private CdMeasureMapper measureMapper;

    @Autowired
    private CdWarningMapper warningMapper;

    @Autowired
    private CdPlanMapper planMapper;

    @Autowired
    private CdRemindMapper remindMapper;

    @Autowired
    private CdGuideMapper guideMapper;

    @Autowired
    private CdArticleMapper articleMapper;

    @Autowired
    private CdHealthScoreMapper scoreMapper;

    @Override
    public List<Map<String, Object>> myPatientList() {
        LoginUser user = SecurityUtils.getLoginUser();
        List<Map<String, Object>> result = new ArrayList<>();

        // 患者本人：直接返回自己的档案，不需要绑定关系
        if ("1".equals(user.getMpRole())) {
            CdPatient self = patientMapper.selectOne(new LambdaQueryWrapper<CdPatient>()
                    .eq(CdPatient::getMpUserId, user.getUserId())
                    .last("limit 1"));
            if (self != null) {
                Map<String, Object> m = buildPatientItem(self);
                m.put("relation", "本人");
                result.add(m);
            }
            return result;
        }

        // 家属：返回已绑定的患者
        List<MpFamilyPatient> relations = familyPatientMapper.selectList(new LambdaQueryWrapper<MpFamilyPatient>()
                .eq(MpFamilyPatient::getFamilyUserId, user.getUserId()));
        for (MpFamilyPatient rel : relations) {
            CdPatient patient = patientMapper.selectById(rel.getPatientId());
            if (patient == null) {
                continue;
            }
            Map<String, Object> m = buildPatientItem(patient);
            m.put("id", rel.getId());
            m.put("relation", rel.getRelation());
            result.add(m);
        }
        return result;
    }

    /** 构建患者列表项 */
    private Map<String, Object> buildPatientItem(CdPatient patient) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", patient.getPatientId());
        m.put("patientNo", patient.getPatientNo());
        m.put("name", patient.getName());
        m.put("gender", patient.getGender());
        m.put("age", patient.getAge());
        m.put("phone", patient.getPhone());
        m.put("riskLevel", patient.getRiskLevel());
        m.put("riskScore", patient.getRiskScore());
        m.put("pendingWarningCount", patient.getPendingWarningCount());
        return m;
    }

    @Override
    public void checkPermission(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            throw new ServiceException(401, "未登录");
        }
        // 患者本人
        if ("1".equals(user.getMpRole())) {
            CdPatient self = patientMapper.selectOne(new LambdaQueryWrapper<CdPatient>()
                    .eq(CdPatient::getMpUserId, user.getUserId()).last("limit 1"));
            if (self != null && self.getPatientId().equals(patientId)) {
                return;
            }
        }
        Long count = familyPatientMapper.selectCount(new LambdaQueryWrapper<MpFamilyPatient>()
                .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                .eq(MpFamilyPatient::getPatientId, patientId));
        if (count == null || count == 0) {
            throw new ServiceException(403, "无权查看该患者数据");
        }
    }

    @Override
    public Map<String, Object> overview(Long patientId) {
        checkPermission(patientId);
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", patient.getPatientId());
        m.put("patientNo", patient.getPatientNo());
        m.put("name", patient.getName());
        m.put("gender", patient.getGender());
        m.put("age", patient.getAge());
        m.put("phone", patient.getPhone());
        m.put("riskLevel", patient.getRiskLevel());
        m.put("riskScore", patient.getRiskScore());
        m.put("pendingWarningCount", patient.getPendingWarningCount());

        List<CdMeasure> latest = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit 6"));
        List<Map<String, Object>> indicators = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (CdMeasure me : latest) {
            if (seen.contains(me.getIndicatorCode())) {
                continue;
            }
            seen.add(me.getIndicatorCode());
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("indicatorCode", me.getIndicatorCode());
            i.put("indicatorName", me.getIndicatorName());
            i.put("value", me.getValue());
            i.put("unit", me.getUnit());
            i.put("measureTime", me.getMeasureTime());
            i.put("abnormalFlag", me.getAbnormalFlag());
            indicators.add(i);
        }
        m.put("indicators", indicators);

        List<CdHealthScore> scores = scoreMapper.selectList(new LambdaQueryWrapper<CdHealthScore>()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .last("limit 1"));
        if (!scores.isEmpty()) {
            CdHealthScore s = scores.get(0);
            Map<String, Object> sc = new LinkedHashMap<>();
            sc.put("totalScore", s.getTotalScore());
            sc.put("riskLevel", s.getRiskLevel());
            sc.put("calcTime", s.getCalcTime());
            m.put("healthScore", sc);
        }
        return m;
    }

    @Override
    public List<Map<String, Object>> measures(Long patientId, String indicatorCode, Integer limit) {
        checkPermission(patientId);
        LambdaQueryWrapper<CdMeasure> w = new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .eq(StringUtils.hasText(indicatorCode), CdMeasure::getIndicatorCode, indicatorCode)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit " + (limit == null || limit <= 0 ? 30 : Math.min(limit, 200)));
        List<CdMeasure> list = measureMapper.selectList(w);
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdMeasure me : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("measureId", me.getMeasureId());
            i.put("indicatorCode", me.getIndicatorCode());
            i.put("indicatorName", me.getIndicatorName());
            i.put("value", me.getValue());
            i.put("unit", me.getUnit());
            i.put("measureTime", me.getMeasureTime());
            i.put("abnormalFlag", me.getAbnormalFlag());
            result.add(i);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> warnings(Long patientId) {
        checkPermission(patientId);
        List<CdWarning> list = warningMapper.selectList(new LambdaQueryWrapper<CdWarning>()
                .eq(CdWarning::getPatientId, patientId)
                .orderByDesc(CdWarning::getTriggerTime)
                .last("limit 50"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdWarning w : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("warningId", w.getWarningId());
            i.put("indicatorName", w.getIndicatorName());
            i.put("indicatorValue", w.getIndicatorValue());
            i.put("unit", w.getUnit());
            i.put("thresholdDesc", w.getThresholdDesc());
            i.put("warningLevel", w.getWarningLevel());
            i.put("handleStatus", w.getHandleStatus());
            i.put("triggerTime", w.getTriggerTime());
            result.add(i);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> plans(Long patientId) {
        checkPermission(patientId);
        List<CdPlan> list = planMapper.selectList(new LambdaQueryWrapper<CdPlan>()
                .eq(CdPlan::getPatientId, patientId)
                .orderByDesc(CdPlan::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdPlan p : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("planId", p.getPlanId());
            i.put("planName", p.getPlanName());
            i.put("beginDate", p.getBeginDate());
            i.put("endDate", p.getEndDate());
            i.put("targetDesc", p.getTargetDesc());
            i.put("planStatus", p.getPlanStatus());
            i.put("executeRate", p.getExecuteRate());
            i.put("doctorName", p.getDoctorName());
            result.add(i);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> reminds(Long patientId) {
        checkPermission(patientId);
        List<CdRemind> list = remindMapper.selectList(new LambdaQueryWrapper<CdRemind>()
                .eq(CdRemind::getPatientId, patientId)
                .eq(CdRemind::getStatus, "1")
                .orderByDesc(CdRemind::getCreateTime));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdRemind r : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("remindId", r.getRemindId());
            i.put("medicineName", r.getMedicineName());
            i.put("dosage", r.getDosage());
            i.put("frequency", r.getFrequency());
            i.put("remindTimes", r.getRemindTimes());
            result.add(i);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> guides(Long patientId) {
        checkPermission(patientId);
        List<CdGuide> list = guideMapper.selectList(new LambdaQueryWrapper<CdGuide>()
                .eq(CdGuide::getPatientId, patientId)
                .orderByDesc(CdGuide::getGuideTime)
                .last("limit 50"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdGuide g : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("guideId", g.getGuideId());
            i.put("guideType", g.getGuideType());
            i.put("content", g.getContent());
            i.put("doctorName", g.getDoctorName());
            i.put("guideTime", g.getGuideTime());
            i.put("readStatus", g.getReadStatus());
            result.add(i);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> articles(Long patientId) {
        checkPermission(patientId);
        List<CdArticle> list = articleMapper.selectList(new LambdaQueryWrapper<CdArticle>()
                .eq(CdArticle::getPublishStatus, "1")
                .orderByDesc(CdArticle::getPublishTime)
                .last("limit 20"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (CdArticle a : list) {
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("articleId", a.getArticleId());
            i.put("title", a.getTitle());
            i.put("categoryName", a.getCategoryName());
            i.put("summary", a.getSummary());
            i.put("coverImage", a.getCoverImage());
            i.put("viewCount", a.getViewCount());
            i.put("publishTime", a.getPublishTime());
            result.add(i);
        }
        return result;
    }
}
