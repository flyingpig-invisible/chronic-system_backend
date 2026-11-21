package com.chronic.modules.chronic.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.DeviceImportParam;
import com.chronic.modules.chronic.entity.CdDevice;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdImportLog;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdDeviceMapper;
import com.chronic.modules.chronic.mapper.CdImportLogMapper;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.modules.chronic.service.HealthImportService;
import com.chronic.modules.chronic.support.huawei.HealthPoint;
import com.chronic.modules.chronic.support.huawei.HuaweiExportParser;
import com.chronic.modules.chronic.support.huawei.ImportIssue;
import com.chronic.modules.chronic.support.huawei.ParseResult;
import com.chronic.modules.chronic.vo.ImportResultVO;
import com.chronic.modules.miniapp.service.MpDataService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 华为运动健康导出包导入实现
 * <p>
 * 解析在事务外完成（可能耗时数秒），只有落库阶段开启事务
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthImportServiceImpl implements HealthImportService {

    /** 上传文件大小上限，与 application.yml 的 max-file-size 保持一致 */
    private static final long MAX_FILE_BYTES = 20L * 1024 * 1024;

    private final HuaweiExportParser parser;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final CdPatientMapper patientMapper;
    private final CdMeasureMapper measureMapper;
    private final CdDeviceMapper deviceMapper;
    private final CdImportLogMapper importLogMapper;
    private final CdWarningService warningService;
    private final CdScoreService scoreService;
    private final MpDataService dataService;

    @Override
    public ImportResultVO importData(MultipartFile file, DeviceImportParam param) {
        if (param == null || param.getPatientId() == null) {
            throw new ServiceException("患者ID不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new ServiceException("请选择要导入的文件");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new ServiceException("文件超过 20MB，请缩小后重试");
        }
        Long patientId = param.getPatientId();
        checkMpPermission(patientId);
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new ServiceException("读取上传文件失败：" + e.getMessage());
        }
        String hash = DigestUtil.md5Hex(bytes);
        boolean dryRun = Boolean.TRUE.equals(param.getDryRun());
        boolean force = Boolean.TRUE.equals(param.getForce());

        Long duplicated = importLogMapper.selectCount(new LambdaQueryWrapper<CdImportLog>()
                .eq(CdImportLog::getPatientId, patientId)
                .eq(CdImportLog::getFileHash, hash));
        boolean duplicatedFile = duplicated != null && duplicated > 0;
        // 预览不做拦截，只标记；真正入库时才拦重复文件
        if (!dryRun && duplicatedFile && !force) {
            throw new ServiceException("该文件已于之前导入过，如需重复导入请勾选强制导入");
        }

        boolean aggregate = param.getAggregate() == null || param.getAggregate();
        ParseResult parsed = parser.parse(file.getOriginalFilename(), bytes, aggregate);

        ImportResultVO vo = buildBaseResult(parsed, file);
        vo.setDuplicateFile(duplicatedFile);
        if (dryRun) {
            vo.setDryRun(true);
            return vo;
        }

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.execute(status -> {
            persist(parsed, param, patient, file, hash, vo);
            return null;
        });
        return vo;
    }

    /** 落库阶段（事务内） */
    private void persist(ParseResult parsed, DeviceImportParam param, CdPatient patient,
                         MultipartFile file, String hash, ImportResultVO vo) {
        int success = 0;
        int duplicated = 0;
        boolean triggered = false;
        int maxLevel = 0;
        Set<Long> touchedDevices = new HashSet<>();

        for (HealthPoint point : parsed.getPoints()) {
            Long deviceId = resolveDevice(point.getDeviceCode(), param.getDeviceId());

            CdMeasure measure = new CdMeasure();
            measure.setPatientId(patient.getPatientId());
            measure.setPatientName(patient.getName());
            measure.setDeviceId(deviceId);
            measure.setIndicatorCode(point.getIndicatorCode());
            measure.setIndicatorName(point.getIndicatorName());
            measure.setValue(point.getValue());
            measure.setUnit(point.getUnit());
            measure.setMeasureTime(point.getMeasureTime());
            measure.setDataSource("3");
            measure.setAbnormalFlag("0");
            try {
                measureMapper.insert(measure);
            } catch (DuplicateKeyException e) {
                // 唯一索引 (patient_id, indicator_code, measure_time) 挡住了重复数据
                duplicated++;
                continue;
            }
            success++;
            if (deviceId != null) {
                touchedDevices.add(deviceId);
            }

            CdWarningRule matched = warningService.matchRuleCode(point.getIndicatorCode(), point.getValue());
            if (matched != null) {
                triggered = true;
                maxLevel = Math.max(maxLevel, parseIntSafe(matched.getWarningLevel()));
            }
            // 复用既有链路：异常标记、待处理预警数、风险等级
            warningService.checkAndCreateWarning(patient.getPatientId(), patient.getName(),
                    point.getIndicatorCode(), point.getValue(), point.getUnit(), measure.getMeasureId());
        }

        LocalDateTime now = LocalDateTime.now();
        for (Long deviceId : touchedDevices) {
            deviceMapper.update(null, new LambdaUpdateWrapper<CdDevice>()
                    .eq(CdDevice::getDeviceId, deviceId)
                    .set(CdDevice::getLastSyncTime, now)
                    .set(CdDevice::getOnlineStatus, "1")
                    .set(CdDevice::getLastOnline, now));
        }

        Integer scoreBefore = latestScore(patient.getPatientId());
        Integer scoreAfter = scoreBefore;
        try {
            scoreService.calcScore(patient.getPatientId());
            scoreAfter = latestScore(patient.getPatientId());
        } catch (Exception e) {
            log.warn("导入后重算评分失败：patientId={} err={}", patient.getPatientId(), e.getMessage());
        }

        int failed = Math.max(0, parsed.getPoints().size() - success);
        CdImportLog row = new CdImportLog();
        row.setPatientId(patient.getPatientId());
        row.setPatientName(patient.getName());
        row.setDeviceId(param.getDeviceId());
        row.setFileName(file.getOriginalFilename());
        row.setFileType(parsed.getFileType());
        row.setFileHash(hash);
        row.setFileCount(parsed.getFileCount());
        row.setRawRecords(parsed.getRawRecords());
        row.setParsedPoints(parsed.getParsedPoints());
        row.setTotalRows(parsed.getRawRecords());
        row.setSuccessRows(success);
        row.setFailedRows(failed);
        row.setSkipDupRows(duplicated);
        row.setErrorDetail(writeJson(parsed.getIssues()));
        row.setDataSource("3");
        row.setStatus(success <= 0 ? "3" : (failed == 0 ? "1" : "2"));
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user != null) {
            row.setOperatorUserId(user.getUserId());
            row.setOperatorName(StringUtils.hasText(user.getRealName()) ? user.getRealName() : user.getUsername());
        }
        row.setDateFrom(parsed.getDateFrom());
        row.setDateTo(parsed.getDateTo());
        importLogMapper.insert(row);

        vo.setImportId(row.getImportId());
        vo.setSuccessRows(success);
        vo.setSkippedDuplicate(duplicated);
        vo.setWarningTriggered(triggered);
        vo.setMaxWarningLevel(maxLevel == 0 ? null : String.valueOf(maxLevel));
        vo.setScoreBefore(scoreBefore);
        vo.setScoreAfter(scoreAfter);
        log.info("数据导入完成：patientId={} success={} dup={} warning={} score {}->{}",
                patient.getPatientId(), success, duplicated, triggered, scoreBefore, scoreAfter);
    }

    @Override
    public PageResult<CdImportLog> pageLogs(Long patientId, Integer pageNum, Integer pageSize) {
        Page<CdImportLog> page = importLogMapper.selectPage(
                new Page<>(pageNum == null || pageNum < 1 ? 1 : pageNum,
                        pageSize == null || pageSize < 1 ? 10 : pageSize),
                new LambdaQueryWrapper<CdImportLog>()
                        .eq(patientId != null, CdImportLog::getPatientId, patientId)
                        .orderByDesc(CdImportLog::getImportTime));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<ImportIssue> issues(Long importId) {
        CdImportLog row = importLogMapper.selectById(importId);
        if (row == null) {
            throw new ServiceException("导入记录不存在");
        }
        if (!StringUtils.hasText(row.getErrorDetail())) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(row.getErrorDetail(), new TypeReference<List<ImportIssue>>() {
            });
        } catch (Exception e) {
            log.warn("解析导入失败明细失败：importId={}", importId);
            return new ArrayList<>();
        }
    }

    // ------------------------------------------------------------------ 内部方法

    /** deviceCode -> 设备ID，不存在则自动建档 */
    private Long resolveDevice(String deviceCode, Long explicitDeviceId) {
        if (explicitDeviceId != null) {
            return explicitDeviceId;
        }
        if (!StringUtils.hasText(deviceCode)) {
            return null;
        }
        CdDevice device = deviceMapper.selectOne(new LambdaQueryWrapper<CdDevice>()
                .eq(CdDevice::getHwDeviceCode, deviceCode)
                .last("limit 1"));
        if (device != null) {
            return device.getDeviceId();
        }
        CdDevice created = new CdDevice();
        created.setDeviceNo("HW-" + deviceCode);
        created.setDeviceName("华为设备 " + deviceCode);
        created.setHwDeviceCode(deviceCode);
        created.setDeviceType("1");
        created.setVendor("HUAWEI");
        created.setModel("HUAWEI");
        created.setModelName("华为设备");
        created.setOnlineStatus("0");
        created.setStatus("0");
        deviceMapper.insert(created);
        log.info("按 deviceCode 自动建档：{} -> deviceId={}", deviceCode, created.getDeviceId());
        return created.getDeviceId();
    }

    private ImportResultVO buildBaseResult(ParseResult parsed, MultipartFile file) {
        ImportResultVO vo = new ImportResultVO();
        vo.setFileName(file.getOriginalFilename());
        vo.setFileType(parsed.getFileType());
        vo.setFileCount(parsed.getFileCount());
        vo.setRawRecords(parsed.getRawRecords());
        vo.setParsedPoints(parsed.getParsedPoints());
        vo.setSaved(parsed.getSaved());
        vo.setSuccessRows(parsed.getPoints().size());
        vo.setSkippedUnsupported(parsed.getUnsupportedPoints());
        vo.setSkippedInvalid(parsed.getInvalidPoints());
        vo.setSkippedDuplicate(0);
        vo.setDroppedSleepNights(parsed.getDroppedSleepNights());
        vo.setDateFrom(parsed.getDateFrom());
        vo.setDateTo(parsed.getDateTo());
        vo.setDeviceCodes(parsed.getDeviceCodes());
        vo.setIssues(parsed.getIssues());
        vo.setWarningTriggered(Boolean.FALSE);
        vo.setDryRun(Boolean.FALSE);
        return vo;
    }

    private Integer latestScore(Long patientId) {
        List<CdHealthScore> list = scoreService.list(new LambdaQueryWrapper<CdHealthScore>()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .last("limit 1"));
        return list.isEmpty() ? null : list.get(0).getTotalScore();
    }

    private String writeJson(List<ImportIssue> issues) {
        try {
            return objectMapper.writeValueAsString(issues);
        } catch (Exception e) {
            return "[]";
        }
    }

    private int parseIntSafe(String value) {
        try {
            return value == null ? 0 : Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** 小程序用户校验患者权限，系统用户（医生）放行 */
    private void checkMpPermission(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user != null && "mp".equals(user.getUserType())) {
            dataService.checkPermission(patientId);
        }
    }
}
