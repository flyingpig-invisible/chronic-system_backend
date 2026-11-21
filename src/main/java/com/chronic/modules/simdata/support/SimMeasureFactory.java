package com.chronic.modules.simdata.support;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 模拟设备采集数据生成器
 * <p>
 * 只负责「某个整点、某个指标应该出什么值」：
 * <ul>
 *     <li>血压计：收缩压 sbp、舒张压 dbp</li>
 *     <li>手表：心率 hr、血糖 glu、血氧 spo2、呼吸频率 rr、睡眠时长 sleep</li>
 * </ul>
 * 数值分布参考医生端手动录入的历史数据（及预警阈值），并叠加昼夜节律，
 * 使模拟曲线在趋势图上呈现「夜间低、日间高」的真实形态。
 *
 * @author chronic
 */
public final class SimMeasureFactory {

    /** 血压计可导入指标 */
    public static final List<String> BP_INDICATORS = List.of("sbp", "dbp");

    /** 手表可导入指标 */
    public static final List<String> WATCH_INDICATORS = List.of("hr", "glu", "spo2", "rr", "sleep");

    /** 睡眠指标：一天只出一次，取值与整天一致 */
    public static final String SLEEP = "sleep";

    private SimMeasureFactory() {
    }

    /** 模拟生成的一项采集值 */
    public static final class SimPoint {
        private final String indicatorCode;
        private final String indicatorName;
        private final String unit;
        private final BigDecimal value;
        private final LocalDateTime measureTime;

        SimPoint(String indicatorCode, String indicatorName, String unit, BigDecimal value, LocalDateTime measureTime) {
            this.indicatorCode = indicatorCode;
            this.indicatorName = indicatorName;
            this.unit = unit;
            this.value = value;
            this.measureTime = measureTime;
        }

        public String getIndicatorCode() { return indicatorCode; }
        public String getIndicatorName() { return indicatorName; }
        public String getUnit() { return unit; }
        public BigDecimal getValue() { return value; }
        public LocalDateTime getMeasureTime() { return measureTime; }
    }

    public static String nameOf(String code) {
        switch (code) {
            case "sbp": return "收缩压";
            case "dbp": return "舒张压";
            case "hr": return "心率";
            case "glu": return "血糖";
            case "spo2": return "血氧";
            case "rr": return "呼吸频率";
            case "sleep": return "睡眠时长";
            default: return code;
        }
    }

    public static String unitOf(String code) {
        switch (code) {
            case "sbp":
            case "dbp": return "mmHg";
            case "glu": return "mmol/L";
            case "spo2": return "%";
            case "sleep": return "小时";
            case "hr":
            case "rr": return "次/分";
            default: return "";
        }
    }

    /** 各项指标的可视化保留位数 */
    public static int scaleOf(String code) {
        return "glu".equals(code) || SLEEP.equals(code) ? 2 : 1;
    }

    /**
     * 生成 [from, to] 内每个整点的模拟数据
     *
     * @param indicators 请求导入的指标编码
     * @param from       起始时间（含）
     * @param to         结束时间（含）
     * @param random     随机源
     */
    public static List<SimPoint> generate(List<String> indicators, LocalDateTime from, LocalDateTime to, Random random) {
        List<SimPoint> points = new ArrayList<>();
        if (indicators == null || indicators.isEmpty() || from == null || to == null) {
            return points;
        }
        // 睡眠：按「自然日」出数，同一天在范围内只取一个值，各整点取值相同
        LocalDateTime day = from.toLocalDate().atStartOfDay();
        while (!day.isAfter(to)) {
            LocalDateTime dayEnd = day.toLocalDate().atTime(23, 59, 59);
            LocalDateTime start = day.isBefore(from) ? from : day;
            LocalDateTime end = dayEnd.isAfter(to) ? to : dayEnd;
            if (indicators.contains(SLEEP) && !start.isAfter(end)) {
                BigDecimal sleep = sleepHours(random);
                for (LocalDateTime t = start; !t.isAfter(end); t = t.plusHours(1)) {
                    points.add(new SimPoint(SLEEP, nameOf(SLEEP), unitOf(SLEEP), sleep, t));
                }
            }
            day = day.plusDays(1);
        }

        for (LocalDateTime t = from; !t.isAfter(to); t = t.plusHours(1)) {
            // 同一整点共享一组随机数，保证收缩压/舒张压、心率/呼吸等彼此相关，像同一次测量
            Map<String, BigDecimal> moment = sampleMoment(t, random);
            for (String code : indicators) {
                if (SLEEP.equals(code)) {
                    continue;
                }
                BigDecimal value = moment.get(code);
                if (value != null) {
                    points.add(new SimPoint(code, nameOf(code), unitOf(code), value, t));
                }
            }
        }
        return points;
    }

