package com.chronic.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 健康助手配置
 *
 * @author chronic
 */
@Data
@Component
@ConfigurationProperties(prefix = "chronic.ai")
public class AiProperties {

    /** 是否启用 */
    private Boolean enabled = true;

    /** 服务地址 */
    private String baseUrl = "https://api.deepseek.com";

    /** API Key */
    private String apiKey;

    /** 模型名称 */
    private String model = "deepseek-chat";

    /** 超时时间（毫秒） */
    private Integer timeout = 60000;

    /** 温度 */
    private Double temperature = 0.3;

    /** 最大输出 token */
    private Integer maxTokens = 1500;
}
