package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysDeptMapper;
import com.chronic.modules.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 科室归属解析
 *
 * <p>本系统中患者可能同时患有多种病，患者本身不绑定固定科室，
 * 科室一律以「责任医生所在科室」为准。所有科室字段的写入与展示都应通过本类推导。</p>
 *
 * @author chronic
 */
@Component
@RequiredArgsConstructor
public class DeptResolver {

    private final SysUserMapper userMapper;
    private final SysDeptMapper deptMapper;

    /** 取责任医生的科室ID，医生不存在或未分配科室时返回 null */
    public Long deptIdOfDoctor(Long doctorId) {
        if (doctorId == null) {
            return null;
        }
        SysUser doctor = userMapper.selectById(doctorId);
        return doctor == null ? null : doctor.getDeptId();
    }

    /** 批量取科室ID -> 科室名称 */
    public Map<Long, String> deptNames(Collection<Long> deptIds) {
        Set<Long> ids = deptIds == null ? Collections.emptySet()
                : deptIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> names = new HashMap<>();
        for (SysDept d : deptMapper.selectList(Wrappers.<SysDept>lambdaQuery().in(SysDept::getDeptId, ids))) {
            names.put(d.getDeptId(), d.getDeptName());
        }
        return names;
    }

    /** 取某科室下的全部用户ID（含该科室，不含下级科室） */
    public Set<Long> userIdsOfDept(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> ids = userMapper.selectList(Wrappers.<SysUser>lambdaQuery()
                        .select(SysUser::getUserId)
                        .eq(SysUser::getDeptId, deptId))
                .stream().map(SysUser::getUserId).collect(Collectors.toCollection(HashSet::new));
        return ids;
    }
}
