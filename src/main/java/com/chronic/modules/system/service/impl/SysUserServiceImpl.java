package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysDeptMapper;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.modules.system.query.UserQuery;
import com.chronic.modules.system.service.SysUserService;
import com.chronic.modules.system.service.DeptResolver;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_]{3,19}$");

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private DeptResolver deptResolver;

    @Autowired
    private SysDeptMapper deptMapper;

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
        // 填充科室名称，前端直接展示汉字名称而不是科室ID
        java.util.Map<Long, String> deptNames = deptResolver.deptNames(
                page.getRecords().stream().map(SysUser::getDeptId)
                        .filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()));
        page.getRecords().forEach(u -> u.setDeptName(u.getDeptId() == null ? null : deptNames.get(u.getDeptId())));
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public SysUser detail(Long userId) {
        SysUser user = getById(userId);
        if (user != null) {
            user.setPassword(null);
            java.util.Map<Long, String> names = deptResolver.deptNames(
                    user.getDeptId() == null ? java.util.Collections.emptySet() : java.util.Set.of(user.getDeptId()));
            user.setDeptName(user.getDeptId() == null ? null : names.get(user.getDeptId()));
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
        // 医生必须归属科室：患者可见范围按科室划分，没有科室的医生看不到任何患者
        if ("1".equals(user.getUserType())) {
            validateDoctorDept(user.getDeptId());
        }
        save(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(SysUser user) {
        if (user == null || user.getUserId() == null) {
            throw new ServiceException("请选择要修改的用户");
        }
        SysUser target = getById(user.getUserId());
        if (target == null) {
            throw new ServiceException("用户不存在");
        }
        // 不允许通过通用修改接口改密码
        user.setPassword(null);
        String newUsername = user.getUsername() == null ? "" : user.getUsername().trim();
        user.setUsername(null);
        if (StringUtils.hasText(newUsername) && !newUsername.equals(target.getUsername())) {
            // 用户名即登录账号：只允许本人修改自己的，避免越权改掉他人的登录入口
            LoginUser current = SecurityUtils.getLoginUserOrNull();
            if (current == null || !Objects.equals(current.getUserId(), target.getUserId())) {
                throw new ServiceException(403, "只能修改自己的用户名");
            }
            user.setUsername(validateUsername(newUsername, target.getUserId()));
        }
        // 医生必须保有所属科室：患者可见范围按科室划分，没有科室的医生看不到任何患者
        String userType = StringUtils.hasText(user.getUserType()) ? user.getUserType() : target.getUserType();
        if ("1".equals(userType)) {
            validateDoctorDept(user.getDeptId());
        }
        updateById(user);
    }

    /** 校验医生所属科室：必填、存在且启用 */
    private Long validateDoctorDept(Long deptId) {
        if (deptId == null) {
            throw new ServiceException("请选择所属科室");
        }
        SysDept dept = deptMapper.selectById(deptId);
        if (dept == null || !"0".equals(dept.getStatus())) {
            throw new ServiceException("所属科室不存在或已停用");
        }
        return deptId;
    }

    /** 用户名规则与小程序端一致：字母/数字开头，仅字母数字下划线，4~20 位 */
    private String validateUsername(String username, Long selfId) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ServiceException("用户名需为 4~20 位，以字母或数字开头，仅可包含字母、数字和下划线");
        }
        Long exists = count(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, username)
                .ne(SysUser::getUserId, selfId));
        if (exists != null && exists > 0) {
            throw new ServiceException("用户名已存在");
        }
        return username;
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
        // 补全科室名称，便于前端在医生下拉中展示所属科室
        Map<Long, String> deptNames = deptResolver.deptNames(
                list.stream().map(SysUser::getDeptId).filter(java.util.Objects::nonNull).collect(java.util.stream.Collectors.toSet()));
        list.forEach(u -> u.setDeptName(u.getDeptId() == null ? null : deptNames.get(u.getDeptId())));
        return list;
    }

    @Override
    public List<SysUser> doctorsOfDept(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }
        List<SysUser> list = list(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserType, "1")
                .eq(SysUser::getDeptId, deptId)
                .orderByAsc(SysUser::getStatus)
                .orderByAsc(SysUser::getRealName));
        list.forEach(u -> u.setPassword(null));
        Map<Long, String> deptNames = deptResolver.deptNames(List.of(deptId));
        list.forEach(u -> u.setDeptName(deptNames.get(deptId)));
        return list;
    }
}
