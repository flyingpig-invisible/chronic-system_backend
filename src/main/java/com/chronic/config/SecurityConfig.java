package com.chronic.config;

import com.chronic.common.result.Result;
import com.chronic.common.result.ResultCode;
import com.chronic.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Spring Security 配置
 *
 * @author chronic
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    /**
     * 本地演示开关：开启后设备相关接口免登录
     * 仅用于课程演示/接口文档调试，生产环境必须保持 false
     */
    @Value("${chronic.dev-open:false}")
    private boolean devOpen;

    /** 无需认证的路径 */
    public static final String[] WHITE_LIST = {
            "/auth/login",
            "/auth/register",
            "/auth/captcha",
            "/mp/auth/login",
            "/mp/auth/register",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/favicon.ico",
            "/profile/**",
            "/upload/**",
            "/error"
    };

    /** 演示模式下额外放行的路径 */
    public static final String[] DEV_OPEN_LIST = {
            "/device/**",
            "/mp/device/**"
    };

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers(WHITE_LIST).permitAll();
                if (devOpen) {
                    // 演示模式：设备相关接口免登录，便于在 Knife4j 里直接点
                    auth.requestMatchers(DEV_OPEN_LIST).permitAll();
                }
                auth.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated();
            })
            .exceptionHandling(ex -> ex
                    .authenticationEntryPoint((request, response, e) ->
                            writeJson(response, Result.error(ResultCode.UNAUTHORIZED, "未登录或登录已过期")))
                    .accessDeniedHandler((request, response, e) ->
                            writeJson(response, Result.error(ResultCode.FORBIDDEN, "没有访问权限"))))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    private void writeJson(HttpServletResponse response, Result<?> result) throws java.io.IOException {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
