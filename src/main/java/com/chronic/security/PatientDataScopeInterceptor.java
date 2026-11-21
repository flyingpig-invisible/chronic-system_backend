package com.chronic.security;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import com.chronic.modules.chronic.service.PatientDataScopeService;
import com.chronic.modules.chronic.service.impl.PatientDataScopeServiceImpl;
import com.chronic.security.LoginUser;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.beans.factory.ObjectProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 患者数据权限（SQL 层统一拦截）
 *
 * <p>医生只能访问「患者已勾选科室包含自己所在科室」的患者数据；管理员与小程序端
 * （患者本人/家属，另有一套归属校验）不受约束。</p>
 *
 * <p>判定口径统一由 {@link PatientDataScopeService} 提供，这里按表名把可见范围翻译成
 * {@code 表.patient_id IN (...)} 追加到 WHERE 上（与原条件 AND）。这样所有查询/更新/删除
 * 默认受控，新增接口无需逐一补校验：</p>
 * <ul>
 *   <li>cd_patient：用自身主键 patient_id 过滤；</li>
 *   <li>其它带 patient_id 归属列的业务表：用该列过滤；</li>
 *   <li>没有可见患者时追加 {@code 1 = 0}；</li>
 *   <li>不在清单内的表（系统表、字典表等）不做任何改写，避免误伤。</li>
 * </ul>
 *
 * @author chronic
 */
@Slf4j
public class PatientDataScopeInterceptor extends DataPermissionInterceptor {

    /** 自身即患者主体的表 */
    private static final String PATIENT_TABLE = "cd_patient";

    /** 带 patient_id 归属列、需要按患者范围隔离的业务表 */
    private static final Set<String> PATIENT_OWNED_TABLES = new HashSet<>(Arrays.asList(
            "cd_measure", "cd_warning", "cd_emergency_link", "cd_plan", "cd_plan_item", "cd_guide",
            "cd_remind", "cd_remind_log", "cd_clinic", "cd_health_score", "cd_device", "cd_import_log",
            "cd_patient_profile", "cd_patient_profile_log", "cd_patient_disease", "cd_ai_chat", "cd_message"));

    /**
     * 注册到 MyBatis-Plus。用 ObjectProvider 延迟解析服务，
     * 避免“拦截器配置 -> 数据范围服务 -> Mapper -> MyBatis”的循环依赖。
     */
    public static void register(MybatisPlusInterceptor interceptor, ObjectProvider<PatientDataScopeService> provider) {
        interceptor.addInnerInterceptor(new DataPermissionInterceptor(new PatientDataPermissionHandler(provider::getObject)));
    }

    /**
     * 请求结束清理请求级缓存。
     * <p>正常情况下缓存会按「用户身份」自动失效，这里只是兜底，防止上下文残留。</p>
     */
    public static class CacheClearFilter extends org.springframework.web.filter.OncePerRequestFilter {
        @Override
        protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                        jakarta.servlet.http.HttpServletResponse response,
                                        jakarta.servlet.FilterChain chain)
                throws jakarta.servlet.ServletException, java.io.IOException {
            try {
                chain.doFilter(request, response);
            } finally {
                PatientDataScopeServiceImpl.clearCache();
            }
        }
    }

    /** 数据权限规则：按表生成范围条件，MyBatis-Plus 负责与原 where 做 AND 合并 */
    static class PatientDataPermissionHandler implements MultiDataPermissionHandler {

        private final java.util.function.Supplier<PatientDataScopeService> scopeSupplier;

        PatientDataPermissionHandler(java.util.function.Supplier<PatientDataScopeService> scopeSupplier) {
            this.scopeSupplier = scopeSupplier;
        }

        private PatientDataScopeService dataScopeService() {
            return scopeSupplier.get();
        }

        @Override
        public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
            if (table == null || !isPatientScoped(table.getName())) {
                return null;
            }
            // 只有“受患者范围约束”的后台医生才改写；管理员与小程序端直接放行
            if (!dataScopeService().isPatientDriven()) {
                return null;
            }
            Collection<Long> visible = dataScopeService().sqlVisiblePatientIds();
            if (visible == null) {
                // null 表示当前用户不受患者范围限制（管理员），不能改写成 1 = 0
                return null;
            }
            if (visible.isEmpty()) {
                return new EqualsTo(new LongValue(1), new LongValue(0));
            }
            List<Expression> items = new ArrayList<>(visible.size());
            for (Long id : visible) {
                items.add(new LongValue(id));
            }
            ParenthesedExpressionList<Expression> values = new ParenthesedExpressionList<>(items);
            InExpression in = new InExpression();
            in.setLeftExpression(new Column(table, "patient_id"));
            in.setRightExpression(values);
            return in;
        }

        private boolean isPatientScoped(String tableName) {
            if (tableName == null) {
                return false;
            }
            String name = tableName.toLowerCase();
            return PATIENT_TABLE.equals(name) || PATIENT_OWNED_TABLES.contains(name);
        }
    }
}
