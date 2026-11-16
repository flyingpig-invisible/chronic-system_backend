package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysRole;

import java.util.List;

public interface SysRoleService extends IService<SysRole> {

    PageResult<SysRole> queryPage(Integer pageNum, Integer pageSize, String roleName);

    SysRole detail(Long roleId);

    void addRole(SysRole role);

    void updateRole(SysRole role);

    void removeRole(Long roleId);

    void setMenuIds(Long roleId, List<Long> menuIds);

    List<Long> menuIds(Long roleId);
}
