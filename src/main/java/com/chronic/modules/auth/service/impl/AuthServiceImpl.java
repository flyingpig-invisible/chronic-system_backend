package com.chronic.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.utils.JwtUtils;
import com.chronic.modules.auth.dto.LoginDTO;
import com.chronic.modules.auth.service.AuthService;
import com.chronic.modules.auth.vo.LoginVO;
import com.chronic.modules.auth.vo.UserInfoVO;
import com.chronic.modules.system.entity.SysMenu;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysMenuMapper;
import com.chronic.modules.system.mapper.SysDeptMapper;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 认证服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final SysDeptMapper deptMapper;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));

        if (user == null) {
            throw new ServiceException("用户名或密码错误");
        }
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException("账号已被停用，请联系管理员");
        }

        // 更新登录时间
        SysUser update = new SysUser();
        update.setUserId(user.getUserId());
        update.setLoginDate(LocalDateTime.now());
        userMapper.updateById(update);

        String token = jwtUtils.createToken(user.getUserId(), user.getUsername(), "sys",
                user.getRealName(), null, user.getDeptId());

        LoginVO vo = new LoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setUserType("sys");
        return vo;
    }

    @Override
    public UserInfoVO getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new ServiceException("用户不存在");
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setUserType(user.getUserType());
        vo.setDeptId(user.getDeptId());
        if (user.getDeptId() != null) {
            SysDept dept = deptMapper.selectById(user.getDeptId());
            vo.setDeptName(dept == null ? null : dept.getDeptName());
        }

        // 超管拥有全部权限
        List<String> roles = new ArrayList<>();
        List<String> permissions = new ArrayList<>();
        if ("2".equals(user.getUserType())) {
            roles.add("admin");
            permissions.add("*:*:*");
        } else {
            roles.add("doctor");
            List<SysMenu> menus = menuMapper.selectList(Wrappers.<SysMenu>lambdaQuery()
                    .eq(SysMenu::getMenuType, "F")
                    .isNotNull(SysMenu::getPerms));
            menus.forEach(m -> permissions.add(m.getPerms()));
        }
        vo.setRoles(roles);
        vo.setPermissions(permissions);
        return vo;
    }

    @Override
    public void logout() {
        // 纯 JWT 无状态，前端清除 token 即可
        log.info("用户退出登录: {}", SecurityUtils.getUsername());
    }
}
