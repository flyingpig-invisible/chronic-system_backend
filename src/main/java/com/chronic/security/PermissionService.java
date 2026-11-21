package com.chronic.security;

import com.chronic.modules.system.entity.SysRole;
import com.chronic.modules.system.mapper.SysRoleMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限校验服务，供 @PreAuthorize("@ss.hasPermi('xxx')") 使用
 *
 * @author chronic
 */
@Slf4j
@Service("ss")
@RequiredArgsConstructor
public class PermissionService {

    private final SysRoleMapper roleMapper;

    /**
     * 校验权限
     * - 系统管理员（user_type=2）拥有全部权限
     * - 小程序用户不走后台权限体系
     * - 其他后台用户按角色绑定的菜单权限标识匹配
     */
    public boolean hasPermi(String permission) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        // 小程序端（患者/家属）没有后台权限体系，不能访问后台管理接口
        if ("mp".equals(user.getUserType())) {
            return false;
        }
        if (isSysAdmin(user)) {
            return true;
        }
        List<String> permissions = permissionsOf(user.getUserId());
        boolean granted = permissions.contains("*:*:*") || permissions.contains(permission);
        if (!granted) {
            log.debug("权限不足：userId={} 需要 {}", user.getUserId(), permission);
        }
        return granted;
    }

    /** 校验角色标识 */
    public boolean hasRole(String role) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        if (isSysAdmin(user)) {
            return "admin".equals(role) || "superadmin".equals(role);
        }
        return roleKeysOf(user.getUserId()).contains(role);
    }

    /** 是否系统管理员：仅 user_type=2 的系统用户 */
    public boolean isSysAdmin(LoginUser user) {
        return user != null && "2".equals(user.getUserType());
    }

    private List<String> permissionsOf(Long userId) {
        return roleMapper.selectPermissionsByUserId(userId);
    }

    private List<String> roleKeysOf(Long userId) {
        return roleMapper.selectList(Wrappers.<SysRole>lambdaQuery()
                        .inSql(SysRole::getRoleId,
                                "SELECT role_id FROM sys_user_role WHERE user_id = " + userId)
                        .eq(SysRole::getStatus, "0"))
                .stream().map(SysRole::getRoleKey).collect(Collectors.toList());
    }
}