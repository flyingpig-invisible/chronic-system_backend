package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdWarningRuleMapper;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.modules.miniapp.dto.MpMeasureDTO;
import com.chronic.modules.miniapp.service.MpDataService;
import com.chronic.modules.miniapp.service.MpMeasureService;
import com.chronic.modules.miniapp.vo.MpMeasureVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序端监测数据录入实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpMeasureServiceImpl implements MpMeasureService {

    private final MpDataService dataService;
    private final CdMeasureMapper measureMapper;
    private final CdPatientMapper patientMapper;
    private final CdWarningRuleMapper ruleMapper;
    private final CdWarningService warningService;
    private final CdScoreService scoreService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 可录入的指标：编码 -> {名称, 单位}，顺序即前端展示顺序 */
    public static final Map<String, String[]> INDICATORS = new LinkedHashMap<>();

    static {
        INDICATORS.put("spo2", new String[]{"血氧", "%"});
        INDICATORS.put("glu", new String[]{"血糖", "mmol/L"});
        INDICATORS.put("sbp", new String[]{"收缩压", "mmHg"});
        INDICATORS.put("dbp", new String[]{"舒张压", "mmHg"});
        INDICATORS.put("hr", new String[]{"心率", "次/分"});
        INDICATORS.put("rr", new String[]{"呼吸频率", "次/分"});
        INDICATORS.put("sleep", new String[]{"睡眠时长", "小时"});
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MpMeasureVO submit(MpMeasureDTO dto) {
        if (dto.getPatientId() == null) {
            throw new ServiceException("患者ID不能为空");
        }
        // 权限：患者本人或已绑定家属
        dataService.checkPermission(dto.getPatientId());

        CdPatient patient = patientMapper.selectById(dto.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }

        LocalDateTime measureTime = resolveTime(dto.getMeasureTime());
        Map<String, BigDecimal> values = collect(dto);

        MpMeasureVO result = new MpMeasureVO();
        if (values.isEmpty()) {
            result.setSkippedCount(INDICATORS.size());
            return result;
        }

        List<MpMeasureVO.Item> items = new ArrayList<>();
        int maxLevel = 0;
        boolean triggered = false;

        for (Map.Entry<String, String[]> meta : INDICATORS.entrySet()) {
            String code = meta.getKey();
            BigDecimal value = values.get(code);
            if (value == null) {
                continue;
            }
            String name = meta.getValue()[0];
            String unit = meta.getValue()[1];

            CdMeasure measure = new CdMeasure();
            measure.setPatientId(patient.getPatientId());
            measure.setPatientName(patient.getName());
            measure.setIndicatorCode(code);
            measure.setIndicatorName(name);
            measure.setValue(value);
            measure.setUnit(unit);
            measure.setMeasureTime(measureTime);
            measure.setDataSource("2");
            measure.setAbnormalFlag("0");
            measureMapper.insert(measure);

            // 预警判定：内部会回写 abnormalFlag、累加待处理预警数、必要时抬升风险等级
            warningService.checkAndCreateWarning(patient.getPatientId(), patient.getName(),
                    code, value, unit, measure.getMeasureId());

            CdMeasure saved = measureMapper.selectById(measure.getMeasureId());
            CdWarningRule matched = matchRule(code, value);

            MpMeasureVO.Item item = new MpMeasureVO.Item();
            item.setMeasureId(saved.getMeasureId());
            item.setIndicatorCode(code);
            item.setIndicatorName(name);
            item.setValue(value.stripTrailingZeros().toPlainString());
            item.setUnit(unit);
            item.setAbnormalFlag(saved == null ? "0" : saved.getAbnormalFlag());
            if (matched != null) {
                item.setWarningTriggered(true);
                item.setWarningLevel(matched.getWarningLevel());
                item.setWarningTip(matched.getThresholdDesc());
                triggered = true;
                if (matched.getWarningLevel() != null) {
                    maxLevel = Math.max(maxLevel, Integer.parseInt(matched.getWarningLevel()));
                }
            }
            items.add(item);
        }

        // 更新患者最近监测时间
        CdPatient pu = new CdPatient();
        pu.setPatientId(patient.getPatientId());
        pu.setLastMeasureTime(measureTime);
        patientMapper.updateById(pu);

        // 上报后按最新监测数据 + 预警规则重算评分，并同步回写患者风险等级
        try {
            scoreService.calcScore(patient.getPatientId());
        } catch (Exception e) {
            log.warn("患者[{}] 小程序上报后重算评分失败：{}", patient.getPatientId(), e.getMessage());
        }

        result.setSavedCount(items.size());
        result.setSkippedCount(INDICATORS.size() - items.size());
        result.setItems(items);
        result.setWarningTriggered(triggered);
        result.setMaxWarningLevel(maxLevel == 0 ? null : String.valueOf(maxLevel));
        log.info("小程序录入监测：患者[{}] 共 {} 项，触发预警={}", patient.getName(), items.size(), triggered);
        return result;
    }

    /** 只收集非空且合法的指标值 */
    private Map<String, BigDecimal> collect(MpMeasureDTO dto) {
        Map<String, BigDecimal> map = new LinkedHashMap<>();
        put(map, "spo2", dto.getSpo2());
        put(map, "glu", dto.getGlu());
        put(map, "sbp", dto.getSbp());
        put(map, "dbp", dto.getDbp());
        put(map, "hr", dto.getHr());
        put(map, "rr", dto.getRr());
        put(map, "sleep", dto.getSleep());
        return map;
    }

    private void put(Map<String, BigDecimal> map, String code, BigDecimal value) {
        if (value == null) {
            return;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException(INDICATORS.get(code)[0] + " 不能为负数");
        }
        if (value.compareTo(BigDecimal.ZERO) == 0) {
            // 0 视为未填写
            return;
        }
        map.put(code, value);
    }

    /** 复现预警规则匹配，取最严重的一条 */
    private CdWarningRule matchRule(String indicatorCode, BigDecimal value) {
        List<CdWarningRule> rules = ruleMapper.selectList(new LambdaQueryWrapper<CdWarningRule>()
                .eq(CdWarningRule::getIndicatorCode, indicatorCode)
                .eq(CdWarningRule::getStatus, "0"));
        CdWarningRule matched = null;
        for (CdWarningRule rule : rules) {
            if (rule.getThresholdValue() == null) {
                continue;
            }
            int cmp = value.compareTo(rule.getThresholdValue());
            boolean hit = switch (rule.getOperator() == null ? "" : rule.getOperator()) {
                case ">=" -> cmp >= 0;
                case ">" -> cmp > 0;
                case "<=" -> cmp <= 0;
                case "<" -> cmp < 0;
                case "=" -> cmp == 0;
                default -> false;
            };
            if (hit && (matched == null
                    || rule.getWarningLevel().compareTo(matched.getWarningLevel()) > 0)) {
                matched = rule;
            }
        }
        return matched;
    }

    private LocalDateTime resolveTime(String text) {
        if (!StringUtils.hasText(text)) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(text.replace('T', ' '), DTF);
        } catch (Exception e) {
            log.warn("测量时间解析失败，使用当前时间：{}", text);
            return LocalDateTime.now();
        }
    }
}