    /**
     * 生成某个整点的全部指标值
     * <p>
     * 血压与心率共用同一个「活动度」因子，收缩压、舒张压共用升压事件，
     * 这样同一次测量的各项数值是协调的，而不是各自独立的随机数。
     */
    private static Map<String, BigDecimal> sampleMoment(LocalDateTime time, Random random) {
        Map<String, BigDecimal> values = new HashMap<>();
        int hour = time.getHour();
        boolean night = hour < 6 || hour >= 22;
        boolean sleepWindow = hour >= 22 || hour < 7;
        double activity = gauss(random);
        boolean pressorSpike = random.nextDouble() < 0.05;
        double bpRise = pressorSpike ? 18 + random.nextDouble() * 25 : 0;

        double sbp = 116 + diurnal(time, 16) + activity * 6 + bpRise;
        double dbp = 74 + diurnal(time, 9) + activity * 4 + bpRise * 0.45;
        double hr = (night ? 66 : 78) + diurnal(time, 10) + activity * 4
                + (random.nextDouble() < 0.04 ? 12 + random.nextDouble() * 12 : 0);
        double glu = 5.5 + diurnal(time, 2.0) + gauss(random) * 0.45
                + (random.nextDouble() < 0.06 ? 2.5 + random.nextDouble() * 3.5 : 0);
        double spo2 = 96.4 + gauss(random) * 1.0 - (sleepWindow ? 1.1 + random.nextDouble() : 0)
                - (random.nextDouble() < 0.04 ? 4 + random.nextDouble() * 3 : 0);
        double rr = (night ? 15.0 : 16.8) + diurnal(time, 2.6) + gauss(random) * 1.1
                + (random.nextDouble() < 0.04 ? 3 + random.nextDouble() * 3 : 0);

        values.put("sbp", round(sbp, "sbp"));
        values.put("dbp", round(Math.min(dbp, sbp - 20), "dbp"));
        values.put("hr", round(hr, "hr"));
        values.put("glu", round(glu, "glu"));
        values.put("spo2", round(Math.min(spo2, 99.5), "spo2"));
        values.put("rr", round(rr, "rr"));
        return values;
    }

    private static BigDecimal round(double value, String code) {
        return BigDecimal.valueOf(value).setScale(scaleOf(code), RoundingMode.HALF_UP);
    }
    /**
     * 睡眠时长：一天一个值，且同一天内所有整点相同
     * <p>
     * 睡眠具有跨天属性，设备在夜间连续监测、次日清晨汇总，因此一天只产出一条记录，
     * 不会随小时变化；这里按 7.2 小时均值生成整体作息状况。
     */
    private static BigDecimal sleepHours(Random random) {
        double hours = 7.2 + gauss(random) * 0.9;
        if (random.nextDouble() < 0.08) {
            hours -= 1.6 + random.nextDouble() * 0.9;
        }
        hours = Math.max(3.6, Math.min(9.8, hours));
        return BigDecimal.valueOf(hours).setScale(2, RoundingMode.HALF_UP);
    }

    /** 昼夜节律：6 点为谷值，15 点附近为峰值 */
    private static double diurnal(LocalDateTime time, double amplitude) {
        double phase = (time.getHour() + time.getMinute() / 60.0 - 6.0) / 24.0 * 2 * Math.PI;
        return -Math.cos(phase) * amplitude / 2;
    }

    private static double gauss(Random random) {
        return random.nextGaussian();
    }
}