package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdPatientDisease;
import com.chronic.modules.chronic.entity.CdPlan;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdGuideMapper;
import com.chronic.modules.chronic.mapper.CdHealthScoreMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientDiseaseMapper;
import com.chronic.modules.miniapp.mapper.MpPatientMapper;
import com.chronic.modules.chronic.mapper.CdPlanMapper;
import com.chronic.modules.chronic.mapper.CdRemindLogMapper;
import com.chronic.modules.chronic.mapper.CdRemindMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import com.chronic.modules.chronic.mapper.CdWarningRuleMapper;
import com.chronic.modules.chronic.dto.MeasureOnceDTO;
import com.chronic.modules.chronic.service.CdMeasureService;
import com.chronic.modules.chronic.service.CdPatientService;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpPatientService;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.service.SysIndicatorService;
import com.chronic.modules.miniapp.vo.MpDashboardVO;
import com.chronic.modules.miniapp.vo.MpDiseaseVO;
import com.chronic.modules.miniapp.vo.MpHealthScoreVO;
import com.chronic.modules.miniapp.vo.MpIndicatorVO;
import com.chronic.modules.miniapp.vo.MpPatientVO;
import com.chronic.modules.miniapp.vo.MpPointVO;
import com.chronic.modules.miniapp.vo.MpTrendVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 小程序患者档案服务实现
 * <p>
 * 全部数据直接读取医生端使用的同一批表（cd_patient / cd_measure / cd_warning / cd_health_score ...），
 * 保证患者端看到的指标、风险等级与医生端完全一致。
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpPatientServiceImpl implements MpPatientService {

    private final MpAccessService accessService;
    private final CdPatientService patientService;
    private final CdMeasureService measureService;
    private final SysIndicatorService indicatorService;
    private final MpPatientMapper patientMapper;
    private final CdPatientDiseaseMapper patientDiseaseMapper;
    private final CdMeasureMapper measureMapper;
    private final CdWarningMapper warningMapper;
    private final CdWarningRuleMapper warningRuleMapper;
    private final CdHealthScoreMapper healthScoreMapper;
    private final CdPlanMapper planMapper;
    private final CdRemindMapper remindMapper;
    private final CdRemindLogMapper remindLogMapper;
    private final CdGuideMapper guideMapper;
    private final MpFamilyPatientMapper familyPatientMapper;

    private static final Map<String, String> RISK_NAME = Map.of("1", "低危", "2", "中危", "3", "高危");
    private static final Map<String, String> SEVERITY_NAME = Map.of("1", "轻度", "2", "中度", "3", "重度");
    private static final Map<String, String> INDICATOR_NAME = new LinkedHashMap<>();
    private static final Map<String, String> INDICATOR_UNIT = new HashMap<>();

    static {
        INDICATOR_NAME.put("sbp", "收缩压");
        INDICATOR_NAME.put("dbp", "舒张压");
        INDICATOR_NAME.put("hr", "心率");
        INDICATOR_NAME.put("glu", "血糖");
        INDICATOR_NAME.put("spo2", "血氧");
        INDICATOR_NAME.put("rr", "呼吸频率");
        INDICATOR_NAME.put("sleep", "睡眠时长");
        INDICATOR_UNIT.put("sbp", "mmHg");
        INDICATOR_UNIT.put("dbp", "mmHg");
        INDICATOR_UNIT.put("hr", "次/分");
        INDICATOR_UNIT.put("glu", "mmol/L");
        INDICATOR_UNIT.put("spo2", "%");
        INDICATOR_UNIT.put("rr", "次/分");
        INDICATOR_UNIT.put("sleep", "小时");
    }

    @Override
    public List<MpPatientVO> listPatients() {
        List<Long> ids = accessService.accessiblePatientIds();
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<CdPatient> patients = patientMapper.selectPatients(ids);
        Map<Long, CdHealthScore> scoreMap = latestScores(ids);
        Map<Long, String> relationMap = new HashMap<>();
        if (accessService.isFamily()) {
            familyPatientMapper.selectList(Wrappers.<MpFamilyPatient>lambdaQuery()
                            .eq(MpFamilyPatient::getFamilyUserId, accessService.currentUser().getUserId()))
                    .forEach(rel -> relationMap.put(rel.getPatientId(), rel.getRelation()));
        }
        return patients.stream()
                .map(p -> toVO(p, scoreMap.get(p.getPatientId()), relationMap.get(p.getPatientId())))
                .sorted(Comparator.comparing(MpPatientVO::getPatientId))
                .collect(Collectors.toList());
    }

    @Override
    public MpPatientVO patientDetail(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        CdPatient patient = accessService.requirePatient(id);
        MpPatientVO vo = toVO(patient, latestScore(id), null);
        if (accessService.isFamily()) {
            MpFamilyPatient rel = familyPatientMapper.selectOne(Wrappers.<MpFamilyPatient>lambdaQuery()
                    .eq(MpFamilyPatient::getFamilyUserId, accessService.currentUser().getUserId())
                    .eq(MpFamilyPatient::getPatientId, id).last("limit 1"));
            vo.setRelation(rel == null ? null : rel.getRelation());
        }
        return vo;
    }

    @Override
    public List<MpDiseaseVO> diseases(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        return patientDiseaseMapper.selectList(Wrappers.<CdPatientDisease>lambdaQuery()
                        .eq(CdPatientDisease::getPatientId, id)
                        .orderByAsc(CdPatientDisease::getDiseaseCode))
                .stream().map(d -> {
                    MpDiseaseVO vo = new MpDiseaseVO();
                    vo.setDiseaseCode(d.getDiseaseCode());
                    vo.setDiseaseName(d.getDiseaseName());
                    vo.setDiagnoseDate(d.getDiagnoseDate());
                    vo.setSeverity(d.getSeverity());
                    vo.setSeverityName(SEVERITY_NAME.getOrDefault(d.getSeverity(), "-"));
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public List<MpIndicatorVO> latestIndicators(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        Map<String, String> reference = ruleReference(id);
        Map<String, MpIndicatorVO> seen = new LinkedHashMap<>();
        for (String code : INDICATOR_NAME.keySet()) {
            seen.put(code, null);
            // 每个指标单独取最近一条，避免高频指标（如心率）占满结果导致其他指标缺失
            CdMeasure m = measureMapper.selectOne(Wrappers.<CdMeasure>lambdaQuery()
                    .eq(CdMeasure::getPatientId, id)
                    .eq(CdMeasure::getIndicatorCode, code)
                    .orderByDesc(CdMeasure::getMeasureTime)
                    .last("limit 1"));
            if (m == null) {
                continue;
            }
            MpIndicatorVO vo = new MpIndicatorVO();
            vo.setIndicatorCode(m.getIndicatorCode());
            vo.setIndicatorName(nameOf(m));
            vo.setValue(m.getValue());
            vo.setUnit(unitOf(m));
            vo.setMeasureTime(m.getMeasureTime());
            vo.setAbnormalFlag(m.getAbnormalFlag());
            vo.setReferenceDesc(reference.get(m.getIndicatorCode()));
            seen.put(m.getIndicatorCode(), vo);
        }
        List<MpIndicatorVO> result = new ArrayList<>();
        seen.values().forEach(v -> {
            if (v != null) {
                result.add(v);
            }
        });
        return result;
    }

    @Override
    public MpDashboardVO dashboard(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        MpDashboardVO vo = new MpDashboardVO();
        vo.setPatient(patientDetail(id));
        vo.setIndicators(latestIndicators(id));
        CdHealthScore score = latestScore(id);
        if (score != null) {
            vo.setHealthScore(toScoreVO(score));
        }
        vo.setWarningStat(warningStat(id));
        vo.setActivePlanCount(Math.toIntExact(planMapper.selectCount(Wrappers.<CdPlan>lambdaQuery()
                .eq(CdPlan::getPatientId, id).eq(CdPlan::getPlanStatus, "1"))));
        vo.setUnreadGuideCount(Math.toIntExact(guideMapper.selectCount(Wrappers.<com.chronic.modules.chronic.entity.CdGuide>lambdaQuery()
                .eq(com.chronic.modules.chronic.entity.CdGuide::getPatientId, id)
                .eq(com.chronic.modules.chronic.entity.CdGuide::getReadStatus, "0"))));
        vo.setMedication(medicationStat(id));
        vo.setServerTime(LocalDateTime.now());
        return vo;
    }

    @Override
    public List<MpTrendVO> trend(Long patientId, Integer days) {
        Long id = accessService.resolvePatientId(patientId);
        List<CdMeasure> all = measureMapper.selectList(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, id)
                .orderByAsc(CdMeasure::getMeasureTime));
        if (all.isEmpty()) {
            return new ArrayList<>();
        }
        LocalDateTime end = all.get(all.size() - 1).getMeasureTime();
        int span = days == null || days <= 0 ? Integer.MAX_VALUE : days;
        // 以最近一次监测时间为基准，与医生端趋势统计口径一致
        List<CdMeasure> scoped = all.stream()
                .filter(m -> span == Integer.MAX_VALUE
                        || !m.getMeasureTime().isBefore(end.minusDays(span).toLocalDate().atStartOfDay()))
                .collect(Collectors.toList());
        Map<String, List<CdMeasure>> grouped = new LinkedHashMap<>();
        for (String code : INDICATOR_NAME.keySet()) {
            grouped.put(code, new ArrayList<>());
        }
        for (CdMeasure m : scoped) {
            if (grouped.containsKey(m.getIndicatorCode())) {
                grouped.get(m.getIndicatorCode()).add(m);
            }
        }
        List<MpTrendVO> result = new ArrayList<>();
        grouped.forEach((code, list) -> {
            if (list.isEmpty()) {
                return;
            }
            MpTrendVO vo = new MpTrendVO();
            vo.setIndicatorCode(code);
            // 名称/单位统一走字典回退：部分录入通道会写成空串，直接取首条会导致前端胶囊空白
            vo.setIndicatorName(INDICATOR_NAME.getOrDefault(code, nameOf(list.get(0))));
            vo.setUnit(INDICATOR_UNIT.getOrDefault(code, unitOf(list.get(0))));
            List<MpPointVO> points = new ArrayList<>();
            BigDecimal sum = BigDecimal.ZERO;
            BigDecimal max = null;
            BigDecimal min = null;
            int abnormal = 0;
            for (CdMeasure m : list) {
                MpPointVO point = new MpPointVO();
                point.setMeasureTime(m.getMeasureTime());
                point.setValue(m.getValue());
                point.setAbnormalFlag(m.getAbnormalFlag());
                points.add(point);
                if (m.getValue() != null) {
                    sum = sum.add(m.getValue());
                    max = max == null || m.getValue().compareTo(max) > 0 ? m.getValue() : max;
                    min = min == null || m.getValue().compareTo(min) < 0 ? m.getValue() : min;
                }
                if ("1".equals(m.getAbnormalFlag())) {
                    abnormal++;
                }
            }
            vo.setPoints(points);
            int size = list.size();
            vo.setAvgValue(sum.divide(BigDecimal.valueOf(size), 1, RoundingMode.HALF_UP));
            vo.setMaxValue(max);
            vo.setMinValue(min);
            vo.setLatestValue(list.get(size - 1).getValue());
            vo.setTotalCount(size);
            vo.setAbnormalCount(abnormal);
            vo.setNormalRate((int) Math.round((size - abnormal) * 100.0 / size));
            result.add(vo);
        });
        return result;
    }

    @Override
    public List<SysIndicator> indicatorOptions() {
        return indicatorService.listEnabled();
    }

    @Override
    public List<Long> addMeasures(MeasureOnceDTO dto) {
        if (dto == null || dto.getPatientId() == null) {
            throw new ServiceException("请选择患者");
        }
        // 患者本人只能为自己录入；家属可为其绑定患者代录
        accessService.requirePatient(dto.getPatientId());
        if (!"1".equals(dto.getDataSource())) {
            dto.setDataSource("2");
        }
        return measureService.addMeasureOnce(dto.getPatientId(), dto.getDiseaseCode(), dto.getMeasureTime(),
                dto.getDataSource(), dto.getEntries());
    }
    @Override
    public Long addMeasure(CdMeasure measure) {
        if (measure == null || measure.getPatientId() == null) {
            throw new ServiceException("请选择患者");
        }
        // 患者本人只能为自己录入；家属可为其绑定患者代录
        CdPatient patient = accessService.requirePatient(measure.getPatientId());
        if (measure.getValue() == null) {
            throw new ServiceException("请输入测量数值");
        }
        SysIndicator dict = indicatorService.listEnabled().stream()
                .filter(i -> i.getIndicatorCode().equals(measure.getIndicatorCode()))
                .findFirst().orElse(null);
        if (dict == null) {
            throw new ServiceException("指标不在指标字典中，请从字典中选择");
        }
        measure.setMeasureId(null);
        measure.setPatientName(patient.getName());
        measure.setIndicatorName(dict.getIndicatorName());
        measure.setUnit(dict.getUnit());
        measure.setDataSource("2");
        if (measure.getMeasureTime() == null) {
            measure.setMeasureTime(LocalDateTime.now());
        }
        return measureService.addMeasure(measure);
    }

    /** 历史数据可能缺失指标名称/单位，统一回退到指标字典 */
    private String nameOf(CdMeasure m) {
        String name = m.getIndicatorName();
        return name != null && !name.isBlank() ? name : INDICATOR_NAME.getOrDefault(m.getIndicatorCode(), m.getIndicatorCode());
    }

    private String unitOf(CdMeasure m) {
        String unit = m.getUnit();
        return unit != null && !unit.isBlank() ? unit : INDICATOR_UNIT.getOrDefault(m.getIndicatorCode(), "");
    }

    // ================= 内部方法 =================

    private Map<Long, CdHealthScore> latestScores(List<Long> ids) {
        Map<Long, CdHealthScore> map = new HashMap<>();
        if (ids.isEmpty()) {
            return map;
        }
        healthScoreMapper.selectLatestOfPatients(ids).forEach(s -> map.put(s.getPatientId(), s));
        return map;
    }

    private CdHealthScore latestScore(Long patientId) {
        List<CdHealthScore> list = healthScoreMapper.selectList(Wrappers.<CdHealthScore>lambdaQuery()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .orderByDesc(CdHealthScore::getScoreId)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0);
    }

    private MpHealthScoreVO toScoreVO(CdHealthScore score) {
        MpHealthScoreVO vo = new MpHealthScoreVO();
        vo.setTotalScore(score.getTotalScore());
        vo.setRiskLevel(score.getRiskLevel());
        vo.setRiskLevelName(RISK_NAME.getOrDefault(score.getRiskLevel(), "-"));
        vo.setCalcTime(score.getCalcTime());
        vo.setTriggerType(score.getTriggerType());
        return vo;
    }

    private MpPatientVO toVO(CdPatient patient, CdHealthScore score, String relation) {
        MpPatientVO vo = new MpPatientVO();
        vo.setPatientId(patient.getPatientId());
        vo.setPatientNo(patient.getPatientNo());
        vo.setName(patient.getName());
        vo.setGender(patient.getGender());
        vo.setBirthday(patient.getBirthday());
        vo.setPhone(patient.getPhone());
        vo.setIdCard(maskIdCard(patient.getIdCard()));
        vo.setAddress(patient.getAddress());
        vo.setDoctorId(patient.getDoctorId());
        vo.setDeptId(patient.getDeptId());
        // 小程序端已由 accessService 校验过归属，这里不能再走医生端的数据范围校验
        com.chronic.modules.chronic.vo.PatientVO detail = patientService.getPatientDetailUnscoped(patient.getPatientId());
        vo.setAge(detail.getAge());
        vo.setDoctorName(detail.getDoctorName());
        vo.setDeptName(detail.getDeptName());
        vo.setDiseases(detail.getDiseases());
        vo.setDiseaseCodes(detail.getDiseaseCodes());
        vo.setRiskLevel(score != null ? score.getRiskLevel() : patient.getRiskLevel());
        vo.setRiskLevelName(RISK_NAME.getOrDefault(vo.getRiskLevel(), "-"));
        vo.setRiskScore(score != null ? score.getTotalScore() : patient.getRiskScore());
        vo.setPendingWarningCount(toInt(warningStat(patient.getPatientId()).get("pending")));
        vo.setLastMeasureTime(latestMeasureTime(patient.getPatientId(), patient.getLastMeasureTime()));
        vo.setStatus(patient.getStatus());
        vo.setRemark(patient.getRemark());
        vo.setRelation(relation);
        return vo;
    }

    private Integer toInt(Object value) {
        return value instanceof Number ? ((Number) value).intValue() : 0;
    }

    /** 最近监测时间：以 cd_measure 最新记录为准，与医生端监测列表一致 */
    private LocalDateTime latestMeasureTime(Long patientId, LocalDateTime fallback) {
        CdMeasure latest = measureMapper.selectOne(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit 1"));
        return latest == null ? fallback : latest.getMeasureTime();
    }
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    private Map<String, String> ruleReference(Long patientId) {
        Map<String, String> map = new HashMap<>();
        List<CdWarningRule> rules = warningRuleMapper.selectList(Wrappers.<CdWarningRule>lambdaQuery()
                .eq(CdWarningRule::getStatus, "0"));
        rules.stream()
                .filter(r -> r.getIndicatorCode() != null && !map.containsKey(r.getIndicatorCode()))
                .sorted(Comparator.comparing(CdWarningRule::getWarningLevel))
                .forEach(r -> map.put(r.getIndicatorCode(), r.getThresholdDesc()));
        return map;
    }

    private Map<String, Object> warningStat(Long patientId) {
        List<CdWarning> list = warningMapper.selectList(Wrappers.<CdWarning>lambdaQuery()
                .eq(CdWarning::getPatientId, patientId));
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("high", list.stream().filter(w -> "3".equals(w.getWarningLevel())).count());
        stat.put("middle", list.stream().filter(w -> "2".equals(w.getWarningLevel())).count());
        stat.put("low", list.stream().filter(w -> "1".equals(w.getWarningLevel())).count());
        stat.put("pending", list.stream().filter(w -> "0".equals(w.getHandleStatus())).count());
        stat.put("total", list.size());
        return stat;
    }

    private Map<String, Object> medicationStat(Long patientId) {
        LocalDate today = LocalDate.now();
        List<CdRemind> reminds = remindMapper.selectList(Wrappers.<CdRemind>lambdaQuery()
                .eq(CdRemind::getPatientId, patientId)
                .eq(CdRemind::getStatus, "1")
                .le(CdRemind::getBeginDate, today)
                .ge(CdRemind::getEndDate, today));
        Set<String> planTimes = new LinkedHashSet<>();
        reminds.forEach(r -> {
            if (r.getRemindTimes() == null) {
                return;
            }
            for (String t : r.getRemindTimes().split(",")) {
                if (!t.isBlank()) {
                    planTimes.add(t.trim());
                }
            }
        });
        long checked = remindLogMapper.selectCount(Wrappers.<com.chronic.modules.chronic.entity.CdRemindLog>lambdaQuery()
                .eq(com.chronic.modules.chronic.entity.CdRemindLog::getPatientId, patientId)
                .eq(com.chronic.modules.chronic.entity.CdRemindLog::getPlanDate, today)
                .eq(com.chronic.modules.chronic.entity.CdRemindLog::getCheckStatus, "1"));
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("total", planTimes.size());
        stat.put("checked", checked);
        stat.put("medicineCount", reminds.size());
        return stat;
    }
}
