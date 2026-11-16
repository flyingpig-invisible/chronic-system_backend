package com.chronic.common.utils;

import com.chronic.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT 工具类
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final JwtProperties jwtProperties;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @param userType 用户类型：sys 系统用户 / mp 小程序用户
     */
    public String createToken(Long userId, String username, String userType) {
        return createToken(userId, username, userType, null, null);
    }

    /**
     * 鐢熸垚 token锛堝惈鐪熷疄濮撳悕涓庡皬绋嬪簭瑙掕壊锛?
     */
    public String createToken(Long userId, String username, String userType, String realName, String mpRole) {
        return createToken(userId, username, userType, realName, mpRole, null);
    }

    /** 生成 token（含科室ID，科室信息以责任医生所在科室为准） */
    public String createToken(Long userId, String username, String userType, String realName, String mpRole, Long deptId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("userType", userType);
        if (realName != null) {
            claims.put("realName", realName);
        }
        if (deptId != null) {
            claims.put("deptId", deptId);
        }
        if (mpRole != null) {
            claims.put("mpRole", mpRole);
        }

        Date now = new Date();
        Date expireDate = new Date(now.getTime() + jwtProperties.getExpire());

        return Jwts.builder()
                .claims(claims)
                .issuedAt(now)
                .expiration(expireDate)
                .signWith(getKey())
                .compact();
    }

    /**
     * 解析 token
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            log.warn("token 解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 校验 token 是否有效
     */
    public boolean validate(String token) {
        return parseToken(token) != null;
    }

    public Long getUserId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object userId = claims.get("userId");
        return userId == null ? null : Long.valueOf(userId.toString());
    }

    public String getUsername(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get("username");
    }

    public String getUserType(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get("userType");
    }

    public String getRealName(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get("realName");
    }

    public Long getDeptId(String token) {
        Claims claims = parseToken(token);
        if (claims == null) {
            return null;
        }
        Object deptId = claims.get("deptId");
        return deptId == null ? null : Long.valueOf(deptId.toString());
    }

    public String getMpRole(String token) {
        Claims claims = parseToken(token);
        return claims == null ? null : (String) claims.get("mpRole");
    }
}
