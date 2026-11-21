package com.chronic.modules.chronic.service;

import java.util.Collection;
import java.util.Set;

/**
 * 患者数据范围
 * <p>
 * 医生只能读取「患者注册时勾选科室」中包含自己所在科室的数据；
 * 系统管理员不受限制。所有按患者ID读取/修改的接口都应走这里做校验，
 * 避免只在列表接口做过滤、详情接口被直接猜 ID 越权读取。
 *
 * @author chronic
 */
public interface PatientDataScopeService {

    /**
     * 当前登录(系统)用户可见的患者ID
     *
     * @return null 表示不受限制（系统管理员）；否则为可见患者ID集合（可能为空）
     */
    Set<Long> visiblePatientIds();

    /** 当前用户是否可读取该患者数据；管理员恒为 true */
    boolean canAccess(Long patientId);

    /** 校验当前用户可读取该患者数据，不可读时抛 403 */
    void checkAccess(Long patientId);

    /** 校验当前用户可读取给定的全部患者数据，不可读时抛 403 */
    void checkAccess(Collection<Long> patientIds);

    /**
     * 非管理员时返回可见患者ID集合（可能为空，调用方应直接返回空结果）；管理员返回 null 表示不加限制。
     * 列表类查询用它把 SQL 范围收窄，单条查询用 checkAccess 直接拒绝。
     */
    Set<Long> restrictPatientIds();

    /**
     * 数据权限拦截器（SQL 层）使用：不给当前用户任何限制时返回 false（管理员 / 小程序端 / 未登录）。
     * 返回 true 表示后续必须追加可见患者过滤。
     */
    boolean isPatientDriven();

    /** 当前用户可见的患者ID（数据权限拦截器使用，非空集合由调用方保证） */
    java.util.Collection<Long> sqlVisiblePatientIds();
}