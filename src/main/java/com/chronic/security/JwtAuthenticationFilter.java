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
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

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
        String token = resolveToken(request);
        if (token != null && jwtUtils.validate(token)) {
            Long userId = jwtUtils.getUserId(token);
            String username = jwtUtils.getUsername(token);
            String userType = jwtUtils.getUserType(token);
            String realName = jwtUtils.getRealName(token);
            String mpRole = jwtUtils.getMpRole(token);
            Long deptId = jwtUtils.getDeptId(token);

            LoginUser loginUser = new LoginUser();
            loginUser.setUserId(userId);
            loginUser.setUsername(username);
            loginUser.setUserType(userType);
            loginUser.setRealName(realName);
            loginUser.setMpRole(mpRole);
            loginUser.setDeptId(deptId);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(loginUser, null,
                            AuthorityUtils.createAuthorityList("ROLE_USER"));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        chain.doFilter(request, response);
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
