package com.chronic.security;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.modules.system.entity.SysRole;
import com.chronic.modules.system.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 权限校验服务，供 @PreAuthorize("@ss.hasPermi('xxx')") 使用
 *
 * @author chronic
 */
@Service("ss")
@RequiredArgsConstructor
public class PermissionService {

    private final SysRoleMapper roleMapper;

    /** 本地演示开关：开启后跳过权限校验（与 SecurityConfig 的 chronic.dev-open 保持一致） */
    @Value("${chronic.dev-open:false}")
    private boolean devOpen;

    /**
     * 校验权限
     * - 系统管理员（user_type=2）拥有全部权限
     * - 小程序用户不走后台权限体系
     * - 其他后台用户按角色绑定的菜单权限标识匹配
     */
    public boolean hasPermi(String permission) {
        // 演示模式：不做权限校验，便于接口文档直接调试
        if (devOpen) {
            return true;
        }
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        if ("mp".equals(user.getUserType())) {
            return true;
        }
        if (isSysAdmin(user)) {
            return true;
        }
        List<String> permissions = permissionsOf(user.getUserId());
        return permissions.contains("*:*:*") || permissions.contains(permission);
    }

    /** 校验角色标识 */
    public boolean hasRole(String role) {
        if (devOpen) {
            return true;
        }
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
        return user != null && !"mp".equals(user.getUserType()) && "2".equals(user.getUserType());
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
