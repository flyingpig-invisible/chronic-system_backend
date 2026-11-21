package com.chronic.modules.miniapp.service;

import com.chronic.modules.miniapp.vo.MpDeptVO;

import java.util.List;

/**
 * 小程序科室服务
 * <p>
 * 患者可见范围以 cd_patient_dept 为准：患者自选科室后，只有这些科室的医生能看到其信息。
 *
 * @author chronic
 */
public interface MpDeptService {

    /** 启用中的科室（下拉选项） */
    List<MpDeptVO> options();

    /** 当前患者档案已选科室 */
    List<Long> selectedDeptIds(Long patientId);

    /** 覆盖保存患者自选科室（来源=患者自选） */
    void setSelectedDeptIds(Long patientId, List<Long> deptIds);
}