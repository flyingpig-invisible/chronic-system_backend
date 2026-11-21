package com.chronic.modules.chronic.support.huawei;

import com.chronic.common.exception.ServiceException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 华为运动健康导出包解析器
 * <p>
 * 输入：ZIP（整个导出目录压缩）或单个 health detail data*.json
 * 输出：按小时聚合后的心率/血氧、按夜汇总的睡眠时长
 * <p>
 * 设计为不依赖数据库的纯解析组件，便于单元测试
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HuaweiExportParser {

    /** 单次导入最多保留的测量点 */
    private static final int MAX_POINTS = 20000;

    /** 解压后允许的最大总字节数，防 zip 炸弹 */
    private static final long MAX_UNZIP_BYTES = 200L * 1024 * 1024;

    /** 单个 json 文件允许的最大字节数 */
    private static final long MAX_JSON_BYTES = 64L * 1024 * 1024;

    private static final BigDecimal HR_MIN = BigDecimal.valueOf(30);
    private static final BigDecimal HR_MAX = BigDecimal.valueOf(220);
    private static final BigDecimal SPO2_MIN = BigDecimal.valueOf(50);
    private static final BigDecimal SPO2_MAX = BigDecimal.valueOf(100);
    private static final BigDecimal SLEEP_MIN_HOURS = BigDecimal.ONE;
    private static final BigDecimal SLEEP_MAX_HOURS = BigDecimal.valueOf(16);

    private final ObjectMapper objectMapper;

    /**
     * 解析入口
     *
     * @param fileName  原始文件名（用于判断 zip / json）
     * @param bytes     文件内容
     * @param aggregate 是否按小时聚合（心率、血氧）
     */
    public ParseResult parse(String fileName, byte[] bytes, boolean aggregate) {
        if (bytes == null || bytes.length == 0) {
            throw new ServiceException("文件内容为空");
        }
        ParseResult result = new ParseResult();
        boolean zip = isZip(fileName, bytes);
        result.setFileType(zip ? "ZIP" : "JSON");

        List<HealthPoint> raw = new ArrayList<>();
        Map<LocalDate, Night> nights = new LinkedHashMap<>();

        if (zip) {
            parseZip(bytes, result, raw, nights);
        } else {
            parseJson(new ByteArrayInputStream(bytes), result, raw, nights);
            result.setFileCount(1);
        }

        if (result.getFileCount() == 0) {
            throw new ServiceException("未在文件中找到华为健康数据（Health detail data 目录下的 json）");
        }
        if (raw.isEmpty() && nights.isEmpty()) {
            throw new ServiceException("未解析到可导入的心率、血氧或睡眠数据");
        }

        postProcess(result, raw, nights, aggregate);
        log.info("华为导出包解析完成：文件 {} 个，记录 {} 条，采样点 {} 个，最终 {} 条",
                result.getFileCount(), result.getRawRecords(), result.getParsedPoints(), result.getPoints().size());
        return result;
    }

    // ------------------------------------------------------------------ ZIP

    private boolean isZip(String fileName, byte[] bytes) {
        if (fileName != null && fileName.toLowerCase().endsWith(".zip")) {
            return true;
        }
        return bytes.length > 3 && bytes[0] == 'P' && bytes[1] == 'K';
    }

    private void parseZip(byte[] bytes, ParseResult result,
                          List<HealthPoint> raw, Map<LocalDate, Night> nights) {
        long unzipped = 0;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName() == null ? "" : entry.getName();
                if (entry.isDirectory()) {
                    continue;
                }
                // 防路径穿越
                if (name.contains("..") || name.startsWith("/") || name.startsWith("\\")) {
                    log.warn("跳过可疑压缩包条目：{}", name);
                    continue;
                }
                String lower = name.toLowerCase();
                // 只解析华为健康明细目录下的 json，其余（xls/xlsx 台账）直接忽略
                if (!lower.endsWith(".json") || !lower.contains("health detail data")) {
                    continue;
                }
                byte[] data = readEntry(zis, MAX_JSON_BYTES);
                if (data == null) {
                    continue;
                }
                unzipped += data.length;
                if (unzipped > MAX_UNZIP_BYTES) {
                    throw new ServiceException("压缩包解压后过大，已中止导入");
                }
                // 空数组文件（health detail data.json）直接跳过
                if (data.length <= 3) {
                    continue;
                }
                parseJson(new ByteArrayInputStream(data), result, raw, nights);
                result.setFileCount(result.getFileCount() + 1);
            }
        } catch (IOException e) {
            String msg = e.getMessage() == null ? "" : e.getMessage();
            if (msg.toLowerCase().contains("encrypt")) {
                throw new ServiceException("压缩包已加密，Java 无法读取。请去掉密码重新压缩后再上传");
            }
            throw new ServiceException("解析压缩包失败：" + msg);
        }
    }

    private byte[] readEntry(InputStream in, long maxBytes) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int len;
        long total = 0;
        while ((len = in.read(buf)) != -1) {
            total += len;
            if (total > maxBytes) {
                return null;
            }
            bos.write(buf, 0, len);
        }
        return bos.toByteArray();
    }

    // ------------------------------------------------------------------ JSON

    /**
     * 流式解析一个 json 文件：顶层是数组，逐个 record 读取，避免整包读入内存
     */
    private void parseJson(InputStream in, ParseResult result,
                           List<HealthPoint> raw, Map<LocalDate, Night> nights) {
        try (JsonParser parser = objectMapper.getFactory().createParser(in)) {
            if (parser.nextToken() != JsonToken.START_ARRAY) {
                return;
            }
            while (parser.nextToken() == JsonToken.START_OBJECT) {
                JsonNode record = objectMapper.readTree(parser);
                expandRecord(record, result, raw, nights);
            }
        } catch (IOException e) {
            log.warn("解析 json 文件失败：{}", e.getMessage());
        }
    }

    private void expandRecord(JsonNode record, ParseResult result,
                              List<HealthPoint> raw, Map<LocalDate, Night> nights) {
        int seq = result.getRawRecords() + 1;
        int type = record.path("type").asInt(0);
        String timeZone = record.path("timeZone").asText("+0800");
        String deviceCode = record.path("deviceCode").isMissingNode()
                ? null : record.path("deviceCode").asText(null);

        JsonNode points = record.path("samplePoints");
        if (!points.isArray() || points.isEmpty()) {
            result.setRawRecords(seq);
            return;
        }
        result.setRawRecords(seq);
        if (deviceCode != null && !deviceCode.isBlank()) {
            result.getDeviceCodes().add(deviceCode);
        }

        for (JsonNode point : points) {
            long start = point.path("startTime").asLong(record.path("startTime").asLong(0));
            long end = point.path("endTime").asLong(record.path("endTime").asLong(0));
            String key = point.path("key").asText("");
            JsonNode value = point.path("value");
            result.setParsedPoints(result.getParsedPoints() + 1);

            if (HuaweiTypes.isHeartRate(type)) {
                BigDecimal v = heartRateValue(type, value);
                LocalDateTime time = toLocal(start, timeZone);
                if (v == null || time == null) {
                    result.setInvalidPoints(result.getInvalidPoints() + 1);
                    result.addIssue(seq, "心率数值或时间无法解析");
                } else if (v.compareTo(HR_MIN) < 0 || v.compareTo(HR_MAX) > 0) {
                    result.setInvalidPoints(result.getInvalidPoints() + 1);
                    result.addIssue(seq, "心率 " + v.stripTrailingZeros().toPlainString() + " 超出合理范围(30-220)");
                } else {
                    raw.add(new HealthPoint("hr", "心率", "次/分", scale(v, 1), time, deviceCode));
                }
            } else if (HuaweiTypes.isBloodOxygen(type)) {
                BigDecimal v = spo2Value(type, value);
                LocalDateTime time = toLocal(start, timeZone);
                if (v == null || time == null) {
                    result.setInvalidPoints(result.getInvalidPoints() + 1);
                    result.addIssue(seq, "血氧数值或时间无法解析");
                } else {
                    if (v.compareTo(BigDecimal.ONE) <= 0) {
                        // 部分机型导出 0.97 这种小数，统一换算成百分比
                        v = v.multiply(BigDecimal.valueOf(100));
                    }
                    if (v.compareTo(SPO2_MIN) < 0 || v.compareTo(SPO2_MAX) > 0) {
                        result.setInvalidPoints(result.getInvalidPoints() + 1);
                        result.addIssue(seq, "血氧 " + v.stripTrailingZeros().toPlainString() + " 超出合理范围(50-100)");
                    } else {
                        raw.add(new HealthPoint("spo2", "血氧", "%", scale(v, 1), time, deviceCode));
                    }
                }
            } else if (HuaweiTypes.isSleepSegment(type) && HuaweiTypes.SLEEP_SEGMENT_KEYS.contains(key)) {
                if (end <= start) {
                    result.setInvalidPoints(result.getInvalidPoints() + 1);
                    continue;
                }
                LocalDateTime segEnd = toLocal(end, timeZone);
                if (segEnd == null) {
                    result.setInvalidPoints(result.getInvalidPoints() + 1);
                    continue;
                }
                LocalDate night = nightOf(segEnd);
                if (night == null) {
                    // 白天的小睡（12:00-18:00）不计入夜间睡眠，避免把午睡并进夜睡
                    result.setUnsupportedPoints(result.getUnsupportedPoints() + 1);
                    continue;
                }
                Night bucket = nights.computeIfAbsent(night, Night::new);
                bucket.minutes += (end - start) / 60000.0;
                if (bucket.wakeTime == null || segEnd.isAfter(bucket.wakeTime)) {
                    bucket.wakeTime = segEnd;
                }
                if (deviceCode != null && !deviceCode.isBlank()) {
                    bucket.deviceCode = deviceCode;
                }
            } else {
                result.setUnsupportedPoints(result.getUnsupportedPoints() + 1);
            }
        }
    }

    // ------------------------------------------------------------------ 聚合

    private void postProcess(ParseResult result, List<HealthPoint> raw,
                             Map<LocalDate, Night> nights, boolean aggregate) {
        List<HealthPoint> out = new ArrayList<>();
        if (aggregate) {
            out.addAll(aggregateByHour(raw, "hr"));
            out.addAll(aggregateByHour(raw, "spo2"));
        } else {
            out.addAll(raw);
        }

        // 睡眠按时长汇总，每晚一条
        for (Night night : nights.values()) {
            BigDecimal hours = BigDecimal.valueOf(night.minutes / 60.0)
                    .setScale(2, RoundingMode.HALF_UP);
            if (hours.compareTo(SLEEP_MIN_HOURS) < 0 || hours.compareTo(SLEEP_MAX_HOURS) > 0) {
                result.setDroppedSleepNights(result.getDroppedSleepNights() + 1);
                continue;
            }
            LocalDateTime time = night.wakeTime == null
                    ? night.date.atTime(8, 0) : night.wakeTime;
            out.add(new HealthPoint("sleep", "睡眠时长", "小时", hours, time, night.deviceCode));
        }

        out.sort((a, b) -> a.getMeasureTime().compareTo(b.getMeasureTime()));
        if (out.size() > MAX_POINTS) {
            out = new ArrayList<>(out.subList(0, MAX_POINTS));
            result.addIssue(0, "测量点超过 " + MAX_POINTS + " 条，仅保留前 " + MAX_POINTS + " 条");
        }

        result.setPoints(out);
        Map<String, Integer> saved = new LinkedHashMap<>();
        LocalDate from = null;
        LocalDate to = null;
        LocalDateTime latest = null;
        for (HealthPoint p : out) {
            saved.merge(p.getIndicatorCode(), 1, Integer::sum);
            LocalDate d = p.getMeasureTime().toLocalDate();
            if (from == null || d.isBefore(from)) {
                from = d;
            }
            if (to == null || d.isAfter(to)) {
                to = d;
            }
            if (latest == null || p.getMeasureTime().isAfter(latest)) {
                latest = p.getMeasureTime();
            }
        }
        result.setSaved(saved);
        result.setDateFrom(from);
        result.setDateTo(to);
        result.setLatestTime(latest);
    }

    /** 同一指标按小时取均值 */
    private List<HealthPoint> aggregateByHour(List<HealthPoint> raw, String indicatorCode) {
        Map<String, List<HealthPoint>> buckets = new LinkedHashMap<>();
        for (HealthPoint p : raw) {
            if (!indicatorCode.equals(p.getIndicatorCode())) {
                continue;
            }
            LocalDateTime t = p.getMeasureTime().withMinute(0).withSecond(0).withNano(0);
            buckets.computeIfAbsent(t.toString(), k -> new ArrayList<>()).add(p);
        }
        List<HealthPoint> out = new ArrayList<>();
        for (List<HealthPoint> list : buckets.values()) {
            BigDecimal sum = BigDecimal.ZERO;
            for (HealthPoint p : list) {
                sum = sum.add(p.getValue());
            }
            BigDecimal avg = sum.divide(BigDecimal.valueOf(list.size()), 1, RoundingMode.HALF_UP);
            HealthPoint first = list.get(0);
            LocalDateTime time = first.getMeasureTime().withMinute(0).withSecond(0).withNano(0);
            out.add(new HealthPoint(indicatorCode, first.getIndicatorName(), first.getUnit(),
                    avg, time, first.getDeviceCode()));
        }
        return out;
    }

    // ------------------------------------------------------------------ 取值

    private BigDecimal heartRateValue(int type, JsonNode value) {
        if (type == HuaweiTypes.HEART_RATE) {
            return toDecimal(value);
        }
        JsonNode node = toJson(value);
        if (node == null) {
            return null;
        }
        return type == HuaweiTypes.DYNAMIC_HEART_RATE
                ? decimalOf(node, "bpm") : decimalOf(node, "restBpm");
    }

    private BigDecimal spo2Value(int type, JsonNode value) {
        JsonNode node = toJson(value);
        if (node == null) {
            if (type == HuaweiTypes.BLOOD_OXYGEN) {
                return toDecimal(value);
            }
            return null;
        }
        return type == HuaweiTypes.BLOOD_OXYGEN_NEW
                ? decimalOf(node, "spo2") : decimalOf(node, "avgSaturation");
    }

    /** value 可能是数字，也可能是字符串化的 json */
    private JsonNode toJson(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isObject()) {
            return value;
        }
        String text = value.asText("");
        if (text.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(text);
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal toDecimal(JsonNode value) {
        if (value == null || value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        String text = value.asText("").trim();
        if (text.isEmpty() || "null".equalsIgnoreCase(text)) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private BigDecimal decimalOf(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return v == null ? null : toDecimal(v);
    }

    private BigDecimal scale(BigDecimal value, int scale) {
        return value.setScale(scale, RoundingMode.HALF_UP);
    }

    // ------------------------------------------------------------------ 时间

    /** UTC 毫秒 + timeZone 偏移 -> LocalDateTime */
    private LocalDateTime toLocal(long epochMs, String timeZone) {
        if (epochMs <= 0) {
            return null;
        }
        ZoneOffset offset = ZoneOffset.ofHours(8);
        if (timeZone != null && !timeZone.isBlank()) {
            try {
                offset = ZoneOffset.of(timeZone);
            } catch (Exception ignored) {
                // 保持默认 +08:00
            }
        }
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), offset);
    }

    /**
     * 归夜规则：18:00 之后算当晚，12:00 之前算前一夜，12:00-18:00 视为小睡返回 null
     */
    private LocalDate nightOf(LocalDateTime time) {
        int hour = time.getHour();
        if (hour >= 18) {
            return time.toLocalDate();
        }
        if (hour < 12) {
            return time.toLocalDate().minusDays(1);
        }
        return null;
    }

    /** 一夜的睡眠累加器 */
    private static class Night {

        private final LocalDate date;
        private double minutes;
        private LocalDateTime wakeTime;
        private String deviceCode;

        private Night(LocalDate date) {
            this.date = date;
        }
    }
}
