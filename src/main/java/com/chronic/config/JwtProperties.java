package com.chronic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * JWT 配置属性
 *
 * @author chronic
 */
@Data
@Component
@ConfigurationProperties(prefix = "chronic.jwt")
public class JwtProperties {

    /** 签名密钥 */
    private String secret;

    /** 有效期（毫秒） */
    private Long expire = 604800000L;

    /** 请求头名称 */
    private String header = "Authorization";

    /** token 前缀 */
    private String prefix = "Bearer ";
}
