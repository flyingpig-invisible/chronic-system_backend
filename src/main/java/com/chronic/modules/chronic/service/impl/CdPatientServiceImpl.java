package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.*;
import com.chronic.modules.chronic.mapper.*;
import com.chronic.modules.chronic.query.PatientQuery;
import com.chronic.modules.chronic.service.CdPatientService;
import com.chronic.modules.chronic.vo.DashboardVO;
import com.chronic.modules.chronic.vo.PatientVO;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.modules.system.service.DeptResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 患者服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdPatientServiceImpl implements CdPatientService {

    private final CdPatientMapper patientMapper;
    private final CdPatientDiseaseMapper patientDiseaseMapper;
    private final CdMeasureMapper measureMapper;
    private final CdWarningMapper warningMapper;
    private final CdHealthScoreMapper healthScoreMapper;
    private final SysUserMapper userMapper;
    private final DeptResolver deptResolver;

    /** 病种编码 -> 名称 */
    private static final Map<String, String> DISEASE_NAME = Map.of(
            "1", "高血压",
            "2", "2型糖尿病",
            "3", "冠心病",
            "4", "慢阻肺"
    );

    @Override
    public PageResult<PatientVO> pagePatient(PatientQuery query) {
        LambdaQueryWrapper<CdPatient> wrapper = Wrappers.<CdPatient>lambdaQuery();

        // 关键字：姓名或手机号任一模糊匹配
        String keyword = query.getKeyword();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(CdPatient::getName, keyword)
                    .or().like(CdPatient::getPhone, keyword)
                    .or().like(CdPatient::getPatientNo, keyword));
        } else {
            wrapper.like(StringUtils.hasText(query.getName()), CdPatient::getName, query.getName())
                   .like(StringUtils.hasText(query.getPhone()), CdPatient::getPhone, query.getPhone())
                   .like(StringUtils.hasText(query.getPatientNo()), CdPatient::getPatientNo, query.getPatientNo());
        }

        wrapper.eq(StringUtils.hasText(query.getRiskLevel()), CdPatient::getRiskLevel, query.getRiskLevel())
               .eq(StringUtils.hasText(query.getStatus()), CdPatient::getStatus, query.getStatus())
               .eq(query.getDoctorId() != null, CdPatient::getDoctorId, query.getDoctorId());

        // 科室筛选：以责任医生所在科室为准，先把科室展开为医生集合
        if (query.getDeptId() != null) {
            Set<Long> doctorIds = deptResolver.userIdsOfDept(query.getDeptId());
            if (doctorIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            wrapper.in(CdPatient::getDoctorId, doctorIds);
        }

        // 病种筛选
        if (query.getDiseaseCodes() != null && !query.getDiseaseCodes().isEmpty()) {
            List<Long> patientIds = patientDiseaseMapper.selectList(
                            Wrappers.<CdPatientDisease>lambdaQuery()
                                    .in(CdPatientDisease::getDiseaseCode, query.getDiseaseCodes()))
                    .stream().map(CdPatientDisease::getPatientId).distinct().collect(Collectors.toList());
            if (patientIds.isEmpty()) {
                return PageResult.of(0L, new ArrayList<>());
            }
            wrapper.in(CdPatient::getPatientId, patientIds);
        }

        // 按风险等级倒序（高危优先），再按评分倒序
        wrapper.orderByDesc(CdPatient::getRiskLevel)
                .orderByDesc(CdPatient::getRiskScore)
                .orderByAsc(CdPatient::getPatientId);

        Page<CdPatient> page = patientMapper.selectPage(
                new Page<>(query.getPageNum(), query.getPageSize()), wrapper);

        List<PatientVO> vos = toVOList(page.getRecords());
        return PageResult.of(page.getTotal(), vos);
    }

    @Override
    public PatientVO getPatientDetail(Long patientId) {
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        List<PatientVO> list = toVOList(List.of(patient));
        PatientVO vo = list.get(0);
        // 风险等级/风险评分以最近一次评分记录为准
        CdHealthScore latest = healthScoreMapper.selectOne(Wrappers.<CdHealthScore>lambdaQuery()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .orderByDesc(CdHealthScore::getScoreId)
                .last("limit 1"));
        if (latest != null) {
            vo.setRiskScore(latest.getTotalScore());
            vo.setRiskLevel(latest.getRiskLevel());
        }

        // 填充病种编码
        List<CdPatientDisease> diseases = patientDiseaseMapper.selectList(
                Wrappers.<CdPatientDisease>lambdaQuery().eq(CdPatientDisease::getPatientId, patientId));
        vo.setDiseaseCodes(diseases.stream().map(CdPatientDisease::getDiseaseCode).collect(Collectors.toList()));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addPatient(CdPatient patient) {
        // 患者编号唯一性校验
        if (StringUtils.hasText(patient.getPatientNo())) {
            Long count = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                    .eq(CdPatient::getPatientNo, patient.getPatientNo()));
            if (count > 0) {
                throw new ServiceException("患者编号已存在");
            }
        } else {
            patient.setPatientNo(generatePatientNo());
        }

        // 科室跟随责任医生，忽略前端传入值
        patient.setDeptId(deptResolver.deptIdOfDoctor(patient.getDoctorId()));

        // 默认值
        if (!StringUtils.hasText(patient.getRiskLevel())) {
            patient.setRiskLevel("1");
        }
        if (!StringUtils.hasText(patient.getStatus())) {
            patient.setStatus("0");
        }
        if (patient.getRiskScore() == null) {
            patient.setRiskScore(0);
        }
        if (patient.getPendingWarningCount() == null) {
            patient.setPendingWarningCount(0);
        }

        patientMapper.insert(patient);
        savePatientDisease(patient.getPatientId(), patient.getDiseaseCodes());
        return patient.getPatientId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePatient(CdPatient patient) {
        if (patient.getPatientId() == null) {
            throw new ServiceException("患者ID不能为空");
        }
        if (StringUtils.hasText(patient.getPatientNo())) {
            Long count = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                    .eq(CdPatient::getPatientNo, patient.getPatientNo())
                    .ne(CdPatient::getPatientId, patient.getPatientId()));
            if (count > 0) {
                throw new ServiceException("患者编号已存在");
            }
        }
        // 科室跟随责任医生，忽略前端传入值
        patient.setDeptId(deptResolver.deptIdOfDoctor(patient.getDoctorId()));
        patientMapper.updateById(patient);

        // 病种变动则重建关联
        if (patient.getDiseaseCodes() != null) {
            patientDiseaseMapper.delete(Wrappers.<CdPatientDisease>lambdaQuery()
                    .eq(CdPatientDisease::getPatientId, patient.getPatientId()));
            savePatientDisease(patient.getPatientId(), patient.getDiseaseCodes());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePatient(Long[] patientIds) {
        patientMapper.deleteByIds(Arrays.asList(patientIds));
        patientDiseaseMapper.delete(Wrappers.<CdPatientDisease>lambdaQuery()
                .in(CdPatientDisease::getPatientId, Arrays.asList(patientIds)));
    }

    @Override
    public DashboardVO getDashboard(Long patientId) {
        DashboardVO vo = new DashboardVO();
        vo.setPatient(getPatientDetail(patientId));

        // 最新一次监测的各指标
        List<CdMeasure> measures = measureMapper.selectList(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime));
        Map<String, CdMeasure> latest = new LinkedHashMap<>();
        for (CdMeasure m : measures) {
            latest.putIfAbsent(m.getIndicatorCode(), m);
        }
        List<Map<String, Object>> latestList = new ArrayList<>();
        for (CdMeasure m : latest.values()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("indicatorCode", m.getIndicatorCode());
            item.put("indicatorName", m.getIndicatorName());
            item.put("value", m.getValue());
            item.put("unit", m.getUnit());
            item.put("measureTime", m.getMeasureTime());
            item.put("abnormalFlag", m.getAbnormalFlag());
            latestList.add(item);
        }
        vo.setLatestMeasures(latestList);

        // 预警统计
        List<CdWarning> warnings = warningMapper.selectList(Wrappers.<CdWarning>lambdaQuery()
                .eq(CdWarning::getPatientId, patientId));
        long high = warnings.stream().filter(w -> "3".equals(w.getWarningLevel())).count();
        long middle = warnings.stream().filter(w -> "2".equals(w.getWarningLevel())).count();
        long low = warnings.stream().filter(w -> "1".equals(w.getWarningLevel())).count();
        long pending = warnings.stream().filter(w -> "0".equals(w.getHandleStatus())).count();
        Map<String, Object> stat = new LinkedHashMap<>();
        stat.put("high", high);
        stat.put("middle", middle);
        stat.put("low", low);
        stat.put("pending", pending);
        stat.put("total", warnings.size());
        vo.setWarningStat(stat);

        // 最近评分
        CdHealthScore score = healthScoreMapper.selectOne(Wrappers.<CdHealthScore>lambdaQuery()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .last("limit 1"));
        if (score != null) {
            vo.setRiskScore(score.getTotalScore());
            vo.setRiskLevel(score.getRiskLevel());
        } else {
            vo.setRiskScore(vo.getPatient().getRiskScore());
            vo.setRiskLevel(vo.getPatient().getRiskLevel());
        }
        return vo;
    }

    @Override
    public List<Map<String, Object>> getMeasureTrend(Long patientId, String indicatorCode,
                                                     String beginTime, String endTime) {
        List<CdMeasure> list = measureMapper.selectList(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, patientId)
                .eq(StringUtils.hasText(indicatorCode), CdMeasure::getIndicatorCode, indicatorCode)
                .ge(StringUtils.hasText(beginTime), CdMeasure::getMeasureTime,
                        StringUtils.hasText(beginTime) ? LocalDateTime.parse(beginTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .le(StringUtils.hasText(endTime), CdMeasure::getMeasureTime,
                        StringUtils.hasText(endTime) ? LocalDateTime.parse(endTime, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null)
                .orderByAsc(CdMeasure::getMeasureTime));

        List<Map<String, Object>> result = new ArrayList<>();
        for (CdMeasure m : list) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("indicatorCode", m.getIndicatorCode());
            item.put("indicatorName", m.getIndicatorName());
            item.put("value", m.getValue());
            item.put("unit", m.getUnit());
            item.put("measureTime", m.getMeasureTime());
            item.put("abnormalFlag", m.getAbnormalFlag());
            result.add(item);
        }
        return result;
    }

    @Override
    public List<Map<String, String>> diseaseOptions() {
        List<Map<String, String>> options = new ArrayList<>();
        DISEASE_NAME.forEach((code, name) -> {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("label", name);
            item.put("value", code);
            options.add(item);
        });
        return options;
    }

    @Override
    public List<PatientVO> listForExport(PatientQuery query) {
        query.setPageNum(1);
        query.setPageSize(10000);
        return pagePatient(query).getRows();
    }

    // ==================== 私有方法 ====================

    /** 实体列表转 VO 列表，并填充病种 */
    private List<PatientVO> toVOList(List<CdPatient> patients) {
        List<PatientVO> vos = new ArrayList<>();
        if (patients.isEmpty()) {
            return vos;
        }

        // 批量查询病种
        List<Long> patientIds = patients.stream().map(CdPatient::getPatientId).collect(Collectors.toList());
        List<CdPatientDisease> allDiseases = patientDiseaseMapper.selectList(
                Wrappers.<CdPatientDisease>lambdaQuery().in(CdPatientDisease::getPatientId, patientIds));
        Map<Long, List<String>> diseaseMap = new HashMap<>();
        for (CdPatientDisease d : allDiseases) {
            String name = d.getDiseaseName() != null && !d.getDiseaseName().isEmpty()
                    ? d.getDiseaseName()
                    : DISEASE_NAME.getOrDefault(d.getDiseaseCode(), "未知");
            diseaseMap.computeIfAbsent(d.getPatientId(), k -> new ArrayList<>()).add(name);
        }

        // 批量补全责任医生姓名与科室名称
        Set<Long> doctorIds = patients.stream().map(CdPatient::getDoctorId)
                .filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> doctorDeptMap = new HashMap<>();
        Map<Long, String> doctorNameMap = new HashMap<>();
        if (!doctorIds.isEmpty()) {
            for (SysUser u : userMapper.selectBatchIds(doctorIds)) {
                doctorNameMap.put(u.getUserId(), u.getRealName());
                doctorDeptMap.put(u.getUserId(), u.getDeptId());
            }
        }
        Map<Long, String> deptNameMap = deptResolver.deptNames(doctorDeptMap.values());

        Map<Long, CdHealthScore> latestScoreMap = latestScores(patientIds);

        for (CdPatient p : patients) {
            PatientVO vo = new PatientVO();
            BeanUtils.copyProperties(p, vo);
            CdHealthScore latestScore = latestScoreMap.get(p.getPatientId());
            if (latestScore != null) {
                vo.setRiskScore(latestScore.getTotalScore());
                vo.setRiskLevel(latestScore.getRiskLevel());
            }
            vo.setDoctorName(doctorNameMap.get(p.getDoctorId()));
            Long ownerDeptId = doctorDeptMap.get(p.getDoctorId());
            vo.setDeptId(ownerDeptId);
            vo.setDeptName(ownerDeptId == null ? null : deptNameMap.get(ownerDeptId));
            vo.setAge(calcAge(p.getBirthday()));
            vo.setDiseases(diseaseMap.getOrDefault(p.getPatientId(), new ArrayList<>()));
            vos.add(vo);
        }
        return vos;
    }

    /** 批量取每个患者最近一次评分（风险等级以此为准） */
    private Map<Long, CdHealthScore> latestScores(List<Long> patientIds) {
        Map<Long, CdHealthScore> result = new HashMap<>();
        if (patientIds == null || patientIds.isEmpty()) {
            return result;
        }
        List<CdHealthScore> scores = healthScoreMapper.selectList(Wrappers.<CdHealthScore>lambdaQuery()
                .in(CdHealthScore::getPatientId, patientIds)
                .orderByDesc(CdHealthScore::getCalcTime)
                .orderByDesc(CdHealthScore::getScoreId));
        for (CdHealthScore score : scores) {
            result.putIfAbsent(score.getPatientId(), score);
        }
        return result;
    }

    /** 保存患者病种关联 */
    private void savePatientDisease(Long patientId, List<String> diseaseCodes) {
        if (patientId == null || diseaseCodes == null || diseaseCodes.isEmpty()) {
            return;
        }
        for (String code : diseaseCodes) {
            CdPatientDisease pd = new CdPatientDisease();
            pd.setPatientId(patientId);
            pd.setDiseaseCode(code);
            pd.setDiseaseName(DISEASE_NAME.getOrDefault(code, "未知"));
            pd.setSeverity("1");
            pd.setCreateTime(LocalDateTime.now());
            patientDiseaseMapper.insert(pd);
        }
    }

    /** 计算年龄 */
    private Integer calcAge(LocalDate birthday) {
        if (birthday == null) {
            return null;
        }
        return Period.between(birthday, LocalDate.now()).getYears();
    }

    /** 生成患者编号 */
    private String generatePatientNo() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = System.currentTimeMillis() % 10000;
        return String.format("P%s%04d", datePart, seq);
    }

    @Override
    public java.util.Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        Long patientTotal = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery());
        Long highRisk = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                .eq(CdPatient::getRiskLevel, "3"));
        Long midRisk = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                .eq(CdPatient::getRiskLevel, "2"));
        Long lowRisk = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                .eq(CdPatient::getRiskLevel, "1"));
        Long pendingWarning = warningMapper.selectCount(Wrappers.<CdWarning>lambdaQuery()
                .eq(CdWarning::getHandleStatus, "0"));
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Long todayMeasure = measureMapper.selectCount(Wrappers.<CdMeasure>lambdaQuery()
                .ge(CdMeasure::getMeasureTime, todayStart));

        result.put("patientTotal", patientTotal);
        result.put("highRiskCount", highRisk);
        result.put("midRiskCount", midRisk);
        result.put("lowRiskCount", lowRisk);
        result.put("pendingWarningCount", pendingWarning);
        result.put("todayMeasureCount", todayMeasure);

        List<CdPatient> highRiskList = patientMapper.selectList(Wrappers.<CdPatient>lambdaQuery()
                .eq(CdPatient::getRiskLevel, "3")
                .orderByDesc(CdPatient::getRiskScore)
                .last("limit 10"));
        List<Map<String, Object>> list = new ArrayList<>();
        for (CdPatient p : highRiskList) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("patientId", p.getPatientId());
            m.put("name", p.getName());
            m.put("gender", p.getGender());
            m.put("phone", p.getPhone());
            m.put("riskScore", p.getRiskScore());
            m.put("pendingWarningCount", p.getPendingWarningCount());
            list.add(m);
        }
        result.put("highRiskPatients", list);
        return result;
    }

    @Override
    public java.util.Map<String, Object> getProfile(Long patientId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("patient", getPatientDetail(patientId));

        List<CdMeasure> latest = measureMapper.selectList(Wrappers.<CdMeasure>lambdaQuery()
                .eq(CdMeasure::getPatientId, patientId)
                .orderByDesc(CdMeasure::getMeasureTime)
                .last("limit 20"));
        Set<String> seen = new HashSet<>();
        List<Map<String, Object>> indicators = new ArrayList<>();
        for (CdMeasure m : latest) {
            if (seen.contains(m.getIndicatorCode())) {
                continue;
            }
            seen.add(m.getIndicatorCode());
            Map<String, Object> i = new LinkedHashMap<>();
            i.put("indicatorCode", m.getIndicatorCode());
            i.put("indicatorName", m.getIndicatorName());
            i.put("value", m.getValue());
            i.put("unit", m.getUnit());
            i.put("measureTime", m.getMeasureTime());
            i.put("abnormalFlag", m.getAbnormalFlag());
            indicators.add(i);
        }
        result.put("indicators", indicators);

        List<CdHealthScore> scores = healthScoreMapper.selectList(Wrappers.<CdHealthScore>lambdaQuery()
                .eq(CdHealthScore::getPatientId, patientId)
                .orderByDesc(CdHealthScore::getCalcTime)
                .last("limit 1"));
        if (!scores.isEmpty()) {
            CdHealthScore sc = scores.get(0);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("totalScore", sc.getTotalScore());
            s.put("riskLevel", sc.getRiskLevel());
            s.put("riskLevelName", "3".equals(sc.getRiskLevel()) ? "高危" : ("2".equals(sc.getRiskLevel()) ? "中危" : "低危"));
            s.put("calcTime", sc.getCalcTime());
            result.put("healthScore", s);
        }
        return result;
    }
}
