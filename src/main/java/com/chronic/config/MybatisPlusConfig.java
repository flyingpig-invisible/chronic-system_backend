package com.chronic.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.chronic.modules.chronic.service.PatientDataScopeService;
import com.chronic.security.PatientDataScopeInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * MyBatis-Plus 配置
 *
 * <p>数据权限拦截器依赖 PatientDataScopeService，而该服务又依赖 Mapper / MyBatis 自身，
 * 直接注入会形成循环依赖，因此这里用 {@link ObjectProvider} 延迟到真正执行 SQL 时才解析。</p>
 *
 * @author chronic
 */
@Configuration
public class MybatisPlusConfig {

    /**
     * 插件顺序：先做患者数据权限（改写 WHERE），再做分页，
     * 保证分页的 count 语句同样带上数据范围条件。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(ObjectProvider<PatientDataScopeService> scopeServiceProvider) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        PatientDataScopeInterceptor.register(interceptor, scopeServiceProvider);
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
