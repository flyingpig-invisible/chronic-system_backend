package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.EmergencyLinkDTO;
import com.chronic.modules.chronic.dto.WarningHandleDTO;
import com.chronic.modules.chronic.entity.*;
import com.chronic.modules.chronic.mapper.*;
import com.chronic.modules.chronic.query.WarningQuery;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 预警服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdWarningServiceImpl implements CdWarningService {

    private final CdWarningMapper warningMapper;
    private final CdWarningRuleMapper ruleMapper;
    private final CdEmergencyLinkMapper emergencyLinkMapper;
    private final CdPatientMapper patientMapper;
    private final CdMeasureMapper measureMapper;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<CdWarning> pageWarning(WarningQuery query) {
        Page<CdWarning> page = warningMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), buildWrapper(query));
        return PageResult.of(page);
    }

    @Override
    public CdWarning getWarning(Long warningId) {
        CdWarning warning = warningMapper.selectById(warningId);
        if (warning == null) {
            throw new ServiceException("预警不存在");
        }
        // 补充处置建议
        if (warning.getRuleId() != null) {
            CdWarningRule rule = ruleMapper.selectById(warning.getRuleId());
            if (rule != null) {
                warning.setActionDesc(rule.getActionDesc());
            }
        }
        return warning;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleWarning(Long warningId, WarningHandleDTO dto) {
        CdWarning warning = warningMapper.selectById(warningId);
        if (warning == null) {
            throw new ServiceException("预警不存在");
        }

        CdWarning update = new CdWarning();
        update.setWarningId(warningId);
        update.setHandleStatus(dto.getHandleStatus());
        update.setHandleType(dto.getHandleType());
        update.setHandleContent(dto.getHandleContent());
        update.setHandleBy(SecurityUtils.getUsername());
        update.setHandleTime(LocalDateTime.now());
        warningMapper.updateById(update);

        // 联动急救
        if (Boolean.TRUE.equals(dto.getLinkEmergency())) {
            EmergencyLinkDTO linkDTO = new EmergencyLinkDTO();
            linkDTO.setChannel("120");
            linkDTO.setReceiver("急救中心");
            linkDTO.setRemark(dto.getRemark());
            linkEmergency(warningId, linkDTO);
        }

        // 已处理则减少患者待处理预警数
        if ("2".equals(dto.getHandleStatus())) {
            CdPatient patient = patientMapper.selectById(warning.getPatientId());
            if (patient != null) {
                CdPatient pu = new CdPatient();
                pu.setPatientId(patient.getPatientId());
                int count = patient.getPendingWarningCount() == null ? 0 : patient.getPendingWarningCount();
                pu.setPendingWarningCount(Math.max(0, count - 1));
                patientMapper.updateById(pu);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void linkEmergency(Long warningId, EmergencyLinkDTO dto) {
        CdWarning warning = warningMapper.selectById(warningId);
        if (warning == null) {
            throw new ServiceException("预警不存在");
        }

        CdEmergencyLink link = new CdEmergencyLink();
        link.setWarningId(warningId);
        link.setPatientId(warning.getPatientId());
        link.setPatientName(warning.getPatientName());
        link.setChannel(dto.getChannel());
        link.setReceiver(dto.getReceiver());
        link.setLinkResult(dto.getLinkResult());
        link.setLinkBy(SecurityUtils.getUsername());
        link.setLinkTime(LocalDateTime.now());
        link.setRemark(dto.getRemark());
        emergencyLinkMapper.insert(link);

        CdWarning update = new CdWarning();
        update.setWarningId(warningId);
        update.setEmergencyLinked("1");
        warningMapper.updateById(update);

        log.info("预警 {} 已联动急救：{} -> {}", warningId, dto.getChannel(), dto.getReceiver());
    }

    @Override
    public List<CdEmergencyLink> listEmergencyByWarning(Long warningId) {
        return emergencyLinkMapper.selectList(Wrappers.<CdEmergencyLink>lambdaQuery()
                .eq(CdEmergencyLink::getWarningId, warningId)
                .orderByDesc(CdEmergencyLink::getLinkTime));
    }

    @Override
    public PageResult<CdEmergencyLink> pageEmergency(WarningQuery query) {
        Page<CdEmergencyLink> page = emergencyLinkMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()),
                Wrappers.<CdEmergencyLink>lambdaQuery()
                        .like(StringUtils.hasText(query.getPatientName()), CdEmergencyLink::getPatientName, query.getPatientName())
                        .orderByDesc(CdEmergencyLink::getLinkTime));
        return PageResult.of(page);
    }

    @Override
    public List<Map<String, Object>> statByLevel(WarningQuery query) {
        List<CdWarning> list = warningMapper.selectList(buildWrapper(query));
        Map<String, String> levelName = Map.of("1", "低危", "2", "中危", "3", "高危");
        List<Map<String, Object>> result = new ArrayList<>();
        for (String level : new String[]{"3", "2", "1"}) {
            long count = list.stream().filter(w -> level.equals(w.getWarningLevel())).count();
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("riskLevel", level);
            item.put("name", levelName.get(level));
            item.put("count", count);
            item.put("ratio", list.isEmpty() ? 0 : Math.round(count * 10000.0 / list.size()) / 100.0);
            result.add(item);
        }
        return result;
    }

    @Override
    public Map<String, Object> statResponse(WarningQuery query) {
        List<CdWarning> list = warningMapper.selectList(buildWrapper(query));
        long total = list.size();
        long handled = list.stream().filter(w -> "2".equals(w.getHandleStatus())).count();
        long pending = list.stream().filter(w -> "0".equals(w.getHandleStatus())).count();

        double avgMinutes = list.stream()
                .filter(w -> w.getHandleTime() != null && w.getTriggerTime() != null)
                .mapToLong(w -> java.time.Duration.between(w.getTriggerTime(), w.getHandleTime()).toMinutes())
                .average().orElse(0);

        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("totalCount", total);
        stat.put("handledCount", handled);
        stat.put("pendingCount", pending);
        stat.put("avgResponseMinutes", Math.round(avgMinutes * 100.0) / 100.0);
        stat.put("handleRate", total == 0 ? 0 : Math.round(handled * 10000.0 / total) / 100.0);
        return stat;
    }

    @Override
    public List<CdWarning> listForExport(WarningQuery query) {
        query.setPageNum(1);
        query.setPageSize(10000);
        return warningMapper.selectList(buildWrapper(query));
    }

    @Override
    public void checkAndCreateWarning(Long patientId, String patientName, String indicatorCode,
                                      BigDecimal value, String unit, Long measureId) {
        if (value == null) {
            return;
        }
        CdWarningRule matched = matchRuleCode(indicatorCode, value);
        if (matched == null) {
            return;
        }

        CdWarning warning = new CdWarning();
        warning.setPatientId(patientId);
        warning.setPatientName(patientName);
        warning.setDiseaseCode(matched.getDiseaseCode());
        warning.setIndicatorCode(indicatorCode);
        warning.setIndicatorName(matched.getIndicatorName());
        warning.setIndicatorValue(value);
        warning.setUnit(unit);
        warning.setThresholdDesc(matched.getThresholdDesc());
        warning.setRuleId(matched.getRuleId());
        warning.setWarningLevel(matched.getWarningLevel());
        warning.setHandleStatus("0");
        warning.setEmergencyLinked("0");
        warning.setTriggerTime(LocalDateTime.now());
        warningMapper.insert(warning);

        // 关联监测记录
        if (measureId != null) {
            CdMeasure m = new CdMeasure();
            m.setMeasureId(measureId);
            m.setAbnormalFlag("1");
            measureMapper.updateById(m);
        }

        // 更新患者待处理预警数；风险评分/等级统一由评分计算按预警规则得出，此处不再抬高
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient != null) {
            CdPatient pu = new CdPatient();
            pu.setPatientId(patientId);
            pu.setPendingWarningCount((patient.getPendingWarningCount() == null ? 0 : patient.getPendingWarningCount()) + 1);
            patientMapper.updateById(pu);
        }

        log.info("患者[{}] 指标[{}={}] 触发 {} 预警", patientName, indicatorCode, value,
                matched.getWarningLevel());
    }

    @Override
    public CdWarningRule matchRuleCode(String indicatorCode, BigDecimal value) {
        if (!StringUtils.hasText(indicatorCode) || value == null) {
            return null;
        }
        List<CdWarningRule> rules = ruleMapper.selectList(Wrappers.<CdWarningRule>lambdaQuery()
                .eq(CdWarningRule::getIndicatorCode, indicatorCode)
                .eq(CdWarningRule::getStatus, "0"));
        CdWarningRule matched = null;
        for (CdWarningRule rule : rules) {
            if (matchRule(rule, value)) {
                if (matched == null || rule.getWarningLevel().compareTo(matched.getWarningLevel()) > 0) {
                    matched = rule;
                }
            }
        }
        return matched;
    }

    /** 判断是否命中规则 */
    private boolean matchRule(CdWarningRule rule, BigDecimal value) {
        BigDecimal threshold = rule.getThresholdValue();
        if (threshold == null) {
            return false;
        }
        int cmp = value.compareTo(threshold);
        return switch (rule.getOperator()) {
            case ">=" -> cmp >= 0;
            case ">" -> cmp > 0;
            case "<=" -> cmp <= 0;
            case "<" -> cmp < 0;
            case "=" -> cmp == 0;
            default -> false;
        };
    }

    private LambdaQueryWrapper<CdWarning> buildWrapper(WarningQuery query) {
        LambdaQueryWrapper<CdWarning> wrapper = Wrappers.<CdWarning>lambdaQuery()
                .eq(query.getPatientId() != null, CdWarning::getPatientId, query.getPatientId())
                .like(StringUtils.hasText(query.getPatientName()), CdWarning::getPatientName, query.getPatientName())
                .eq(StringUtils.hasText(query.getDiseaseCode()), CdWarning::getDiseaseCode, query.getDiseaseCode())
                .eq(StringUtils.hasText(query.getWarningLevel()), CdWarning::getWarningLevel, query.getWarningLevel())
                .eq(StringUtils.hasText(query.getHandleStatus()), CdWarning::getHandleStatus, query.getHandleStatus())
                .eq(StringUtils.hasText(query.getIndicatorCode()), CdWarning::getIndicatorCode, query.getIndicatorCode())
                .ge(StringUtils.hasText(query.getBeginTime()), CdWarning::getTriggerTime,
                        StringUtils.hasText(query.getBeginTime()) ? LocalDateTime.parse(query.getBeginTime(), DTF) : null)
                .le(StringUtils.hasText(query.getEndTime()), CdWarning::getTriggerTime,
                        StringUtils.hasText(query.getEndTime()) ? LocalDateTime.parse(query.getEndTime(), DTF) : null);
        // 高危优先
        return wrapper.orderByAsc(CdWarning::getHandleStatus)
                .orderByDesc(CdWarning::getWarningLevel)
                .orderByDesc(CdWarning::getTriggerTime);
    }
}
