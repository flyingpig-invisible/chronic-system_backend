package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdPatientDept;
import com.chronic.modules.chronic.mapper.CdPatientDeptMapper;
import com.chronic.modules.chronic.service.CdPatientDeptService;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 患者可见科室实现
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class CdPatientDeptServiceImpl implements CdPatientDeptService {

    private final CdPatientDeptMapper patientDeptMapper;
    private final SysDeptMapper deptMapper;

    @Override
    public List<Long> deptIdsOf(Long patientId) {
        if (patientId == null) {
            return Collections.emptyList();
        }
        return patientDeptMapper.selectList(Wrappers.<CdPatientDept>lambdaQuery()
                        .eq(CdPatientDept::getPatientId, patientId)
                        .orderByAsc(CdPatientDept::getDeptId))
                .stream().map(CdPatientDept::getDeptId).collect(Collectors.toList());
    }

    @Override
    public Map<Long, List<Long>> deptIdsOfPatients(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (CdPatientDept row : patientDeptMapper.selectList(Wrappers.<CdPatientDept>lambdaQuery()
                .in(CdPatientDept::getPatientId, patientIds)
                .orderByAsc(CdPatientDept::getDeptId))) {
            result.computeIfAbsent(row.getPatientId(), k -> new ArrayList<>()).add(row.getDeptId());
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replace(Long patientId, List<Long> deptIds, String source) {
        List<Long> valid = validate(patientId, deptIds);
        patientDeptMapper.delete(Wrappers.<CdPatientDept>lambdaQuery()
                .eq(CdPatientDept::getPatientId, patientId));
        insert(patientId, valid, source);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void fillIfEmpty(Long patientId, List<Long> deptIds, String source) {
        if (patientId == null || deptIds == null || deptIds.isEmpty()) {
            return;
        }
        Long exists = patientDeptMapper.selectCount(Wrappers.<CdPatientDept>lambdaQuery()
                .eq(CdPatientDept::getPatientId, patientId));
        if (exists != null && exists > 0) {
            return;
        }
        insert(patientId, validate(patientId, deptIds), source);
    }

    @Override
    public void removeByPatients(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return;
        }
        patientDeptMapper.delete(Wrappers.<CdPatientDept>lambdaQuery()
                .in(CdPatientDept::getPatientId, patientIds));
    }

    @Override
    public Set<Long> patientIdsOfDept(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        return patientIdsOfDepts(List.of(deptId));
    }

    @Override
    public Set<Long> patientIdsOfDepts(Collection<Long> deptIds) {
        Set<Long> ids = deptIds == null ? Collections.emptySet()
                : deptIds.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptySet();
        }
        return patientDeptMapper.selectList(Wrappers.<CdPatientDept>lambdaQuery()
                        .select(CdPatientDept::getPatientId)
                        .in(CdPatientDept::getDeptId, ids))
                .stream().map(CdPatientDept::getPatientId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /** 去重后只保留存在且启用的科室ID（不校验非空，供注册暂存和校验使用） */
    @Override
    public List<Long> validDeptIds(Collection<Long> deptIds) {
        Set<Long> ids = deptIds == null ? new LinkedHashSet<>()
                : deptIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            return new ArrayList<>();
        }
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .in(SysDept::getDeptId, ids)
                .eq(SysDept::getStatus, "0"));
        if (depts.size() != ids.size()) {
            throw new ServiceException("所选科室不存在或已停用");
        }
        return new ArrayList<>(ids);
    }

    /** 去重并校验：科室必须存在且启用，至少选择一个 */
    private List<Long> validate(Long patientId, List<Long> deptIds) {
        if (patientId == null) {
            throw new ServiceException("缺少患者信息");
        }
        Set<Long> ids = deptIds == null ? new HashSet<>()
                : deptIds.stream().filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
        if (ids.isEmpty()) {
            throw new ServiceException("请至少选择一个科室");
        }
        List<SysDept> depts = deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .in(SysDept::getDeptId, ids)
                .eq(SysDept::getStatus, "0"));
        if (depts.size() != ids.size()) {
            throw new ServiceException("所选科室不存在或已停用");
        }
        return new ArrayList<>(ids);
    }

    private void insert(Long patientId, List<Long> deptIds, String source) {
        String src = source == null || source.isBlank() ? "1" : source;
        LocalDateTime now = LocalDateTime.now();
        for (Long deptId : deptIds) {
            CdPatientDept row = new CdPatientDept();
            row.setPatientId(patientId);
            row.setDeptId(deptId);
            row.setSource(src);
            row.setCreateTime(now);
            patientDeptMapper.insert(row);
        }
    }
}
