package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ScoreWeightSaveDTO;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdScoreWeight;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.mapper.CdHealthScoreMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdScoreWeightMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import com.chronic.modules.chronic.query.ScoreQuery;
import com.chronic.modules.chronic.service.CdScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

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
        // 取每个指标最近一次监测值
        List<CdMeasure> measures = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime));
        Map<String, CdMeasure> latest = new LinkedHashMap<>();
        for (CdMeasure m : measures) {
            latest.putIfAbsent(m.getIndicatorCode(), m);
        }
        if (latest.isEmpty()) {
            throw new ServiceException("该患者暂无监测数据，无法评分");
        }

        String diseaseCode = latest.values().iterator().next().getDiseaseCode();
        List<CdScoreWeight> weights = weightMapper.selectList(new LambdaQueryWrapper<CdScoreWeight>()
                .eq(StringUtils.hasText(diseaseCode), CdScoreWeight::getDiseaseCode, diseaseCode));

        double totalWeight = 0;
        Map<String, Double> weightMap = new LinkedHashMap<>();
        for (CdScoreWeight sw : weights) {
            if (latest.containsKey(sw.getIndicatorCode())) {
                weightMap.put(sw.getIndicatorCode(), sw.getWeight().doubleValue());
                totalWeight += sw.getWeight().doubleValue();
            }
        }
        if (totalWeight <= 0) {
            // 无权重配置时各指标等权
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
            detail.put(code, d);
        }

        int totalScore = (int) Math.round(weighted * 100);
        totalScore = Math.max(0, Math.min(100, totalScore));
        String riskLevel = totalScore >= 80 ? "3" : (totalScore >= 50 ? "2" : "1");

        CdHealthScore score = new CdHealthScore();
        score.setPatientId(patientId);
        score.setTotalScore(totalScore);
        score.setRiskLevel(riskLevel);
        score.setDetailJson(toJson(detail));
        score.setTriggerType("3");
        score.setCalcTime(LocalDateTime.now());
        save(score);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("scoreId", score.getScoreId());
        result.put("patientId", patientId);
        result.put("totalScore", totalScore);
        result.put("riskLevel", riskLevel);
        result.put("riskLevelName", levelName(riskLevel));
        result.put("detail", detail);
        result.put("calcTime", score.getCalcTime());
        return result;
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

        weightMapper.delete(new LambdaQueryWrapper<CdScoreWeight>()
                .eq(CdScoreWeight::getDiseaseCode, diseaseCode));
        for (ScoreWeightSaveDTO.Item item : dto.getItems()) {
            CdScoreWeight weight = new CdScoreWeight();
            weight.setDiseaseCode(diseaseCode);
            weight.setIndicatorCode(item.getIndicatorCode().trim());
            weight.setIndicatorName(item.getIndicatorName().trim());
            weight.setWeight(item.getWeight().setScale(2, RoundingMode.HALF_UP));
            weightMapper.insert(weight);
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
            sb.append("\"").append(e.getKey()).append("\":").append(jsonValue(e.getValue()));
        }
        return sb.append("}").toString();
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
        return "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\"";
    }
}
