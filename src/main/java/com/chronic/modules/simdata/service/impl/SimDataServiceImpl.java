package com.chronic.modules.simdata.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdDevice;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdDeviceMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.simdata.dto.SimImportDTO;
import com.chronic.modules.simdata.service.SimDataService;
import com.chronic.modules.simdata.support.SimMeasureFactory;
import com.chronic.modules.simdata.support.SimMeasureFactory.SimPoint;
import com.chronic.modules.simdata.vo.SimDeviceVO;
import com.chronic.modules.simdata.vo.SimImportResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 设备模拟数据导入实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SimDataServiceImpl implements SimDataService {

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 时间范围：最少 2 小时，最多 2 天 */
    private static final int MIN_HOURS = 2;
    private static final int MAX_HOURS = 48;

    /** 模拟数据来源标记：3=设备导入，与手动录入(2)区分，便于追溯 */
    private static final String DATA_SOURCE = "3";

    private static final String TYPE_WATCH = "1";
    private static final String TYPE_BP = "2";

    private final MpAccessService accessService;
    private final CdDeviceMapper deviceMapper;
    private final CdMeasureMapper measureMapper;
    private final CdPatientMapper patientMapper;
    private final CdWarningService warningService;
    private final CdScoreService scoreService;

    private final Random random = new Random();

    @Override
    public SimDeviceVO deviceOptions(Long patientId, Long deviceId) {
        Long id = accessService.resolvePatientId(patientId);
        CdDevice device = requireBoundDevice(id, deviceId);
        List<String> codes = indicatorsOf(device.getDeviceType());
        SimDeviceVO vo = new SimDeviceVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(deviceDisplayName(device));
        vo.setDeviceType(device.getDeviceType());
        vo.setDeviceTypeName("1".equals(device.getDeviceType()) || "3".equals(device.getDeviceType())
                ? "智能手表" : "2".equals(device.getDeviceType()) ? "血压计" : "健康设备");
        vo.setIndicators(codes.stream()
                .map(c -> new SimDeviceVO.Option(c, SimMeasureFactory.nameOf(c), SimMeasureFactory.unitOf(c)))
                .collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SimImportResultVO importData(SimImportDTO dto) {
        Long patientId = accessService.resolvePatientId(dto.getPatientId());
        CdPatient patient = accessService.requirePatient(patientId);
        CdDevice device = requireBoundDevice(patientId, dto.getDeviceId());

        List<String> supported = indicatorsOf(device.getDeviceType());
        if (supported.isEmpty()) {
            throw new ServiceException("该设备暂不支持导入数据");
        }
        Set<String> codes = new LinkedHashSet<>();
        if (dto.getIndicators() != null) {
            codes.addAll(dto.getIndicators());
        }
        if (codes.isEmpty()) {
            throw new ServiceException("请选择要导入的数据");
        }
        for (String code : codes) {
            if (!supported.contains(code)) {
                throw new ServiceException(deviceDisplayName(device) + "不支持导入「" + SimMeasureFactory.nameOf(code) + "」");
            }
        }

        LocalDateTime[] range = normalizeRange(dto.getStartTime(), dto.getEndTime());
        LocalDateTime from = range[0];
        LocalDateTime to = range[1];
        int hours = (int) Duration.between(from, to).toHours() + 1;

        List<SimPoint> points = SimMeasureFactory.generate(new ArrayList<>(codes), from, to, random).stream()
                .filter(p -> !p.getMeasureTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
        int success = 0;
        int skipped = 0;
        int warningCount = 0;
        for (SimPoint point : points) {
            CdMeasure measure = new CdMeasure();
            measure.setPatientId(patientId);
            measure.setPatientName(patient.getName());
            measure.setDeviceId(device.getDeviceId());
            measure.setIndicatorCode(point.getIndicatorCode());
            measure.setIndicatorName(point.getIndicatorName());
            measure.setValue(point.getValue());
            measure.setUnit(point.getUnit());
            measure.setMeasureTime(point.getMeasureTime());
            measure.setDataSource(DATA_SOURCE);
            measure.setAbnormalFlag("0");
            try {
                measureMapper.insert(measure);
            } catch (DuplicateKeyException e) {
                // 同一患者、指标、整点已存在数据：跳过而不是报错，避免重复导入中断
                skipped++;
                continue;
            }
            success++;
            String unit = point.getUnit();
            BigDecimal value = point.getValue();
            if (warningService.matchRuleCode(point.getIndicatorCode(), value) != null) {
                warningCount++;
            }
            // 复用既有链路：命中规则则生成预警并把该条监测标记为异常
            warningService.checkAndCreateWarning(patientId, patient.getName(),
                    point.getIndicatorCode(), value, unit, measure.getMeasureId());
        }

        LocalDateTime now = LocalDateTime.now();
        deviceMapper.update(null, Wrappers.<CdDevice>lambdaUpdate()
                .eq(CdDevice::getDeviceId, device.getDeviceId())
                .set(CdDevice::getLastSyncTime, now)
                .set(CdDevice::getLastOnline, now)
                .set(CdDevice::getOnlineStatus, "1"));

        Integer scoreBefore = latestScore(patientId);
        Integer scoreAfter = scoreBefore;
        try {
            scoreService.calcScore(patientId);
            scoreAfter = latestScore(patientId);
        } catch (Exception e) {
            log.warn("模拟导入后重算评分失败：patientId={} err={}", patientId, e.getMessage());
        }

        SimImportResultVO vo = new SimImportResultVO();
        vo.setDeviceId(device.getDeviceId());
        vo.setDeviceName(deviceDisplayName(device));
        vo.setHours(hours);
        vo.setTotalRows(points.size());
        vo.setSuccessRows(success);
        vo.setSkippedRows(skipped);
        vo.setRangeStart(from);
        vo.setRangeEnd(to);
        vo.setIndicatorNames(codes.stream().map(SimMeasureFactory::nameOf).collect(Collectors.toList()));
        vo.setWarningCount(warningCount);
        vo.setScoreBefore(scoreBefore);
        vo.setScoreAfter(scoreAfter);
        log.info("模拟导入完成：patientId={} deviceId={} indicators={} range={}~{} 写入{}条 跳过{}条",
                patientId, device.getDeviceId(), codes, from, to, success, skipped);
        return vo;
    }

    /** 设备类型 -> 可导入指标 */
    private List<String> indicatorsOf(String deviceType) {
        if (TYPE_BP.equals(deviceType)) {
            return SimMeasureFactory.BP_INDICATORS;
        }
        if (TYPE_WATCH.equals(deviceType) || "3".equals(deviceType)) {
            return SimMeasureFactory.WATCH_INDICATORS;
        }
        return Collections.emptyList();
    }

    private CdDevice requireBoundDevice(Long patientId, Long deviceId) {
        if (deviceId == null) {
            throw new ServiceException("请选择设备");
        }
        CdDevice device = deviceMapper.selectById(deviceId);
        if (device == null || !Objects.equals(device.getPatientId(), patientId) || !"0".equals(device.getStatus())) {
            throw new ServiceException("设备不存在或未绑定该患者");
        }
        return device;
    }

    /** 对齐到整点，并校验：不晚于当前整点、至少 2 小时、最多 2 天 */
    private LocalDateTime[] normalizeRange(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new ServiceException("请选择时间范围");
        }
        LocalDateTime from = start.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime to = end.withMinute(0).withSecond(0).withNano(0);
        if (to.isBefore(from)) {
            throw new ServiceException("结束时间不能早于开始时间");
        }
        LocalDateTime currentHour = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        if (from.isAfter(currentHour) || to.isAfter(currentHour)) {
            throw new ServiceException("不能导入未来的数据");
        }
        long hours = Duration.between(from, to).toHours();
        if (hours < MIN_HOURS - 1) {
            throw new ServiceException("时间范围不能少于 2 小时");
        }
        if (hours > MAX_HOURS - 1) {
            throw new ServiceException("时间范围不能超过 2 天");
        }
        return new LocalDateTime[]{from, to};
    }

    private String deviceDisplayName(CdDevice device) {
        if (device.getAlias() != null && !device.getAlias().isBlank()) {
            return device.getAlias();
        }
        return device.getDeviceName() != null && !device.getDeviceName().isBlank()
                ? device.getDeviceName() : device.getDeviceNo();
    }

    /** 评分计算结果会回写患者档案的 risk_score */
    private Integer latestScore(Long patientId) {
        CdPatient patient = patientMapper.selectById(patientId);
        return patient == null ? null : patient.getHealthScore();
    }
}