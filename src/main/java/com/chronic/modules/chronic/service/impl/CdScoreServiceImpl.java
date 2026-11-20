package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ScoreWeightSaveDTO;
import com.chronic.modules.chronic.entity.CdDiseaseWeight;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdPatientDisease;
import com.chronic.modules.chronic.entity.CdScoreWeight;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdDiseaseWeightMapper;
import com.chronic.modules.chronic.mapper.CdHealthScoreMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientDiseaseMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdScoreWeightMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import com.chronic.modules.chronic.query.ScoreQuery;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.mapper.SysIndicatorMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 健康评分：基于最近监测数据加权计算风险分（0-100，越高风险越大）
 *
 * @author chronic
 */
@Service
public class CdScoreServiceImpl extends ServiceImpl<CdHealthScoreMapper, CdHealthScore> implements CdScoreService {

    @Autowired
    private CdMeasureMapper measureMapper;

    @Autowired
    private CdWarningMapper warningMapper;

    @Autowired
    private CdScoreWeightMapper weightMapper;

    @Autowired
    private CdPatientMapper patientMapper;

    @Autowired
    private CdPatientDiseaseMapper patientDiseaseMapper;

    @Autowired
    private CdDiseaseWeightMapper diseaseWeightMapper;

    @Autowired
    private CdWarningService warningService;

    @Autowired
    private SysIndicatorMapper indicatorMapper;

    /** 病种编码 -> 名称 */
    private static final Map<String, String> DISEASE_NAME = Map.of(
            "1", "高血压",
            "2", "2型糖尿病",
            "3", "冠心病",
            "4", "慢阻肺"
    );

    /** 指标参考区间与危险区间：code -> [正常下限, 正常上限, 危险下限, 危险上限] */
    private static final Map<String, double[]> RANGE = new HashMap<>();

    static {
        RANGE.put("sbp", new double[]{90, 140, 90, 180});
        RANGE.put("dbp", new double[]{60, 90, 60, 110});
        RANGE.put("hr", new double[]{60, 100, 50, 130});
        RANGE.put("spo2", new double[]{95, 100, 90, 100});
        RANGE.put("glu", new double[]{3.9, 7.0, 3.0, 13.0});
        RANGE.put("rr", new double[]{12, 20, 10, 30});
    }

    @Override
    public PageResult<CdHealthScore> queryPage(ScoreQuery query) {
        return pageAll(query);
    }

