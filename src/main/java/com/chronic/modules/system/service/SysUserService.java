package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.query.UserQuery;

import java.util.List;

public interface SysUserService extends IService<SysUser> {

    PageResult<SysUser> queryPage(UserQuery query);

    SysUser detail(Long userId);

    void addUser(SysUser user);

    void updateUser(SysUser user);

    void removeUser(Long userId);

    void resetPassword(Long userId, String password);

    void changeStatus(Long userId, String status);

    List<SysUser> doctorList();
}
