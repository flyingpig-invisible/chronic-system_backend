package com.chronic.modules.chronic.service;

import com.chronic.modules.chronic.entity.CdPatientDept;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 患者可见科室
 * <p>
 * 患者在注册时选择所属科室，只有这些科室的医生能看到其信息；
 * 可见性判定统一走本服务，避免各处口径不一致。
 *
 * @author chronic
 */
public interface CdPatientDeptService {

    /** 去重后保留存在且启用的科室ID（不校验非空，供注册时暂存/校验使用） */
    List<Long> validDeptIds(Collection<Long> deptIds);

    /** 患者已选择的科室ID */
    List<Long> deptIdsOf(Long patientId);

    /** 批量查询：患者ID -> 科室ID列表 */
    java.util.Map<Long, List<Long>> deptIdsOfPatients(Collection<Long> patientIds);

    /** 覆盖保存患者科室（先删后插），科室需为启用状态且至少一个 */
    void replace(Long patientId, List<Long> deptIds, String source);

    /** 仅在患者尚未选择科室时补充（注册/建档时用） */
    void fillIfEmpty(Long patientId, List<Long> deptIds, String source);

    /** 删除患者的全部科室关联（删除档案时同步清理） */
    void removeByPatients(Collection<Long> patientIds);

    /** 某科室下可见的患者ID（管理端按科室筛选用） */
    Set<Long> patientIdsOfDept(Long deptId);

    /** 某批科室可见的患者ID（医生端数据范围用，多科室取并集） */
    Set<Long> patientIdsOfDepts(Collection<Long> deptIds);
}
