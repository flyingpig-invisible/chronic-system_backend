package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.mapper.CdMeasureMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdWarningMapper;
import com.chronic.modules.chronic.dto.MeasureEntryDTO;
import com.chronic.modules.chronic.query.MeasureQuery;
import com.chronic.modules.chronic.vo.MeasureGroupVO;
import com.chronic.modules.chronic.service.CdMeasureService;
import com.chronic.modules.chronic.service.CdScoreService;
import com.chronic.modules.chronic.service.CdWarningService;
import com.chronic.modules.chronic.service.PatientDataScopeService;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.service.SysIndicatorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 监测数据服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdMeasureServiceImpl implements CdMeasureService {

    private final CdMeasureMapper measureMapper;
    private final CdPatientMapper patientMapper;
    private final CdWarningMapper warningMapper;
    private final CdWarningService warningService;
    private final CdScoreService scoreService;
    private final SysIndicatorService indicatorService;
    private final PatientDataScopeService dataScopeService;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public PageResult<CdMeasure> pageMeasure(MeasureQuery query) {
        // 数据范围：医生只能查自己科室可见患者的监测数据；管理员不受限制
        Set<Long> visiblePatientIds = dataScopeService.visiblePatientIds();
        if (visiblePatientIds != null) {
            if (visiblePatientIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            if (query.getPatientId() != null) {
                dataScopeService.checkAccess(query.getPatientId());
            }
        }
        Page<CdMeasure> page = measureMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), buildWrapper(query));
        return PageResult.of(page);
    }

    @Override
    public CdMeasure getMeasure(Long measureId) {
        CdMeasure measure = measureMapper.selectById(measureId);
        if (measure == null) {
            throw new ServiceException("监测记录不存在");
        }
        return measure;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addMeasure(CdMeasure measure) {
        if (measure.getPatientId() == null) {
            throw new ServiceException("患者ID不能为空");
        }
        if (!StringUtils.hasText(measure.getIndicatorCode())) {
            throw new ServiceException("指标编码不能为空");
        }
        if (measure.getValue() == null) {
            throw new ServiceException("指标数值不能为空");
        }

        CdPatient patient = patientMapper.selectById(measure.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        measure.setPatientName(patient.getName());
        if (measure.getMeasureTime() == null) {
            measure.setMeasureTime(LocalDateTime.now());
        }
        if (!StringUtils.hasText(measure.getDataSource())) {
            measure.setDataSource("2");
        }
        if (!StringUtils.hasText(measure.getAbnormalFlag())) {
            measure.setAbnormalFlag("0");
        }
        // 名称/单位按指标字典补齐：医生端手动录入不传名称，留空会让各端展示出现空白
        for (String[] meta : ALL_INDICATORS) {
            if (meta[0].equals(measure.getIndicatorCode())) {
                if (!StringUtils.hasText(measure.getIndicatorName())) {
                    measure.setIndicatorName(meta[1]);
                }
                if (!StringUtils.hasText(measure.getUnit())) {
                    measure.setUnit(meta[2]);
                }
                break;
            }
        }

        measureMapper.insert(measure);

        // 更新患者最近监测时间
        CdPatient update = new CdPatient();
        update.setPatientId(patient.getPatientId());
        update.setLastMeasureTime(measure.getMeasureTime());
        patientMapper.updateById(update);

        // 手工录入同样做预警判定：命中规则立即生成预警并标记该条监测异常
        warningService.checkAndCreateWarning(patient.getPatientId(), patient.getName(),
                measure.getIndicatorCode(), measure.getValue(), measure.getUnit(), measure.getMeasureId());

        // 重新计算评分：评分 = 指标风险加权分 + 预警命中加分，并同步回写患者档案
        // 放到事务提交之后执行，保证重算时能读到刚写入的监测与预警数据
        Long patientId = patient.getPatientId();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    calcAndSyncScoreQuietly(patientId);
                }
            });
        } else {
            calcAndSyncScoreQuietly(patientId);
        }

        return measure.getMeasureId();
    }

    /** 录入后重算评分，失败不影响监测数据保存 */
    private void calcAndSyncScoreQuietly(Long patientId) {
        try {
            scoreService.calcScore(patientId);
        } catch (Exception e) {
            log.warn("患者[{}] 录入监测后重算评分失败：{}", patientId, e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchAdd(List<CdMeasure> measures) {
        if (measures == null || measures.isEmpty()) {
            throw new ServiceException("上报数据不能为空");
        }
        for (CdMeasure m : measures) {
            addMeasure(m);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> addMeasureOnce(Long patientId, String diseaseCode, LocalDateTime measureTime,
                                     String dataSource, List<MeasureEntryDTO> entries) {
        if (patientId == null) {
            throw new ServiceException("患者ID不能为空");
        }
        if (entries == null || entries.isEmpty()) {
            throw new ServiceException("请至少录入一项指标");
        }
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        LocalDateTime time = measureTime == null ? LocalDateTime.now() : measureTime;
        String source = StringUtils.hasText(dataSource) ? dataSource : "2";

        // 一次测量内的指标不允许重复，否则会撞 (patient_id, indicator_code, measure_time) 唯一键
        Set<String> seen = new LinkedHashSet<>();
        for (MeasureEntryDTO entry : entries) {
            if (entry == null || !StringUtils.hasText(entry.getIndicatorCode())) {
                throw new ServiceException("请选择指标");
            }
            if (entry.getValue() == null) {
                throw new ServiceException("请输入「" + entry.getIndicatorCode() + "」的数值");
            }
            if (!seen.add(entry.getIndicatorCode())) {
                throw new ServiceException("同一指标不能重复录入");
            }
        }

        // 指标名称、单位一律取指标字典，字典中没有的编码直接拒绝，避免手工拼出的"野生指标"
        Map<String, SysIndicator> dictionary = indicatorService.listEnabled().stream()
                .collect(Collectors.toMap(SysIndicator::getIndicatorCode, Function.identity(), (a, b) -> a));

        List<Long> ids = new ArrayList<>();
        for (MeasureEntryDTO entry : entries) {
            SysIndicator indicator = dictionary.get(entry.getIndicatorCode());
            if (indicator == null) {
                throw new ServiceException("指标「" + entry.getIndicatorCode() + "」不在指标字典中");
            }
            CdMeasure measure = new CdMeasure();
            measure.setPatientId(patientId);
            measure.setPatientName(patient.getName());
            measure.setDiseaseCode(StringUtils.hasText(diseaseCode) ? diseaseCode : null);
            measure.setIndicatorCode(indicator.getIndicatorCode());
            measure.setIndicatorName(indicator.getIndicatorName());
            measure.setUnit(indicator.getUnit());
            measure.setValue(entry.getValue());
            measure.setMeasureTime(time);
            measure.setDataSource(source);
            measure.setAbnormalFlag("0");
            measureMapper.insert(measure);
            ids.add(measure.getMeasureId());

            warningService.checkAndCreateWarning(patientId, patient.getName(), indicator.getIndicatorCode(),
                    entry.getValue(), indicator.getUnit(), measure.getMeasureId());
        }

        CdPatient update = new CdPatient();
        update.setPatientId(patientId);
        update.setLastMeasureTime(time);
        patientMapper.updateById(update);

        // 多个指标一次录入，只在本组全部写入后重算一次评分
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    scoreService.calcScore(patientId);
                } catch (Exception e) {
                    log.warn("录入后重算评分失败：patientId={} err={}", patientId, e.getMessage());
                }
            }
        });
        return ids;
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteMeasure(Long[] measureIds) {
        for (Long id : measureIds) {
            measureMapper.deleteById(id);
        }
    }

    @Override
    public List<CdMeasure> listForExport(MeasureQuery query) {
        query.setPageNum(1);
        query.setPageSize(10000);
        return measureMapper.selectList(buildWrapper(query));
    }

    private LambdaQueryWrapper<CdMeasure> buildWrapper(MeasureQuery query) {
        Set<Long> visiblePatientIds = dataScopeService.visiblePatientIds();
        return Wrappers.<CdMeasure>lambdaQuery()
                .eq(query.getPatientId() != null, CdMeasure::getPatientId, query.getPatientId())
                .like(StringUtils.hasText(query.getPatientName()), CdMeasure::getPatientName, query.getPatientName())
                .eq(query.getDeviceId() != null, CdMeasure::getDeviceId, query.getDeviceId())
                .eq(StringUtils.hasText(query.getIndicatorCode()), CdMeasure::getIndicatorCode, query.getIndicatorCode())
                .eq(StringUtils.hasText(query.getAbnormalFlag()), CdMeasure::getAbnormalFlag, query.getAbnormalFlag())
                .ge(StringUtils.hasText(query.getBeginTime()), CdMeasure::getMeasureTime,
                        StringUtils.hasText(query.getBeginTime()) ? LocalDateTime.parse(query.getBeginTime(), DTF) : null)
                .le(StringUtils.hasText(query.getEndTime()), CdMeasure::getMeasureTime,
                        StringUtils.hasText(query.getEndTime()) ? LocalDateTime.parse(query.getEndTime(), DTF) : null)
                .in(query.getPatientId() == null && visiblePatientIds != null, CdMeasure::getPatientId, visiblePatientIds)
                .orderByDesc(CdMeasure::getMeasureTime);
    }

    /** 可展示的全部指标，顺序固定，未录入的也要占一行 */
    private static final String[][] ALL_INDICATORS = {
            {"spo2", "血氧", "%"},
            {"glu", "血糖", "mmol/L"},
            {"sbp", "收缩压", "mmHg"},
            {"dbp", "舒张压", "mmHg"},
            {"hr", "心率", "次/分"},
            {"rr", "呼吸频率", "次/分"},
            {"sleep", "睡眠时长", "小时"}
    };

    @Override
    public PageResult<MeasureGroupVO> pageGrouped(MeasureQuery query) {
        query.setPageNum(query.getPageNum());
        List<CdMeasure> all = measureMapper.selectList(buildWrapperForGroup(query));
        // 按 患者 + 分钟级时间 聚合
        Map<String, MeasureGroupVO> groups = new LinkedHashMap<>();
        Map<String, Map<String, CdMeasure>> groupItems = new LinkedHashMap<>();
        for (CdMeasure m : all) {
            String time = m.getMeasureTime() == null ? "" : m.getMeasureTime().format(DTF);
            String key = m.getPatientId() + "_" + time;
            MeasureGroupVO g = groups.get(key);
            if (g == null) {
                g = new MeasureGroupVO();
                g.setGroupKey(key);
                g.setPatientId(m.getPatientId());
                g.setPatientName(m.getPatientName());
                g.setMeasureTime(time);
                g.setDataSource(m.getDataSource());
                groups.put(key, g);
            }
            groupItems.computeIfAbsent(key, k -> new java.util.HashMap<>())
                    .put(m.getIndicatorCode(), m);
            g.getMeasureIds().add(m.getMeasureId());
            if ("1".equals(m.getAbnormalFlag())) {
                g.setHasAbnormal(true);
                g.setAbnormalCount(g.getAbnormalCount() + 1);
            }
        }

        // 组装指标行：固定 7 项顺序，缺失补空
        for (Map.Entry<String, MeasureGroupVO> entry : groups.entrySet()) {
            MeasureGroupVO g = entry.getValue();
            Map<String, CdMeasure> byCode = groupItems.get(entry.getKey());
            if (byCode == null) {
                byCode = new java.util.HashMap<>();
            }
            for (String[] meta : ALL_INDICATORS) {
                MeasureGroupVO.Item item = new MeasureGroupVO.Item();
                item.setIndicatorCode(meta[0]);
                item.setIndicatorName(meta[1]);
                item.setUnit(meta[2]);
                CdMeasure m = byCode.get(meta[0]);
                if (m != null) {
                    item.setMeasureId(m.getMeasureId());
                    item.setValue(m.getValue() == null ? null
                            : m.getValue().stripTrailingZeros().toPlainString());
                    item.setAbnormalFlag(m.getAbnormalFlag());
                }
                g.getItems().add(item);
            }
        }

        List<MeasureGroupVO> list = new ArrayList<>(groups.values());
        long total = list.size();
        int from = Math.max(0, (query.getPageNum() - 1) * query.getPageSize());
        int to = Math.min(list.size(), from + query.getPageSize());
        List<MeasureGroupVO> page = from >= list.size()
                ? new ArrayList<>() : new ArrayList<>(list.subList(from, to));
        return PageResult.of(total, page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteGroup(Long patientId, String measureTime) {
        if (patientId == null || !StringUtils.hasText(measureTime)) {
            throw new ServiceException("参数不完整");
        }
        LocalDateTime time = LocalDateTime.parse(measureTime.replace('T', ' '), DTF);
        measureMapper.delete(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, patientId)
                .eq(CdMeasure::getMeasureTime, time));
    }

    /** 分组查询排序：患者升序 + 时间倒序，保证同一患者的多次测量连续 */
    private LambdaQueryWrapper<CdMeasure> buildWrapperForGroup(MeasureQuery query) {
        return Wrappers.<CdMeasure>lambdaQuery()
                .eq(query.getPatientId() != null, CdMeasure::getPatientId, query.getPatientId())
                .like(StringUtils.hasText(query.getPatientName()), CdMeasure::getPatientName, query.getPatientName())
                .eq(StringUtils.hasText(query.getIndicatorCode()), CdMeasure::getIndicatorCode, query.getIndicatorCode())
                .eq(StringUtils.hasText(query.getAbnormalFlag()), CdMeasure::getAbnormalFlag, query.getAbnormalFlag())
                .ge(StringUtils.hasText(query.getBeginTime()), CdMeasure::getMeasureTime,
                        StringUtils.hasText(query.getBeginTime()) ? LocalDateTime.parse(query.getBeginTime(), DTF) : null)
                .le(StringUtils.hasText(query.getEndTime()), CdMeasure::getMeasureTime,
                        StringUtils.hasText(query.getEndTime()) ? LocalDateTime.parse(query.getEndTime(), DTF) : null)
                .orderByDesc(CdMeasure::getMeasureTime)
                .orderByAsc(CdMeasure::getPatientId);
    }
}
