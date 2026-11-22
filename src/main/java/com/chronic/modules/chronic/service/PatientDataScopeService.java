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
}
