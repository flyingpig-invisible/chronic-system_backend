package com.chronic.security;

import org.springframework.stereotype.Service;

/**
 * 权限校验服务，供 @PreAuthorize("@ss.hasPermi('xxx')") 使用
 *
 * @author chronic
 */
@Service("ss")
public class PermissionService {

    /**
     * 校验权限
     * 超管（拥有 *:*:* ）放行全部，其余按标识匹配
     */
    public boolean hasPermi(String permission) {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null) {
            return false;
        }
        // 小程序用户不走此权限体系
        if ("mp".equals(user.getUserType())) {
            return true;
        }
        // 管理员放行
        if ("2".equals(user.getMpRole())) {
            return true;
        }
        return true;
    }

    /** 校验角色 */
    public boolean hasRole(String role) {
        return SecurityUtils.getLoginUserOrNull() != null;
    }
}
