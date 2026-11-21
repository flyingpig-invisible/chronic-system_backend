package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.entity.CdPlan;
import com.chronic.modules.chronic.entity.CdPlanItem;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdRemindLog;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdGuideMapper;
import com.chronic.modules.chronic.mapper.CdPlanItemMapper;
import com.chronic.modules.chronic.mapper.CdPlanMapper;
import com.chronic.modules.chronic.mapper.CdRemindLogMapper;
import com.chronic.modules.chronic.mapper.CdRemindMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import com.chronic.modules.chronic.mapper.CdWarningRuleMapper;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpCareService;
import com.chronic.modules.miniapp.vo.MpGuideVO;
import com.chronic.modules.miniapp.vo.MpMedicationVO;
import com.chronic.modules.miniapp.vo.MpPlanItemVO;
import com.chronic.modules.miniapp.vo.MpPlanVO;
import com.chronic.modules.miniapp.vo.MpWarningVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 预警 / 指导 / 计划 / 用药服务实现
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class MpCareServiceImpl implements MpCareService {

    private final MpAccessService accessService;
    private final CdWarningMapper warningMapper;
    private final CdWarningRuleMapper warningRuleMapper;
    private final CdGuideMapper guideMapper;
    private final CdPlanMapper planMapper;
    private final CdPlanItemMapper planItemMapper;
    private final CdRemindMapper remindMapper;
    private final CdRemindLogMapper remindLogMapper;

    private static final Map<String, String> LEVEL_NAME = Map.of("1", "低危", "2", "中危", "3", "高危");
    private static final Map<String, String> HANDLE_NAME = Map.of("0", "待处理", "1", "处理中", "2", "已处理");
    private static final Map<String, String> GUIDE_TYPE_NAME = new LinkedHashMap<>();
    private static final Map<String, String> ITEM_TYPE_NAME = new LinkedHashMap<>();
    private static final Map<String, String> PLAN_STATUS_NAME = Map.of("0", "未开始", "1", "执行中", "2", "已完成");
    private static final Map<String, String> FREQ_NAME = Map.of("1", "每日一次", "2", "每日两次", "3", "每日三次");
    private static final Map<String, String> CHECK_NAME = Map.of("0", "未打卡", "1", "已打卡", "2", "漏服");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("MM-dd");

    static {
        GUIDE_TYPE_NAME.put("1", "调药指导");
        GUIDE_TYPE_NAME.put("2", "用药提醒");
        GUIDE_TYPE_NAME.put("3", "饮食推荐");
        ITEM_TYPE_NAME.put("1", "饮食");
        ITEM_TYPE_NAME.put("2", "运动");
        ITEM_TYPE_NAME.put("3", "监测");
        ITEM_TYPE_NAME.put("4", "用药");
    }

    @Override
    public List<MpWarningVO> warnings(Long patientId, String level) {
        Long id = accessService.resolvePatientId(patientId);
        List<CdWarning> list = warningMapper.selectList(Wrappers.<CdWarning>lambdaQuery()
                .eq(CdWarning::getPatientId, id)
                .eq(level != null && !level.isBlank() && !"all".equals(level), CdWarning::getWarningLevel, level)
                .orderByDesc(CdWarning::getTriggerTime));
        Map<Long, String> actions = ruleActions();
        return list.stream().map(w -> toWarningVO(w, actions)).collect(Collectors.toList());
    }

    @Override
    public MpWarningVO warningDetail(Long warningId) {
        CdWarning warning = warningMapper.selectById(warningId);
        if (warning == null) {
            throw new ServiceException("预警记录不存在");
        }
        accessService.requirePatient(warning.getPatientId());
        return toWarningVO(warning, ruleActions());
    }

    @Override
    public List<MpGuideVO> guides(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        List<CdWarning> related = warningMapper.selectList(Wrappers.<CdWarning>lambdaQuery()
                .eq(CdWarning::getPatientId, id));
        Map<Long, CdWarning> warningMap = related.stream()
                .collect(Collectors.toMap(CdWarning::getWarningId, w -> w, (a, b) -> a));
        Map<Long, String> actions = ruleActions();
        return guideMapper.selectList(Wrappers.<CdGuide>lambdaQuery()
                        .eq(CdGuide::getPatientId, id)
                        .orderByDesc(CdGuide::getGuideTime))
                .stream().map(g -> {
                    MpGuideVO vo = new MpGuideVO();
                    vo.setGuideId(g.getGuideId());
                    vo.setPatientId(g.getPatientId());
                    vo.setPatientName(g.getPatientName());
                    vo.setWarningId(g.getWarningId());
                    vo.setGuideType(g.getGuideType());
                    vo.setGuideTypeName(GUIDE_TYPE_NAME.getOrDefault(g.getGuideType(), "健康指导"));
                    vo.setContent(g.getContent());
                    vo.setDoctorName(g.getDoctorName());
                    vo.setGuideTime(g.getGuideTime());
                    vo.setReadStatus(g.getReadStatus());
                    CdWarning w = g.getWarningId() == null ? null : warningMap.get(g.getWarningId());
                    if (w != null && w.getRuleId() != null) {
                        vo.setActionDesc(actions.get(w.getRuleId()));
                    }
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public void readGuide(Long guideId) {
        CdGuide guide = guideMapper.selectById(guideId);
        if (guide == null) {
            throw new ServiceException("指导记录不存在");
        }
        accessService.requirePatient(guide.getPatientId());
        guide.setReadStatus("1");
        guideMapper.updateById(guide);
    }

    @Override
    public List<MpPlanVO> plans(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        List<CdPlan> plans = planMapper.selectList(Wrappers.<CdPlan>lambdaQuery()
                .eq(CdPlan::getPatientId, id)
                .orderByDesc(CdPlan::getBeginDate));
        if (plans.isEmpty()) {
            return new ArrayList<>();
        }
        List<Long> planIds = plans.stream().map(CdPlan::getPlanId).collect(Collectors.toList());
        Map<Long, List<CdPlanItem>> itemMap = planItemMapper.selectList(Wrappers.<CdPlanItem>lambdaQuery()
                        .in(CdPlanItem::getPlanId, planIds).orderByAsc(CdPlanItem::getItemId))
                .stream().collect(Collectors.groupingBy(CdPlanItem::getPlanId));
        return plans.stream().map(p -> {
            MpPlanVO vo = toPlanVO(p);
            vo.setItems(itemMap.getOrDefault(p.getPlanId(), new ArrayList<>())
                    .stream().map(this::toItemVO).collect(Collectors.toList()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public MpPlanVO planDetail(Long planId) {
        CdPlan plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new ServiceException("计划不存在");
        }
        accessService.requirePatient(plan.getPatientId());
        MpPlanVO vo = toPlanVO(plan);
        vo.setItems(planItemMapper.selectList(Wrappers.<CdPlanItem>lambdaQuery()
                        .eq(CdPlanItem::getPlanId, planId).orderByAsc(CdPlanItem::getItemId))
                .stream().map(this::toItemVO).collect(Collectors.toList()));
        return vo;
    }

    @Override
    public List<MpMedicationVO> todayMedication(Long patientId) {
        return medicationOfDay(patientId, null);
    }

    @Override
    public List<MpMedicationVO> medicationOfDay(Long patientId, String date) {
        Long id = accessService.resolvePatientId(patientId);
        LocalDate day = date == null || date.isBlank() ? LocalDate.now() : LocalDate.parse(date);
        List<CdRemind> reminds = remindMapper.selectList(Wrappers.<CdRemind>lambdaQuery()
                .eq(CdRemind::getPatientId, id)
                .eq(CdRemind::getStatus, "1")
                .le(CdRemind::getBeginDate, day)
                .ge(CdRemind::getEndDate, day)
                .orderByAsc(CdRemind::getRemindId));
        Map<String, CdRemindLog> logs = remindLogMapper.selectList(Wrappers.<CdRemindLog>lambdaQuery()
                        .eq(CdRemindLog::getPatientId, id)
                        .eq(CdRemindLog::getPlanDate, day))
                .stream().collect(Collectors.toMap(l -> l.getRemindId() + "|" + l.getPlanTime(), l -> l, (a, b) -> a));
        List<MpMedicationVO> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (CdRemind remind : reminds) {
            MpMedicationVO vo = new MpMedicationVO();
            vo.setRemindId(remind.getRemindId());
            vo.setMedicineName(remind.getMedicineName());
            vo.setDosage(remind.getDosage());
            vo.setFrequency(remind.getFrequency());
            vo.setFrequencyName(FREQ_NAME.getOrDefault(remind.getFrequency(), "-"));
            vo.setBeginDate(remind.getBeginDate());
            vo.setEndDate(remind.getEndDate());
            List<MpMedicationVO.Dose> doses = new ArrayList<>();
            if (remind.getRemindTimes() != null) {
                for (String t : remind.getRemindTimes().split(",")) {
                    String time = t.trim();
                    if (time.isEmpty()) {
                        continue;
                    }
                    MpMedicationVO.Dose dose = new MpMedicationVO.Dose();
                    dose.setPlanTime(time);
                    CdRemindLog log = logs.get(remind.getRemindId() + "|" + time);
                    if (log != null) {
                        dose.setLogId(log.getLogId());
                        dose.setCheckStatus(log.getCheckStatus());
                        dose.setCheckTime(log.getCheckTime());
                    } else {
                        boolean overdue = day.atTime(
                                Integer.parseInt(time.substring(0, 2)),
                                Integer.parseInt(time.substring(3, 5))).isBefore(now);
                        dose.setCheckStatus(day.isBefore(LocalDate.now()) || overdue ? "2" : "0");
                    }
                    dose.setCheckStatusName(CHECK_NAME.getOrDefault(dose.getCheckStatus(), "-"));
                    doses.add(dose);
                }
            }
            vo.setDoses(doses);
            result.add(vo);
        }
        return result;
    }

    @Override
    public void checkMedication(Long patientId, Long remindId, String planTime) {
        Long id = accessService.resolvePatientId(patientId);
        CdRemind remind = remindMapper.selectById(remindId);
        if (remind == null || !Objects.equals(remind.getPatientId(), id)) {
            throw new ServiceException("用药提醒不存在");
        }
        LocalDate today = LocalDate.now();
        CdRemindLog log = remindLogMapper.selectOne(Wrappers.<CdRemindLog>lambdaQuery()
                .eq(CdRemindLog::getRemindId, remindId)
                .eq(CdRemindLog::getPatientId, id)
                .eq(CdRemindLog::getPlanDate, today)
                .eq(CdRemindLog::getPlanTime, planTime)
                .last("limit 1"));
        if (log == null) {
            log = new CdRemindLog();
            log.setRemindId(remindId);
            log.setPatientId(id);
            log.setPlanDate(today);
            log.setPlanTime(planTime);
            log.setCheckStatus("1");
            log.setCheckTime(LocalDateTime.now());
            remindLogMapper.insert(log);
        } else if (!"1".equals(log.getCheckStatus())) {
            log.setCheckStatus("1");
            log.setCheckTime(LocalDateTime.now());
            remindLogMapper.updateById(log);
        }
    }

    // ================= 内部方法 =================

    private Map<Long, String> ruleActions() {
        Map<Long, String> map = new HashMap<>();
        warningRuleMapper.selectList(Wrappers.<CdWarningRule>lambdaQuery())
                .forEach(r -> map.put(r.getRuleId(), r.getActionDesc()));
        return map;
    }

    private MpWarningVO toWarningVO(CdWarning w, Map<Long, String> actions) {
        MpWarningVO vo = new MpWarningVO();
        vo.setWarningId(w.getWarningId());
        vo.setPatientId(w.getPatientId());
        vo.setPatientName(w.getPatientName());
        vo.setDiseaseCode(w.getDiseaseCode());
        vo.setIndicatorCode(w.getIndicatorCode());
        vo.setIndicatorName(w.getIndicatorName());
        vo.setIndicatorValue(w.getIndicatorValue());
        vo.setUnit(w.getUnit());
        vo.setThresholdDesc(w.getThresholdDesc());
        vo.setWarningLevel(w.getWarningLevel());
        vo.setWarningLevelName(LEVEL_NAME.getOrDefault(w.getWarningLevel(), "-"));
        vo.setHandleStatus(w.getHandleStatus());
        vo.setHandleStatusName(HANDLE_NAME.getOrDefault(w.getHandleStatus(), "-"));
        vo.setHandleType(w.getHandleType());
        vo.setHandleContent(w.getHandleContent());
        vo.setHandleBy(w.getHandleBy());
        vo.setHandleTime(w.getHandleTime());
        vo.setEmergencyLinked(w.getEmergencyLinked());
        vo.setTriggerTime(w.getTriggerTime());
        if (w.getRuleId() != null) {
            vo.setActionDesc(actions.get(w.getRuleId()));
        }
        if (w.getTriggerTime() != null) {
            vo.setDateLabel(w.getTriggerTime().format(DAY));
            vo.setTimeLabel(w.getTriggerTime().format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        return vo;
    }

    private MpPlanVO toPlanVO(CdPlan p) {
        MpPlanVO vo = new MpPlanVO();
        vo.setPlanId(p.getPlanId());
        vo.setPatientId(p.getPatientId());
        vo.setPlanName(p.getPlanName());
        vo.setDiseaseCode(p.getDiseaseCode());
        vo.setBeginDate(p.getBeginDate());
        vo.setEndDate(p.getEndDate());
        vo.setTargetDesc(p.getTargetDesc());
        vo.setPlanStatus(p.getPlanStatus());
        vo.setPlanStatusName(PLAN_STATUS_NAME.getOrDefault(p.getPlanStatus(), "-"));
        vo.setExecuteRate(p.getExecuteRate());
        vo.setDoctorName(p.getDoctorName());
        return vo;
    }

    private MpPlanItemVO toItemVO(CdPlanItem item) {
        MpPlanItemVO vo = new MpPlanItemVO();
        vo.setItemId(item.getItemId());
        vo.setItemType(item.getItemType());
        vo.setItemTypeName(ITEM_TYPE_NAME.getOrDefault(item.getItemType(), "其他"));
        vo.setContent(item.getContent());
        vo.setFrequency(item.getFrequency());
        vo.setTargetValue(item.getTargetValue());
        return vo;
    }
}