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
 * <p>可见口径只有一条：患者已勾选科室（cd_patient_dept）中包含当前医生所在科室。
 * 管理员与小程序端不受医生数据范围约束（小程序端另有归属校验）。</p>
 *
 * <p>科室以数据库实时值为准，不用 JWT 里的快照，管理员调整医生科室后立即生效。
 * SQL 层的列表过滤由 {@code PatientScopeInterceptor} 用同样口径完成，这里负责单条/批量显式校验。</p>
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
        if (user.getDeptId() == null) {
            return Collections.emptySet();
        }
        return new LinkedHashSet<>(patientDeptService.patientIdsOfDept(user.getDeptId()));
    }

    @Override
    public boolean canAccess(Long patientId) {
        if (patientId == null) {
            return false;
        }
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        if (current == null) {
            return false;
        }
        if ("mp".equals(current.getUserType())) {
            return false;
        }
        SysUser user = userMapper.selectById(current.getUserId());
        if (user == null) {
            return false;
        }
        if ("2".equals(user.getUserType())) {
            return true;
        }
        if (user.getDeptId() == null) {
            return false;
        }
        // 直接查「该患者是否勾选了本科室」，比先取全部可见患者再 contains 更省
        Long allowed = patientMapper.selectCount(Wrappers.<CdPatient>lambdaQuery()
                .eq(CdPatient::getPatientId, patientId)
                .inSql(CdPatient::getPatientId,
                        "SELECT d.patient_id FROM cd_patient_dept d WHERE d.dept_id = " + user.getDeptId()));
        return allowed != null && allowed > 0;
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
        boolean denied = patientIds.stream()
                .filter(Objects::nonNull)
                .anyMatch(id -> !canAccess(id));
        if (denied) {
            log.debug("越权访问患者数据：userId={} 请求={}", SecurityUtils.getUsername(), patientIds);
            throw new ServiceException(403, "无权查看该患者数据");
        }
    }
}
