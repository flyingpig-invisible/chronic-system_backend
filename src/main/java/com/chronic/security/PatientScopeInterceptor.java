package com.chronic.security;

import com.baomidou.mybatisplus.extension.plugins.handler.MultiDataPermissionHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.DataPermissionInterceptor;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.ParenthesedSelect;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * 患者数据权限（SQL 层统一拦截）
 *
 * <p>医生只能访问「患者已勾选科室包含自己所在科室」的患者数据；管理员与小程序端
 * （患者本人/家属，另有一套归属校验）不受约束。</p>
 *
 * <p>这里只读当前登录身份里的科室ID，不查任何业务表，把可见范围直接表达成子查询：</p>
 * <pre>patient_id IN (SELECT d.patient_id FROM cd_patient_dept d WHERE d.dept_id = 当前医生科室)</pre>
 * <p>因此不存在「拦截器要查库、查询本身又被拦截器改写」的循环依赖，也不需要请求级缓存和防递归标记。</p>
 *
 * @author chronic
 */
@Slf4j
public class PatientScopeInterceptor extends DataPermissionInterceptor {

    /** 自身即患者主体的表 */
    private static final String PATIENT_TABLE = "cd_patient";

    /** 带 patient_id 归属列、需要按患者范围隔离的业务表 */
    private static final Set<String> PATIENT_OWNED_TABLES = new HashSet<>(Arrays.asList(
            "cd_measure", "cd_warning", "cd_emergency_link", "cd_plan", "cd_plan_item", "cd_guide",
            "cd_remind", "cd_remind_log", "cd_clinic", "cd_health_score", "cd_device", "cd_import_log",
            "cd_patient_profile", "cd_patient_profile_log", "cd_patient_disease", "cd_ai_chat", "cd_message"));

    /** 登录身份来源。用 Supplier 传入，避免 MyBatis 初始化时就绑定 Spring Security 上下文 */
    private final Supplier<LoginUser> loginUserSupplier;

    public PatientScopeInterceptor(Supplier<LoginUser> loginUserSupplier) {
        super(new Handler(loginUserSupplier));
        this.loginUserSupplier = loginUserSupplier;
    }

    /** 注册到 MyBatis-Plus 插件链 */
    public static PatientScopeInterceptor of(Supplier<LoginUser> loginUserSupplier) {
        return new PatientScopeInterceptor(loginUserSupplier);
    }

    /** 数据权限规则：按表生成范围条件，MyBatis-Plus 负责与原 where 做 AND 合并 */
    static final class Handler implements MultiDataPermissionHandler {

        private final Supplier<LoginUser> loginUserSupplier;

        Handler(Supplier<LoginUser> loginUserSupplier) {
            this.loginUserSupplier = loginUserSupplier;
        }

        @Override
        public Expression getSqlSegment(Table table, Expression where, String mappedStatementId) {
            return scopeOf(table, loginUserSupplier.get());
        }
    }

    /** 患者数据范围条件；不适用（非患者相关表 / 管理员 / 小程序端）时返回 null */
    static Expression scopeOf(Table table, LoginUser user) {
        if (table == null || user == null || !isPatientScoped(table.getName())) {
            return null;
        }
        String userType = user.getUserType();
        // 小程序端有自己的归属校验；系统管理员不受患者范围限制
        if (userType == null || "mp".equals(userType) || "2".equals(userType)) {
            return null;
        }
        Long deptId = user.getDeptId();
        // 医生未分配科室时看不到任何患者，避免退化成全量可见
        if (deptId == null) {
            return new EqualsTo(new LongValue(1), new LongValue(0));
        }
        return new InExpression(new Column(table, "patient_id"), deptPatientsSubSelect(deptId));
    }

    private static ParenthesedSelect deptPatientsSubSelect(Long deptId) {
        PlainSelect sub = new PlainSelect();
        sub.addSelectItems(new SelectItem<>(new Column(new Table("d"), "patient_id")));
        // 注意：Table(String, String) 的双参是「库名.表名」，别名必须单独设置，否则会渲染成 cd_patient_dept.d
        Table patientDept = new Table("cd_patient_dept");
        patientDept.setAlias(new Alias("d"));
        sub.setFromItem(patientDept);
        sub.setWhere(new EqualsTo(new Column(new Table("d"), "dept_id"), new LongValue(deptId)));
        return new ParenthesedSelect().withSelect(sub);
    }

    private static boolean isPatientScoped(String tableName) {
        if (tableName == null) {
            return false;
        }
        String name = tableName.toLowerCase();
        return PATIENT_TABLE.equals(name) || PATIENT_OWNED_TABLES.contains(name);
    }
}
