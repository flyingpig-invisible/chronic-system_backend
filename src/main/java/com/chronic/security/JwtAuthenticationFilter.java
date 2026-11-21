package com.chronic.security;

import com.chronic.common.utils.JwtUtils;
import com.chronic.config.JwtProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JWT 认证过滤器
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtProperties jwtProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            authenticate(request);
        } catch (Exception e) {
            log.warn("JWT 认证失败: {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }
        // 无论是否认证成功都必须放行，未认证请求交由后续鉴权规则处理（返回 401/403）
        chain.doFilter(request, response);
    }

    private void authenticate(HttpServletRequest request) {
        String token = resolveToken(request);
        if (token == null || !jwtUtils.validate(token)) {
            return;
        }
        Long userId = jwtUtils.getUserId(token);
        if (userId == null) {
            return;
        }
        String userType = jwtUtils.getUserType(token);
        if (userType == null || userType.isBlank()) {
            // 有效 token 却缺少用户类型，无法判断身份，按未认证处理
            return;
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(userId);
        loginUser.setUsername(jwtUtils.getUsername(token));
        // 保留原始值：1 医生/普通用户、2 系统管理员、mp 小程序用户
        loginUser.setUserType(userType.trim());
        loginUser.setRealName(jwtUtils.getRealName(token));
        loginUser.setMpRole(jwtUtils.getMpRole(token));
        loginUser.setDeptId(jwtUtils.getDeptId(token));

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(loginUser, null, authoritiesOf(userType.trim()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    /** 按用户类型授予角色，便于后续使用 @PreAuthorize("hasRole('ADMIN')") */
    private List<GrantedAuthority> authoritiesOf(String userType) {
        List<GrantedAuthority> authorities = new ArrayList<>(AuthorityUtils.createAuthorityList("ROLE_USER"));
        if ("2".equals(userType)) {
            authorities.addAll(AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        } else if ("mp".equals(userType)) {
            authorities.addAll(AuthorityUtils.createAuthorityList("ROLE_MP"));
        }
        return authorities;
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(jwtProperties.getHeader());
        if (header == null || header.isBlank()) {
            return null;
        }
        String value = header.trim();
        String prefix = jwtProperties.getPrefix();
        if (prefix != null && value.startsWith(prefix)) {
            return value.substring(prefix.length()).trim();
        }
        // 兼容不带 Bearer 前缀的客户端（如 Knife4j 的 Authorize 会原样发送填写内容）
        if (value.split("\\.").length == 3) {
            return value;
        }
        return null;
    }
}