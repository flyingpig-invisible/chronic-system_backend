package com.chronic.modules.chronic.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatientScoreWeight;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdRemindLog;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientScoreWeightMapper;
import com.chronic.modules.chronic.mapper.CdRemindLogMapper;
import com.chronic.modules.chronic.mapper.CdRemindMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 健康评分算法：以计算时刻为终点回溯 7 天窗口，四项分项加权合成健康分。
 * <p>
 * Ridx   指标达标率：按每次录入汇总，Σ各批次达标指标数 / Σ各批次录入指标数。
 * Rmed   用药依从率：按时打卡 / (按时 + 漏服 + 迟服)；晚 1~2 小时为迟服，超过 2 小时为漏服。
 * Dalert 预警密度：窗口内出现中危及以上预警的天数 / 窗口内有测量数据的天数。
 * Rmon   检测完成率：每天应测 3 次，按窗口内已过完的整天统计实际测量次数。
 * 健康分 = (w1*Ridx + w2*Rmed + w3*(1-Dalert) + w4*Rmon) * 100。
 *
 * @author chronic
 */
@Component
@RequiredArgsConstructor
public class HealthScoreCalculator {

    public static final int WINDOW_DAYS = 7;

    /** 每天应测次数，用于计算检测完成率 */
    public static final int DAILY_TARGET_TIMES = 3;

    public static final String CHECK_ON_TIME = "1";
    public static final String CHECK_MISSED = "2";
    public static final String CHECK_LATE = "3";

    public static final BigDecimal DEFAULT_W_RISK = new BigDecimal("0.40");
    public static final BigDecimal DEFAULT_W_MED = new BigDecimal("0.25");
    public static final BigDecimal DEFAULT_W_ALERT = new BigDecimal("0.15");
    public static final BigDecimal DEFAULT_W_MONITOR = new BigDecimal("0.20");

    private final CdMeasureMapper measureMapper;
    private final CdWarningMapper warningMapper;
    private final CdRemindMapper remindMapper;
    private final CdRemindLogMapper remindLogMapper;
    private final CdPatientScoreWeightMapper weightMapper;

    /** 指标编码 -> 名称，用于补齐历史数据缺失的指标名 */
    private final Map<String, String> indicatorNames = new HashMap<>();

