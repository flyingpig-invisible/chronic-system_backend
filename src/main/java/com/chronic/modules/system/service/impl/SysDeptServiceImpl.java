package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.mapper.SysDeptMapper;
import com.chronic.modules.system.service.SysDeptService;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.modules.system.entity.SysUser;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements SysDeptService {

    private final SysUserMapper userMapper;

    @Override
    public List<SysDept> tree() {
        List<SysDept> all = list(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getOrderNum));
        return buildTree(all, 0L);
    }

    private List<SysDept> buildTree(List<SysDept> all, Long parentId) {
        List<SysDept> result = new ArrayList<>();
        for (SysDept d : all) {
            Long pid = d.getParentId() == null ? 0L : d.getParentId();
            if (pid.equals(parentId)) {
                d.setChildren(buildTree(all, d.getDeptId()));
                result.add(d);
            }
        }
        return result;
    }

    @Override
    public void addDept(SysDept dept) {
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new ServiceException("科室名称不能为空");
        }
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (!StringUtils.hasText(dept.getStatus())) {
            dept.setStatus("0");
        }
        save(dept);
    }

    @Override
    public void updateDept(SysDept dept) {
        updateById(dept);
    }

    @Override
    public void removeDept(Long deptId) {
        Long children = count(new LambdaQueryWrapper<SysDept>().eq(SysDept::getParentId, deptId));
        if (children != null && children > 0) {
            throw new ServiceException("存在下级科室，无法删除");
        }
        // 科室被在职用户引用时不允许删除
        Long userCount = userMapper.selectCount(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getDeptId, deptId)
                .eq(SysUser::getStatus, "0"));
        if (userCount != null && userCount > 0) {
            throw new ServiceException("该科室下还有在职用户，无法删除");
        }

        removeById(deptId);
    }
}
