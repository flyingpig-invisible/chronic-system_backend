package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.service.CdPatientDeptService;
import com.chronic.modules.chronic.service.PatientDataScopeService;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 患者数据范围实现
 *
 * <p>可见口径（满足任一即可读）：
 * 1. 患者已勾选科室（cd_patient_dept）中包含当前医生所在科室；
 * 2. 当前医生是该患者的责任医生。
 * 科室以数据库实时值为准，不用 JWT 里的快照，管理员调整医生科室后立即生效。</p>
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PatientDataScopeServiceImpl implements PatientDataScopeService {

    private final SysUserMapper userMapper;
    private final CdPatientMapper patientMapper;
    private final CdPatientDeptService patientDeptService;

    @Override
    public Set<Long> visiblePatientIds() {
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        if (current == null) {
            return Collections.emptySet();
        }
        // 小程序端（患者本人/家属）有各自的归属校验，不走医生数据范围
        if ("mp".equals(current.getUserType())) {
            return null;
        }
        SysUser user = userMapper.selectById(current.getUserId());
        if (user == null) {
            return Collections.emptySet();
        }
        if ("2".equals(user.getUserType())) {
            return null;
        }
        Set<Long> ids = new LinkedHashSet<>();
        if (user.getDeptId() != null) {
            ids.addAll(patientDeptService.patientIdsOfDept(user.getDeptId()));
        }
        patientMapper.selectList(Wrappers.<CdPatient>lambdaQuery()
                        .select(CdPatient::getPatientId)
                        .eq(CdPatient::getDoctorId, user.getUserId()))
                .stream().map(CdPatient::getPatientId).forEach(ids::add);
        return ids;
    }

    @Override
    public boolean canAccess(Long patientId) {
        Set<Long> visible = visiblePatientIds();
        return visible == null || (patientId != null && visible.contains(patientId));
    }

    @Override
    public void checkAccess(Long patientId) {
        if (!canAccess(patientId)) {
            log.debug("越权访问患者数据：userId={} patientId={}", SecurityUtils.getUsername(), patientId);
            throw new ServiceException(403, "无权查看该患者数据");
        }
    }

    @Override
    public void checkAccess(Collection<Long> patientIds) {
        if (patientIds == null || patientIds.isEmpty()) {
            return;
        }
        Set<Long> visible = visiblePatientIds();
        if (visible == null) {
            return;
        }
        boolean denied = patientIds.stream().filter(Objects::nonNull).anyMatch(id -> !visible.contains(id));
        if (denied) {
            log.debug("越权访问患者数据：userId={} 请求={}", SecurityUtils.getUsername(), patientIds);
            throw new ServiceException(403, "无权查看该患者数据");
        }
    }
}