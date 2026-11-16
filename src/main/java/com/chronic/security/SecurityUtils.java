package com.chronic.security;

import com.chronic.common.exception.ServiceException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 安全上下文工具
 *
 * @author chronic
 */
public class SecurityUtils {

    /** 获取当前登录用户 */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            throw new ServiceException(401, "未登录或登录已过期");
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /** 获取当前登录用户（可能为 null） */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser)) {
            return null;
        }
        return (LoginUser) authentication.getPrincipal();
    }

    /** 获取当前用户ID */
    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    /** 获取当前用户名 */
    public static String getUsername() {
        LoginUser user = getLoginUserOrNull();
        return user == null ? "system" : user.getUsername();
    }

    /** 判断是否为小程序用户 */
    public static boolean isMpUser() {
        LoginUser user = getLoginUserOrNull();
        return user != null && "mp".equals(user.getUserType());
    }
}
