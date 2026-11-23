package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.PatientScoreWeightDTO;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdPatientScoreWeight;
import com.chronic.modules.chronic.mapper.CdHealthScoreMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdPatientScoreWeightMapper;
import com.chronic.modules.chronic.query.ScoreQuery;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.support.HealthScoreCalculator;
import com.chronic.modules.chronic.support.HealthScoreResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 健康评分：近 7 天窗口内按 w1~w4 加权合成，分数越高越健康
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class CdScoreServiceImpl extends ServiceImpl<CdHealthScoreMapper, CdHealthScore> implements CdScoreService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final CdPatientMapper patientMapper;

    private final CdPatientScoreWeightMapper patientWeightMapper;

    private final HealthScoreCalculator calculator;

    @Override
    public PageResult<CdHealthScore> queryPage(ScoreQuery query) {
        return pageAll(query);
    }

    @Override
    public PageResult<CdHealthScore> pageAll(ScoreQuery query) {
        LambdaQueryWrapper<CdHealthScore> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdHealthScore::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getTriggerType()), CdHealthScore::getTriggerType, query.getTriggerType())
         .orderByDesc(CdHealthScore::getCalcTime);
        Page<CdHealthScore> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        fillPatientName(page.getRecords());
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 填充患者姓名 */
    private void fillPatientName(List<CdHealthScore> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        list.forEach(s -> {
            if (s.getPatientId() != null) {
                ids.add(s.getPatientId());
            }
        });
        if (ids.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = new java.util.HashMap<>();
        patientMapper.selectBatchIds(ids).forEach(p -> nameMap.put(p.getPatientId(), p.getName()));
        list.forEach(s -> s.setPatientName(nameMap.get(s.getPatientId())));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> calcScore(Long patientId) {
        if (patientId == null) {
            throw new ServiceException("患者ID不能为空");
        }
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }

        HealthScoreResult result = calculator.calc(patientId);

        CdHealthScore score = new CdHealthScore();
        score.setPatientId(patientId);
        score.setHealthScore(result.getHealthScore());
        score.setDetailJson(result.getDetailJson());
        score.setTriggerType("3");
        score.setCalcTime(LocalDateTime.now());
        save(score);

        // 评分记录是健康分的权威来源，同步回写患者档案供列表与小程序展示
        CdPatient update = new CdPatient();
        update.setPatientId(patientId);
        update.setHealthScore(result.getHealthScore());
        patientMapper.updateById(update);

        return toView(result, score);
    }

    private Map<String, Object> toView(HealthScoreResult result, CdHealthScore score) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("scoreId", score.getScoreId());
        m.put("patientId", result.getPatientId());
        m.put("healthScore", result.getHealthScore());
        m.put("healthLevel", healthLevel(result.getHealthScore()));
        m.put("healthLevelName", healthLevelName(healthLevel(result.getHealthScore())));
        m.put("dailyScore", result.getDailyScore());
        m.put("windowStart", result.getWindowStart());
        m.put("windowEnd", result.getWindowEnd());
        m.put("weights", result.getWeights());
        m.put("indicatorRate", result.getIndicatorRate());
        m.put("indicatorMeasured", result.getIndicatorMeasured());
        m.put("indicatorTotal", result.getIndicatorTotal());
        m.put("indicatorItems", result.getIndicatorItems());
        m.put("indicatorBatches", result.getIndicatorBatches());
        m.put("indicatorBatchItems", result.getIndicatorBatchItems());
        m.put("medicationRate", result.getMedicationRate());
        m.put("medicationOnTime", result.getMedicationOnTime());
        m.put("medicationLate", result.getMedicationLate());
        m.put("medicationMissed", result.getMedicationMissed());
        m.put("medicationPending", result.getMedicationPending());
        m.put("alertDensity", result.getAlertDensity());
        m.put("alertDays", result.getAlertDays());
        m.put("measuredDays", result.getMeasuredDays());
        m.put("monitorRate", result.getMonitorRate());
        m.put("monitorMeasuredTimes", result.getMonitorMeasuredTimes());
        m.put("monitorTargetTimes", result.getMonitorTargetTimes());
        m.put("calcTime", score.getCalcTime());
        return m;
    }

    @Override
    public Map<String, Object> patientWeight(Long patientId) {
        if (patientId == null) {
            throw new ServiceException("患者ID不能为空");
        }
        CdPatientScoreWeight weight = calculator.resolveWeight(patientId);
        boolean configured = weight.getUpdateTime() != null;
        return weightView(weight, configured);
    }

    private Map<String, Object> weightView(CdPatientScoreWeight weight, boolean configured) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("patientId", weight.getPatientId());
        m.put("wRisk", weight.getWRisk());
        m.put("wMed", weight.getWMed());
        m.put("wAlert", weight.getWAlert());
        m.put("wMonitor", weight.getWMonitor());
        m.put("configured", configured);
        m.put("updateTime", weight.getUpdateTime());
        return m;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePatientWeight(PatientScoreWeightDTO dto) {
        if (dto == null || dto.getPatientId() == null) {
            throw new ServiceException("患者ID不能为空");
        }
        if (patientMapper.selectById(dto.getPatientId()) == null) {
            throw new ServiceException("患者不存在");
        }
        List<BigDecimal> weights = List.of(dto.getWRisk(), dto.getWMed(), dto.getWAlert(), dto.getWMonitor());
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal w : weights) {
            if (w == null) {
                throw new ServiceException("权重不能为空");
            }
            if (w.compareTo(BigDecimal.ZERO) < 0 || w.compareTo(ONE) > 0) {
                throw new ServiceException("权重必须在 0~1 之间");
            }
            total = total.add(w);
        }
        if (total.subtract(ONE).abs().compareTo(new BigDecimal("0.0001")) > 0) {
            throw new ServiceException("四项权重之和必须为 1，当前为 " + total.stripTrailingZeros().toPlainString());
        }

        CdPatientScoreWeight weight = new CdPatientScoreWeight();
        weight.setPatientId(dto.getPatientId());
        weight.setWRisk(dto.getWRisk().setScale(2, java.math.RoundingMode.HALF_UP));
        weight.setWMed(dto.getWMed().setScale(2, java.math.RoundingMode.HALF_UP));
        weight.setWAlert(dto.getWAlert().setScale(2, java.math.RoundingMode.HALF_UP));
        weight.setWMonitor(dto.getWMonitor().setScale(2, java.math.RoundingMode.HALF_UP));
        weight.setUpdateTime(LocalDateTime.now());
        if (patientWeightMapper.selectById(dto.getPatientId()) == null) {
            patientWeightMapper.insert(weight);
        } else {
            patientWeightMapper.updateById(weight);
        }

        // 权重变化后立即按新权重重算一次，避免页面展示与配置不一致
        calcScore(dto.getPatientId());
    }

    @Override
    public void deleteScore(Long scoreId) {
        if (scoreId == null) {
            throw new ServiceException("评分记录ID不能为空");
        }
        if (getById(scoreId) == null) {
            throw new ServiceException("评分记录不存在");
        }
        // cd_health_score 无逻辑删除字段，此处直接物理删除
        getBaseMapper().deleteById(scoreId);
    }

    @Override
    public int deleteScores(List<Long> scoreIds) {
        if (scoreIds == null || scoreIds.isEmpty()) {
            throw new ServiceException("请选择要删除的评分记录");
        }
        List<Long> ids = scoreIds.stream().filter(java.util.Objects::nonNull).distinct()
                .collect(java.util.stream.Collectors.toList());
        if (ids.isEmpty()) {
            throw new ServiceException("请选择要删除的评分记录");
        }
        return getBaseMapper().deleteByIds(ids);
    }

    @Override
    public Map<String, Object> trend(Long patientId) {
        List<CdHealthScore> list = list(new LambdaQueryWrapper<CdHealthScore>()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .last("limit 30"));
        java.util.Collections.reverse(list);
        List<String> dates = new ArrayList<>();
        List<Integer> scores = new ArrayList<>();
        for (CdHealthScore s : list) {
            dates.add(s.getCalcTime() == null ? "" : s.getCalcTime().toLocalDate().toString());
            scores.add(s.getHealthScore() == null ? 0 : s.getHealthScore());
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("scores", scores);
        return result;
    }

    public static String healthLevel(Integer score) {
        int s = score == null ? 0 : score;
        return s >= 80 ? "3" : (s >= 50 ? "2" : "1");
    }

    public static String healthLevelName(String level) {
        switch (level) {
            case "3":
                return "健康良好";
            case "2":
                return "一般";
            default:
                return "较差";
        }
    }
}