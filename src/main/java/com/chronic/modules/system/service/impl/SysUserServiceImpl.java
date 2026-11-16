package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.modules.system.query.UserQuery;
import com.chronic.modules.system.service.SysUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public PageResult<SysUser> queryPage(UserQuery query) {
        LambdaQueryWrapper<SysUser> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(query.getUsername()), SysUser::getUsername, query.getUsername())
         .like(StringUtils.hasText(query.getRealName()), SysUser::getRealName, query.getRealName())
         .like(StringUtils.hasText(query.getPhone()), SysUser::getPhone, query.getPhone())
         .eq(query.getDeptId() != null, SysUser::getDeptId, query.getDeptId())
         .eq(StringUtils.hasText(query.getUserType()), SysUser::getUserType, query.getUserType())
         .eq(StringUtils.hasText(query.getStatus()), SysUser::getStatus, query.getStatus())
         .orderByDesc(SysUser::getCreateTime);
        Page<SysUser> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        page.getRecords().forEach(u -> u.setPassword(null));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public SysUser detail(Long userId) {
        SysUser user = getById(userId);
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }

    @Override
    public void addUser(SysUser user) {
        if (!StringUtils.hasText(user.getUsername())) {
            throw new ServiceException("用户名不能为空");
        }
        Long exists = count(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, user.getUsername()));
        if (exists != null && exists > 0) {
            throw new ServiceException("用户名已存在");
        }
        String raw = StringUtils.hasText(user.getPassword()) ? user.getPassword() : "123456";
        user.setPassword(passwordEncoder.encode(raw));
        if (!StringUtils.hasText(user.getUserType())) {
            user.setUserType("1");
        }
        if (!StringUtils.hasText(user.getStatus())) {
            user.setStatus("0");
        }
        save(user);
    }

    @Override
    public void updateUser(SysUser user) {
        // 不允许通过通用修改接口改密码
        user.setPassword(null);
        user.setUsername(null);
        updateById(user);
    }

    @Override
    public void removeUser(Long userId) {
        if (userId != null && userId == 1L) {
            throw new ServiceException("超级管理员不可删除");
        }
        removeById(userId);
    }

    @Override
    public void resetPassword(Long userId, String password) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        String raw = StringUtils.hasText(password) ? password : "123456";
        user.setPassword(passwordEncoder.encode(raw));
        updateById(user);
    }

    @Override
    public void changeStatus(Long userId, String status) {
        SysUser user = getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        user.setStatus(status);
        updateById(user);
    }

    @Override
    public List<SysUser> doctorList() {
        List<SysUser> list = list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, "1")
                .eq(SysUser::getStatus, "0")
                .orderByAsc(SysUser::getRealName));
        list.forEach(u -> u.setPassword(null));
        return list;
    }
}