    @PostConstruct
    public void loadIndicatorNames() {
        measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                        .select(CdMeasure::getIndicatorCode, CdMeasure::getIndicatorName)
                        .groupBy(CdMeasure::getIndicatorCode, CdMeasure::getIndicatorName))
                .forEach(m -> {
                    if (StringUtils.hasText(m.getIndicatorCode()) && StringUtils.hasText(m.getIndicatorName())) {
                        indicatorNames.putIfAbsent(m.getIndicatorCode(), m.getIndicatorName());
                    }
                });
    }

    public String indicatorName(String code) {
        return indicatorNames.getOrDefault(code, code);
    }

    /** 患者权重，未配置时返回默认值 */
    public CdPatientScoreWeight resolveWeight(Long patientId) {
        CdPatientScoreWeight weight = weightMapper.selectById(patientId);
        if (weight == null) {
            weight = new CdPatientScoreWeight();
            weight.setPatientId(patientId);
            weight.setWRisk(DEFAULT_W_RISK);
            weight.setWMed(DEFAULT_W_MED);
            weight.setWAlert(DEFAULT_W_ALERT);
            weight.setWMonitor(DEFAULT_W_MONITOR);
        }
        return weight;
    }

    public HealthScoreResult calc(Long patientId) {
        return calc(patientId, LocalDateTime.now());
    }

    public HealthScoreResult calc(Long patientId, LocalDateTime now) {
        LocalDateTime from = now.minusDays(WINDOW_DAYS);
        CdPatientScoreWeight weight = resolveWeight(patientId);

        List<CdMeasure> measures = measureMapper.selectList(new LambdaQueryWrapper<CdMeasure>()
                .eq(CdMeasure::getPatientId, patientId)
                .ge(CdMeasure::getMeasureTime, from)
                .le(CdMeasure::getMeasureTime, now)
                .orderByAsc(CdMeasure::getMeasureTime));

        MonitorPart monitor = calcMonitor(measures, now);
        RiskPart risk = calcRisk(measures);
        MedPart med = calcMedication(patientId, from, now);
        AlertPart alert = calcAlertDensity(patientId, from, now, monitor);

        double w1 = num(weight.getWRisk());
        double w2 = num(weight.getWMed());
        double w3 = num(weight.getWAlert());
        double w4 = num(weight.getWMonitor());

        double riskContrib = w1 * risk.rate;
        double medContrib = w2 * med.rate;
        double alertContrib = w3 * (1.0 - alert.density);
        double monitorContrib = w4 * monitor.rate;
        double dailyScore = riskContrib + medContrib + alertContrib + monitorContrib;
        int healthScore = (int) Math.round(Math.max(0.0, Math.min(1.0, dailyScore)) * 100);

        HealthScoreResult result = new HealthScoreResult();
        result.setPatientId(patientId);
        result.setHealthScore(healthScore);
        result.setDailyScore(round4(dailyScore));
        result.setWindowStart(from);
        result.setWindowEnd(now);
        result.setWeights(weightMap(w1, w2, w3, w4));

        result.setIndicatorRate(round4(risk.rate));
        result.setIndicatorMeasured(risk.qualified);
        result.setIndicatorTotal(risk.entries);
        result.setIndicatorBatches(risk.batchCount);
        result.setIndicatorItems(new LinkedHashMap<>(risk.items));
        result.setIndicatorBatchItems(risk.batches);

        result.setMedicationRate(round4(med.rate));
        result.setMedicationOnTime(med.onTime);
        result.setMedicationLate(med.late);
        result.setMedicationMissed(med.missed);
        result.setMedicationPending(med.pending);

        result.setAlertDensity(round4(alert.density));
        result.setAlertDays(alert.alertDays);
        result.setMeasuredDays(alert.measuredDays);

        result.setMonitorRate(round4(monitor.rate));
        result.setMonitorMeasuredTimes(monitor.times);
        result.setMonitorTargetTimes(monitor.targetTimes);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("windowStart", from.toString());
        detail.put("windowEnd", now.toString());
        detail.put("dailyScore", result.getDailyScore());
        detail.put("weights", result.getWeights());
        detail.put("items", itemDetail(result));
        detail.put("indicatorBatches", risk.batches);
        result.setDetailJson(toJson(detail));
        return result;
    }

    /**
     * 指标达标率：按“每次录入”汇总，达标率 = Σ各批次达标指标数 / Σ各批次录入指标数。
     * 例如一周内第一次录入 7 项达标 5 项、第二次录入 5 项达标 3 项，则 (5+3)/(7+5)。
     * 同一监测时刻入库的多条记录视为同一次录入。
     */
    private RiskPart calcRisk(List<CdMeasure> measures) {
        RiskPart part = new RiskPart();
        part.items = new LinkedHashMap<>();
        if (measures.isEmpty()) {
            return part;
        }
        Map<LocalDateTime, List<CdMeasure>> batches = new TreeMap<>();
        for (CdMeasure m : measures) {
            if (!StringUtils.hasText(m.getIndicatorCode())) {
                continue;
            }
            LocalDateTime time = m.getMeasureTime() != null ? m.getMeasureTime() : m.getCreateTime();
            if (time == null) {
                continue;
            }
            batches.computeIfAbsent(time, k -> new ArrayList<>()).add(m);
        }
        for (Map.Entry<LocalDateTime, List<CdMeasure>> entry : batches.entrySet()) {
            Map<String, CdMeasure> latestOfBatch = new LinkedHashMap<>();
            for (CdMeasure m : entry.getValue()) {
                latestOfBatch.put(m.getIndicatorCode(), m);
            }
            int batchOk = 0;
            for (CdMeasure m : latestOfBatch.values()) {
                boolean qualified = !"1".equals(m.getAbnormalFlag());
                part.entries++;
                if (qualified) {
                    part.qualified++;
                    batchOk++;
                }
                Map<String, Object> item = part.items.get(m.getIndicatorCode());
                if (item == null) {
                    item = new LinkedHashMap<>();
                    item.put("indicatorCode", m.getIndicatorCode());
                    item.put("indicatorName", StringUtils.hasText(m.getIndicatorName())
                            ? m.getIndicatorName() : indicatorName(m.getIndicatorCode()));
                    item.put("qualifiedCount", 0);
                    item.put("measuredCount", 0);
                    part.items.put(m.getIndicatorCode(), item);
                }
                item.put("measuredCount", (Integer) item.get("measuredCount") + 1);
                if (qualified) {
                    item.put("qualifiedCount", (Integer) item.get("qualifiedCount") + 1);
                }
                item.put("lastValue", m.getValue());
                item.put("unit", m.getUnit());
                item.put("lastMeasureTime", m.getMeasureTime() == null ? null : m.getMeasureTime().toString());
                item.put("lastQualified", qualified);
            }
            Map<String, Object> batchInfo = new LinkedHashMap<>();
            batchInfo.put("batchTime", entry.getKey().toString());
            batchInfo.put("count", latestOfBatch.size());
            batchInfo.put("qualified", batchOk);
            part.batches.add(batchInfo);
        }
        part.batchCount = batches.size();
        part.total = part.entries;
        part.rate = part.entries == 0 ? 0.0 : (double) part.qualified / part.entries;
        return part;
    }

    /** 检测完成率：窗口内已过完的整天，每天应测 3 次；单日超过 3 次按 3 次计 */
    private MonitorPart calcMonitor(List<CdMeasure> measures, LocalDateTime now) {
        MonitorPart part = new MonitorPart();
        LocalDate startDay = now.minusDays(WINDOW_DAYS).toLocalDate();
        LocalDate lastFinishedDay = now.toLocalDate().minusDays(1);

        int targetDays = 0;
        for (LocalDate d = startDay; !d.isAfter(lastFinishedDay); d = d.plusDays(1)) {
            targetDays++;
        }
        part.targetTimes = targetDays * DAILY_TARGET_TIMES;

        for (CdMeasure m : measures) {
            if (m.getMeasureTime() == null) {
                continue;
            }
            LocalDate day = m.getMeasureTime().toLocalDate();
            if (day.isAfter(lastFinishedDay)) {
                continue;
            }
            part.dayCount.merge(day, 1, Integer::sum);
        }
        for (Integer count : part.dayCount.values()) {
            part.times += Math.min(count, DAILY_TARGET_TIMES);
        }
        part.rate = part.targetTimes == 0 ? 0.0 : Math.min(1.0, (double) part.times / part.targetTimes);
        return part;
    }

    /** 用药依从率：打卡时间与计划时间比对，晚 1~2 小时迟服，超过 2 小时漏服 */
    private MedPart calcMedication(Long patientId, LocalDateTime from, LocalDateTime to) {
        MedPart part = new MedPart();
        List<CdRemindLog> logs = remindLogMapper.selectList(new LambdaQueryWrapper<CdRemindLog>()
                .eq(CdRemindLog::getPatientId, patientId)
                .ge(CdRemindLog::getPlanDate, from.toLocalDate())
                .le(CdRemindLog::getPlanDate, to.toLocalDate()));

        Set<Long> remindIds = logs.stream()
                .map(CdRemindLog::getRemindId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, CdRemind> remindMap = new HashMap<>();
        if (!remindIds.isEmpty()) {
            remindMapper.selectBatchIds(remindIds).forEach(r -> remindMap.put(r.getRemindId(), r));
        }
        Set<String> seen = new HashSet<>();
        for (CdRemindLog log : logs) {
            String key = log.getRemindId() + "|" + log.getPlanDate() + "|" + log.getPlanTime();
            if (!seen.add(key)) {
                continue;
            }
            LocalDateTime planAt = planAt(log);
            if (planAt == null || planAt.isBefore(from) || planAt.isAfter(to)) {
                continue;
            }
            CdRemind remind = remindMap.get(log.getRemindId());
            if (remind != null) {
                LocalDate begin = remind.getBeginDate();
                LocalDate end = remind.getEndDate();
                if ((begin != null && log.getPlanDate().isBefore(begin))
                        || (end != null && log.getPlanDate().isAfter(end))) {
                    continue;
                }
            }
            String status = classify(log, planAt, to);
            if (CHECK_ON_TIME.equals(status)) {
                part.onTime++;
            } else if (CHECK_LATE.equals(status)) {
                part.late++;
            } else if (CHECK_MISSED.equals(status)) {
                part.missed++;
            } else {
                part.pending++;
            }
        }
        int base = part.onTime + part.late + part.missed;
        part.rate = base == 0 ? 1.0 : (double) part.onTime / base;
        return part;
    }

    private LocalDateTime planAt(CdRemindLog log) {
        if (log.getPlanDate() == null) {
            return null;
        }
        String time = StringUtils.hasText(log.getPlanTime()) ? log.getPlanTime().trim() : "08:00";
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return LocalDateTime.of(log.getPlanDate(), LocalTime.of(hour, minute));
        } catch (Exception e) {
            return null;
        }
    }

    /** 提前/准点（≤60 分钟）按时，超过 60 分钟且不超过 120 分钟迟服，超过 120 分钟漏服 */
    private String classify(CdRemindLog log, LocalDateTime planAt, LocalDateTime now) {
        LocalDateTime checkTime = log.getCheckTime();
        if (checkTime != null) {
            long minutes = Duration.between(planAt, checkTime).toMinutes();
            if (minutes <= 60) {
                return CHECK_ON_TIME;
            }
            return minutes <= 120 ? CHECK_LATE : CHECK_MISSED;
        }
        if (CHECK_ON_TIME.equals(log.getCheckStatus())) {
            return CHECK_ON_TIME;
        }
        if (CHECK_LATE.equals(log.getCheckStatus())) {
            return CHECK_LATE;
        }
        if (CHECK_MISSED.equals(log.getCheckStatus())) {
            return CHECK_MISSED;
        }
        return planAt.plusMinutes(120).isAfter(now) ? "0" : CHECK_MISSED;
    }

    /** 预警密度：中危及以上预警的天数 / 有测量数据的天数，无数据的日子不计入分母 */
    private AlertPart calcAlertDensity(Long patientId, LocalDateTime from, LocalDateTime to, MonitorPart monitor) {
        AlertPart part = new AlertPart();
        Set<LocalDate> warningDays = new HashSet<>();
        warningMapper.selectList(new LambdaQueryWrapper<CdWarning>()
                        .eq(CdWarning::getPatientId, patientId)
                        .in(CdWarning::getWarningLevel, "2", "3")
                        .ge(CdWarning::getTriggerTime, from)
                        .le(CdWarning::getTriggerTime, to)
                        .select(CdWarning::getTriggerTime))
                .forEach(w -> {
                    if (w.getTriggerTime() != null) {
                        warningDays.add(w.getTriggerTime().toLocalDate());
                    }
                });
        int measuredDays = monitor.dayCount.size();
        int hitDays = 0;
        for (LocalDate day : warningDays) {
            if (monitor.dayCount.containsKey(day)) {
                hitDays++;
            }
        }
        part.alertDays = hitDays;
        part.measuredDays = measuredDays;
        part.density = measuredDays == 0 ? 0.0 : (double) hitDays / measuredDays;
        return part;
    }

    private List<Map<String, Object>> itemDetail(HealthScoreResult r) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> weights = r.getWeights();
        list.add(item("risk", "指标达标率", "Ridx", r.getIndicatorRate(), weights.get("wRisk"),
                String.format("近 7 天共 %d 次录入，%d/%d 项达标", r.getIndicatorBatches(),
                        r.getIndicatorMeasured(), r.getIndicatorTotal())));
        list.add(item("alert", "预警密度(反向)", "1-Dalert", 1 - r.getAlertDensity(), weights.get("wAlert"),
                String.format("有数据 %d 天，其中 %d 天出现中危及以上预警", r.getMeasuredDays(), r.getAlertDays())));
        list.add(item("med", "用药依从率", "Rmed", r.getMedicationRate(), weights.get("wMed"),
                String.format("按时 %d 次，迟服 %d 次，漏服 %d 次", r.getMedicationOnTime(),
                        r.getMedicationLate(), r.getMedicationMissed())));
        list.add(item("monitor", "检测完成率", "Rmon", r.getMonitorRate(), weights.get("wMonitor"),
                String.format("完成 %d 次 / 应测 %d 次", r.getMonitorMeasuredTimes(), r.getMonitorTargetTimes())));
        return list;
    }

    private Map<String, Object> item(String code, String name, String formula, double value, Object weight, String desc) {
        double w = num(weight);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", code);
        m.put("name", name);
        m.put("formula", formula);
        m.put("value", round4(value));
        m.put("weight", round4(w));
        m.put("contribution", round4(w * value));
        m.put("desc", desc);
        return m;
    }

    private Map<String, Object> weightMap(double w1, double w2, double w3, double w4) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("wRisk", round4(w1));
        m.put("wMed", round4(w2));
        m.put("wAlert", round4(w3));
        m.put("wMonitor", round4(w4));
        return m;
    }

    private double num(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    @SuppressWarnings("unchecked")
    private String toJson(Object v) {
        if (v == null) {
            return "null";
        }
        if (v instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, Object> e : ((Map<String, Object>) v).entrySet()) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append("\"").append(escape(e.getKey())).append("\":").append(toJson(e.getValue()));
            }
            return sb.append("}").toString();
        }
        if (v instanceof List) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object o : (List<Object>) v) {
                if (!first) {
                    sb.append(",");
                }
                first = false;
                sb.append(toJson(o));
            }
            return sb.append("]").toString();
        }
        if (v instanceof Number || v instanceof Boolean) {
            return v.toString();
        }
        return "\"" + escape(v.toString()) + "\"";
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    private static class RiskPart {
        double rate;
        int qualified;
        int entries;
        int total;
        int batchCount;
        Map<String, Map<String, Object>> items;
        List<Map<String, Object>> batches = new ArrayList<>();
    }

    private static class MonitorPart {
        double rate;
        int times;
        int targetTimes;
        Map<LocalDate, Integer> dayCount = new TreeMap<>();
    }

    private static class MedPart {
        double rate;
        int onTime;
        int late;
        int missed;
        int pending;
    }

    private static class AlertPart {
        double density;
        int alertDays;
        int measuredDays;
    }
}