package com.chronic.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.chronic.security.PatientScopeInterceptor;
import com.chronic.security.SecurityUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置
 *
 * <p>数据权限拦截器只读取当前登录身份里的科室，不依赖任何 Service / Mapper，
 * 因此这里可以直接用 {@link SecurityUtils} 取当前用户，不存在循环依赖。</p>
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
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(PatientScopeInterceptor.of(SecurityUtils::getLoginUserOrNull));
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.MYSQL);
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
