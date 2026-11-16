package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysRole;
import com.chronic.modules.system.mapper.SysRoleMapper;
import com.chronic.modules.system.service.SysRoleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public PageResult<SysRole> queryPage(Integer pageNum, Integer pageSize, String roleName) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
         .orderByAsc(SysRole::getOrderNum);
        Page<SysRole> page = page(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public SysRole detail(Long roleId) {
        return getById(roleId);
    }

    @Override
    public void addRole(SysRole role) {
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new ServiceException("角色名称不能为空");
        }
        if (!StringUtils.hasText(role.getStatus())) {
            role.setStatus("0");
        }
        save(role);
    }

    @Override
    public void updateRole(SysRole role) {
        updateById(role);
    }

    @Override
    public void removeRole(Long roleId) {
        if (roleId != null && roleId == 1L) {
            throw new ServiceException("超级管理员角色不可删除");
        }
        removeById(roleId);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
    }

    @Override
    public void setMenuIds(Long roleId, List<Long> menuIds) {
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
            }
        }
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return jdbcTemplate.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id = ?", Long.class, roleId);
    }
}