    @Override
    public PageResult<CdHealthScore> pageAll(ScoreQuery query) {
        LambdaQueryWrapper<CdHealthScore> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdHealthScore::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getRiskLevel()), CdHealthScore::getRiskLevel, query.getRiskLevel())
         .eq(StringUtils.hasText(query.getTriggerType()), CdHealthScore::getTriggerType, query.getTriggerType())
         .orderByDesc(CdHealthScore::getCalcTime);
        Page<CdHealthScore> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        fillPatientName(page.getRecords());
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 填充患者姓名 */
    private void fillPatientName(java.util.List<CdHealthScore> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        list.forEach(s -> { if (s.getPatientId() != null) ids.add(s.getPatientId()); });
        if (ids.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        measureMapper.selectList(com.baomidou.mybatisplus.core.toolkit.Wrappers.<CdMeasure>lambdaQuery()
                        .in(CdMeasure::getPatientId, ids)
                        .select(CdMeasure::getPatientId, CdMeasure::getPatientName))
                .forEach(m -> nameMap.putIfAbsent(m.getPatientId(), m.getPatientName()));
        list.forEach(s -> s.setPatientName(nameMap.get(s.getPatientId())));
    }

    @Override
    public Map<String, Object> calcScore(Long patientId) {
        if (patientId == null) {
            throw new ServiceException("患者不能为空");
        }
        // 取每个指标最近一次监测值（同一指标可能来自不同病种，统一取全量最新）
        List<CdMeasure> measures = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime));
        Map<String, CdMeasure> latest = new LinkedHashMap<>();
        for (CdMeasure m : measures) {
            if (m.getIndicatorCode() != null) {
                latest.putIfAbsent(m.getIndicatorCode(), m);
            }
        }
        if (latest.isEmpty()) {
            throw new ServiceException("该患者暂无监测数据，无法评分");
        }

        // 患者所患病种：以 cd_patient_disease 为准（多病共存）
        List<CdPatientDisease> patientDiseases = patientDiseaseMapper.selectList(
                new LambdaQueryWrapper<CdPatientDisease>().eq(CdPatientDisease::getPatientId, patientId));
        List<String> diseaseCodes = patientDiseases.stream()
                .map(CdPatientDisease::getDiseaseCode)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        if (diseaseCodes.isEmpty()) {
            // 兼容历史数据：患者-病种关联缺失时，退回监测记录上携带的病种
            diseaseCodes = latest.values().stream()
                    .map(CdMeasure::getDiseaseCode)
                    .filter(StringUtils::hasText)
                    .distinct()
                    .collect(Collectors.toList());
        }
        if (diseaseCodes.isEmpty()) {
            throw new ServiceException("该患者未登记病种，无法评分");
        }

        // 病种间权重：未配置时默认平分
        Map<String, Double> diseaseWeightMap = resolveDiseaseWeights(diseaseCodes);
        Map<String, Map<String, CdScoreWeight>> diseaseIndicatorWeights = new HashMap<>();
        for (String code : diseaseCodes) {
            Map<String, CdScoreWeight> map = new LinkedHashMap<>();
            weightMapper.selectList(new LambdaQueryWrapper<CdScoreWeight>()
                            .eq(CdScoreWeight::getDiseaseCode, code))
                    .forEach(w -> map.put(w.getIndicatorCode(), w));
            diseaseIndicatorWeights.put(code, map);
        }

        Map<String, Object> detail = new LinkedHashMap<>();
        Map<String, Object> diseaseDetails = new LinkedHashMap<>();
        double weightedSum = 0;
        for (String code : diseaseCodes) {
            double diseaseWeight = diseaseWeightMap.getOrDefault(code, 0.0);
            Map<String, Object> one = calcOneDisease(code, latest, diseaseIndicatorWeights.get(code));
            int diseaseScore = (Integer) one.get("diseaseScore");
            weightedSum += diseaseScore * diseaseWeight;
            diseaseDetails.put(code, one);
            // 明细按病种分组保存：多个病种共用同一批指标编码，扁平合并会互相覆盖，只留下最后一个病种
            detail.put(code, one.get("detail"));
        }

        int baseScore = (int) Math.round(weightedSum);
        baseScore = Math.max(0, Math.min(100, baseScore));

        // 预警规则加分：每个指标最近一次监测值命中中危规则 +10，命中高危规则 +20（低危不加分）
        List<Map<String, Object>> warningHits = new ArrayList<>();
        int warningBonus = 0;
        for (CdMeasure m : latest.values()) {
            CdWarningRule hit = warningService.matchRuleCode(m.getIndicatorCode(), m.getValue());
            if (hit == null || hit.getWarningLevel() == null
                || !("2".equals(hit.getWarningLevel()) || "3".equals(hit.getWarningLevel()))) {
                continue;
            }
            int bonus = "3".equals(hit.getWarningLevel()) ? 20 : 10;
            warningBonus += bonus;
            Map<String, Object> hitInfo = new LinkedHashMap<>();
            hitInfo.put("indicatorCode", m.getIndicatorCode());
            hitInfo.put("indicatorName", m.getIndicatorName());
            hitInfo.put("value", m.getValue());
            hitInfo.put("unit", m.getUnit());
            hitInfo.put("warningLevel", hit.getWarningLevel());
            hitInfo.put("warningLevelName", levelName(hit.getWarningLevel()));
            hitInfo.put("thresholdDesc", hit.getThresholdDesc());
            hitInfo.put("bonus", bonus);
            warningHits.add(hitInfo);
        }

        int totalScore = Math.max(0, Math.min(100, baseScore + warningBonus));
        String riskLevel = levelOf(totalScore);

        // 基础分与加分摘要也写入明细，便于历史记录回看评分构成
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("baseScore", baseScore);
        summary.put("warningBonus", warningBonus);
        summary.put("warningHits", warningHits);
        detail.put("_summary", summary);

        CdHealthScore score = new CdHealthScore();
        score.setPatientId(patientId);
        score.setTotalScore(totalScore);
        score.setRiskLevel(riskLevel);
        score.setDetailJson(toJson(detail));
        score.setTriggerType("3");
        score.setCalcTime(LocalDateTime.now());
        save(score);
        // 评分记录是风险等级的权威来源，同步回写患者档案，使档案页展示最近一次计算结果
        syncPatientRisk(patientId, totalScore, riskLevel);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scoreId", score.getScoreId());
        result.put("patientId", patientId);
        result.put("totalScore", totalScore);
        result.put("riskLevel", riskLevel);
        result.put("riskLevelName", levelName(riskLevel));
        result.put("detail", detail);
        result.put("diseases", diseaseDetails);
        result.put("baseScore", baseScore);
        result.put("warningBonus", warningBonus);
        result.put("warningHits", warningHits);
        result.put("calcTime", score.getCalcTime());
        return result;
    }

    /**
     * 病种间权重。
     * 约定：所有病种配置权重相等（默认 1/4）即为“平分”，此时按患者实际病种数各占 1/n；
     * 配置为不等权重（且覆盖该患者全部病种、合计为 1）时采用配置值。
     */
    private Map<String, Double> resolveDiseaseWeights(List<String> diseaseCodes) {
        Map<String, Double> result = new LinkedHashMap<>();
        if (diseaseCodes == null || diseaseCodes.isEmpty()) {
            return result;
        }
        Map<String, Double> configured = new HashMap<>();
        diseaseWeightMapper.selectList(new LambdaQueryWrapper<CdDiseaseWeight>())
                .forEach(w -> {
                    if (w.getWeight() != null && StringUtils.hasText(w.getDiseaseCode())) {
                        configured.put(w.getDiseaseCode(), w.getWeight().doubleValue());
                    }
                });

        double uniform = 1.0 / diseaseCodes.size();
        boolean configuredEqual = !configured.isEmpty();
        Double firstWeight = null;
        for (Double w : configured.values()) {
            if (firstWeight == null) {
                firstWeight = w;
            } else if (Math.abs(w - firstWeight) > 1e-6) {
                configuredEqual = false;
                break;
            }
        }
        if (configuredEqual) {
            diseaseCodes.forEach(code -> result.put(code, uniform));
            return result;
        }

        double sum = configured.values().stream().mapToDouble(Double::doubleValue).sum();
        boolean usable = Math.abs(sum - 1.0) <= 0.01
                && diseaseCodes.stream().allMatch(configured::containsKey);
        for (String code : diseaseCodes) {
            result.put(code, usable ? configured.get(code) : uniform);
        }
        return result;
    }

    /** 单个病种的评分：该病种权重命中的指标加权，无交集时退化为等权 */
    private Map<String, Object> calcOneDisease(String diseaseCode, Map<String, CdMeasure> latest,
                                               Map<String, CdScoreWeight> indicatorWeights) {
        double totalWeight = 0;
        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (CdScoreWeight sw : indicatorWeights.values()) {
            if (latest.containsKey(sw.getIndicatorCode())) {
                weightMap.put(sw.getIndicatorCode(), sw.getWeight() == null ? 0.0 : sw.getWeight().doubleValue());
                totalWeight += sw.getWeight() == null ? 0.0 : sw.getWeight().doubleValue();
            }
        }
        if (totalWeight <= 0) {
            for (String code : latest.keySet()) {
                weightMap.put(code, 1.0);
                totalWeight += 1.0;
            }
        }

        double weighted = 0;
        Map<String, Object> detail = new LinkedHashMap<>();
        for (Map.Entry<String, CdMeasure> e : latest.entrySet()) {
            String code = e.getKey();
            CdMeasure m = e.getValue();
            double value = m.getValue() == null ? 0 : m.getValue().doubleValue();
            double risk = indicatorRisk(code, value);
            double w = weightMap.getOrDefault(code, 0.0) / totalWeight;
            weighted += risk * w;
            Map<String, Object> d = new LinkedHashMap<>();
            d.put("indicatorCode", code);
            d.put("indicatorName", m.getIndicatorName());
            d.put("value", value);
            d.put("unit", m.getUnit());
            d.put("riskScore", Math.round(risk * 100.0) / 100.0);
            d.put("weight", Math.round(w * 10000.0) / 10000.0);
            d.put("diseaseCode", diseaseCode);
            d.put("diseaseName", DISEASE_NAME.getOrDefault(diseaseCode, diseaseCode));
            detail.put(code, d);
        }

        int diseaseScore = (int) Math.round(weighted * 100);
        diseaseScore = Math.max(0, Math.min(100, diseaseScore));
        Map<String, Object> one = new LinkedHashMap<>();
        one.put("diseaseCode", diseaseCode);
        one.put("diseaseName", DISEASE_NAME.getOrDefault(diseaseCode, diseaseCode));
        one.put("diseaseScore", diseaseScore);
        one.put("riskLevel", levelOf(diseaseScore));
        one.put("riskLevelName", levelName(levelOf(diseaseScore)));
        one.put("detail", detail);
        return one;
    }

    /** 将最新评分结果同步到患者档案（等级与分数一并覆盖，不再只升不降） */
    private void syncPatientRisk(Long patientId, int totalScore, String riskLevel) {
        CdPatient patient = new CdPatient();
        patient.setPatientId(patientId);
        patient.setRiskScore(totalScore);
        patient.setRiskLevel(riskLevel);
        patientMapper.updateById(patient);
    }

    @Override
    public List<CdScoreWeight> weightList(String diseaseCode) {
        return weightMapper.selectList(new LambdaQueryWrapper<CdScoreWeight>()
                .eq(StringUtils.hasText(diseaseCode), CdScoreWeight::getDiseaseCode, diseaseCode)
                .orderByAsc(CdScoreWeight::getDiseaseCode));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteScore(Long scoreId) {
        if (scoreId == null) {
            throw new ServiceException("评分记录ID不能为空");
        }
        // cd_health_score 无逻辑删除字段，此处直接物理删除
        int rows = getBaseMapper().deleteById(scoreId);
        if (rows == 0) {
            throw new ServiceException("评分记录不存在或已被删除");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteScores(List<Long> scoreIds) {
        if (scoreIds == null || scoreIds.isEmpty()) {
            throw new ServiceException("请选择要删除的评分记录");
        }
        List<Long> ids = scoreIds.stream().filter(java.util.Objects::nonNull).distinct().collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            throw new ServiceException("请选择要删除的评分记录");
        }
        // 无逻辑删除字段，物理删除
        return getBaseMapper().deleteByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveDiseaseWeights(ScoreWeightSaveDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getDiseaseCode())) {
            throw new ServiceException("病种不能为空");
        }
        if (dto.getItems() == null || dto.getItems().isEmpty()) {
            throw new ServiceException("请至少配置一个指标");
        }
        String diseaseCode = dto.getDiseaseCode().trim();
        Set<String> codes = new LinkedHashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreWeightSaveDTO.Item item : dto.getItems()) {
            if (item == null || !StringUtils.hasText(item.getIndicatorCode()) || !StringUtils.hasText(item.getIndicatorName())) {
                throw new ServiceException("指标名称与指标编码不能为空");
            }
            String code = item.getIndicatorCode().trim();
            if (!codes.add(code)) {
                throw new ServiceException("指标编码[" + code + "]重复");
            }
            BigDecimal w = item.getWeight();
            if (w == null) {
                throw new ServiceException("指标[" + item.getIndicatorName().trim() + "]的权重不能为空");
            }
            if (w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(BigDecimal.ONE) > 0) {
                throw new ServiceException("指标[" + item.getIndicatorName().trim() + "]的权重必须在0~1之间");
            }
            total = total.add(w);
        }
        if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.0001")) > 0) {
            throw new ServiceException("该病种指标权重之和必须为1，当前为" + total.stripTrailingZeros().toPlainString());
        }

        // 指标必须存在于系统指标库（系统管理 → 指标管理），名称以指标库为准
        List<SysIndicator> indicatorList = indicatorMapper.selectList(new LambdaQueryWrapper<SysIndicator>()
                .in(SysIndicator::getIndicatorCode, codes));
        Map<String, SysIndicator> indicatorMap = indicatorList.stream()
                .collect(Collectors.toMap(SysIndicator::getIndicatorCode, i -> i, (a, b) -> a));
        for (String code : codes) {
            if (!indicatorMap.containsKey(code)) {
                throw new ServiceException("指标[" + code + "]不在系统指标库中，请先在指标管理中维护");
            }
        }

        weightMapper.delete(new LambdaQueryWrapper<CdScoreWeight>()
                .eq(CdScoreWeight::getDiseaseCode, diseaseCode));
        for (ScoreWeightSaveDTO.Item item : dto.getItems()) {
            String code = item.getIndicatorCode().trim();
            CdScoreWeight weight = new CdScoreWeight();
            weight.setDiseaseCode(diseaseCode);
            weight.setIndicatorCode(code);
            weight.setIndicatorName(indicatorMap.get(code).getIndicatorName());
            weight.setWeight(item.getWeight().setScale(2, RoundingMode.HALF_UP));
            weightMapper.insert(weight);
        }
    }

    @Override
    public List<Map<String, Object>> diseaseWeightList(List<String> diseaseCodes) {
        List<String> codes = (diseaseCodes == null ? Collections.<String>emptyList() : diseaseCodes).stream()
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
        Map<String, Double> weights = resolveDiseaseWeights(codes);
        List<Map<String, Object>> result = new ArrayList<>();
        for (String code : codes) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("diseaseCode", code);
            m.put("diseaseName", DISEASE_NAME.getOrDefault(code, code));
            m.put("weight", Math.round(weights.getOrDefault(code, 0.0) * 100.0) / 100.0);
            result.add(m);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveCombinedDiseaseWeights(List<ScoreWeightSaveDTO.DiseaseItem> items) {
        if (items == null || items.isEmpty()) {
            throw new ServiceException("请至少配置一个病种");
        }
        Set<String> codes = new LinkedHashSet<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ScoreWeightSaveDTO.DiseaseItem item : items) {
            if (item == null || !StringUtils.hasText(item.getDiseaseCode())) {
                throw new ServiceException("病种不能为空");
            }
            String code = item.getDiseaseCode().trim();
            if (!codes.add(code)) {
                throw new ServiceException("病种[" + code + "]重复");
            }
            if (item.getWeight() == null) {
                throw new ServiceException("病种[" + DISEASE_NAME.getOrDefault(code, code) + "]的权重不能为空");
            }
            if (item.getWeight().compareTo(BigDecimal.ZERO) < 0 || item.getWeight().compareTo(BigDecimal.ONE) > 0) {
                throw new ServiceException("病种[" + DISEASE_NAME.getOrDefault(code, code) + "]的权重必须在0~1之间");
            }
            total = total.add(item.getWeight());
        }
        if (total.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.0001")) > 0) {
            throw new ServiceException("病种权重之和必须为1，当前为" + total.stripTrailingZeros().toPlainString());
        }

        diseaseWeightMapper.delete(new LambdaQueryWrapper<CdDiseaseWeight>()
                .in(CdDiseaseWeight::getDiseaseCode, codes));
        for (ScoreWeightSaveDTO.DiseaseItem item : items) {
            CdDiseaseWeight weight = new CdDiseaseWeight();
            weight.setDiseaseCode(item.getDiseaseCode().trim());
            weight.setDiseaseName(DISEASE_NAME.getOrDefault(item.getDiseaseCode().trim(), "未知"));
            weight.setWeight(item.getWeight().setScale(2, RoundingMode.HALF_UP));
            diseaseWeightMapper.insert(weight);
        }
    }
    @Override
    public Map<String, Object> trend(Long patientId) {
        List<CdHealthScore> list = list(new LambdaQueryWrapper<CdHealthScore>()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByAsc(CdHealthScore::getCalcTime)
                .last("limit 30"));
        List<String> dates = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (CdHealthScore s : list) {
            dates.add(s.getCalcTime() == null ? "" : s.getCalcTime().toLocalDate().toString());
            scores.add(s.getTotalScore());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("scores", scores);
        return result;
    }

    /** 单指标风险度 0~1 */
    private double indicatorRisk(String code, double value) {
        double[] r = RANGE.get(code);
        if (r == null) {
            return 0.5;
        }
        double low = r[0], high = r[1], dangerLow = r[2], dangerHigh = r[3];
        if (value >= low && value <= high) {
            return 0.0;
        }
        if (value < low) {
            double span = Math.max(1e-6, low - dangerLow);
            return Math.min(1.0, (low - value) / span);
        }
        double span = Math.max(1e-6, dangerHigh - high);
        return Math.min(1.0, (value - high) / span);
    }

    private String levelOf(int score) {
        return score >= 80 ? "3" : (score >= 50 ? "2" : "1");
    }

    private String levelName(String level) {
        switch (level) {
            case "3": return "高危";
            case "2": return "中危";
            default: return "低危";
        }
    }

    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) {
                sb.append(",");
            }
            first = false;
            sb.append("\"").append(escapeJson(e.getKey())).append("\":").append(jsonValue(e.getValue()));
        }
        return sb.append("}").toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    @SuppressWarnings("unchecked")
    private String jsonValue(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map) {
            return toJson((Map<String, Object>) v);
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return "\"" + escapeJson(v.toString()) + "\"";
    }
}
