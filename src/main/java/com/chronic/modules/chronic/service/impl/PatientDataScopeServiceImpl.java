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

    /** 请求级缓存：同一次请求内数据权限拦截器会对多条 SQL 反复取可见范围 */
    private static final ThreadLocal<ScopeCache> CACHE = new ThreadLocal<>();
    /** 正在计算可见范围，防止拦截器递归 */
    private static final ThreadLocal<Boolean> COMPUTING = new ThreadLocal<>();

    private static final class ScopeCache {
        final String owner;
        final boolean restricted;
        final Set<Long> ids;
        ScopeCache(String owner, boolean restricted, Set<Long> ids) {
            this.owner = owner;
            this.restricted = restricted;
            this.ids = ids;
        }
    }

    @Override
    public Set<Long> visiblePatientIds() {
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        // 缓存按用户身份绑定：同一线程被下一个请求复用时不会串用上一个用户的可见范围
        String owner = current == null ? "-" : current.getUserType() + ":" + current.getUserId();
        ScopeCache cached = CACHE.get();
        if (cached != null && owner.equals(cached.owner)) {
            return cached.restricted ? cached.ids : null;
        }
        // 防死循环：计算可见范围本身要查医生所属科室的患者关联，这些 SQL 不应再被拦截器处理
        if (Boolean.TRUE.equals(COMPUTING.get())) {
            return null;
        }
        if (current == null) {
            return Collections.emptySet();
        }
        // 小程序端（患者本人/家属）有各自的归属校验，不走医生数据范围
        if ("mp".equals(current.getUserType())) {
            CACHE.set(new ScopeCache(owner, false, null));
            return null;
        }
        SysUser user = userMapper.selectById(current.getUserId());
        if (user == null) {
            return Collections.emptySet();
        }
        if ("2".equals(user.getUserType())) {
            CACHE.set(new ScopeCache(owner, false, null));
            return null;
        }
        Set<Long> ids = new LinkedHashSet<>();
        COMPUTING.set(Boolean.TRUE);
        try {
            if (user.getDeptId() != null) {
                ids.addAll(patientDeptService.patientIdsOfDept(user.getDeptId()));
            }
        } finally {
            COMPUTING.remove();
        }
        CACHE.set(new ScopeCache(owner, true, ids));
        return ids;
    }

    /** 与 visiblePatientIds 同义，单独暴露给列表查询做语义区分：null 表示管理员不受限 */
    @Override
    public Set<Long> restrictPatientIds() {
        return visiblePatientIds();
    }

    @Override
    public boolean isPatientDriven() {
        LoginUser current = SecurityUtils.getLoginUserOrNull();
        if (current == null) {
            return false;
        }
        // 小程序端有自己的归属校验；系统管理员（user_type=2）不受患者范围限制
        String userType = current.getUserType();
        return !"mp".equals(userType) && !"2".equals(userType);
    }

    /**
     * 给 SQL 拦截器使用：null 表示不受限（管理员/小程序端），空集合表示确实没有可见患者。
     * 注意不能把 null 转成空集合，否则管理员会被拼上 1 = 0 而查不到任何数据。
     */
    @Override
    public java.util.Collection<Long> sqlVisiblePatientIds() {
        return visiblePatientIds();
    }
    /** 请求级缓存清理（过滤器在请求结束时调用） */
    public static void clearCache() {
        CACHE.remove();
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