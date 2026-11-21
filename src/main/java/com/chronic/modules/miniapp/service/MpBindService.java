package com.chronic.modules.miniapp.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.BindAuditDTO;
import com.chronic.modules.miniapp.dto.MpBindApplyDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;

import java.util.Map;

/**
 * 家属-患者绑定服务
 * <p>
 * 流程：家属提交申请 -> 患者在小程序确认 -> 医生端审核通过后生效。
 *
 * @author chronic
 */
public interface MpBindService {

    /** 家属提交绑定申请 */
    Long apply(MpBindApplyDTO dto);

    /** 家属查看自己的申请 */
    java.util.List<MpBindApply> myApplies();

    /** 患者查看待自己确认的申请 */
    java.util.List<MpBindApply> pendingForPatient();

    /** 患者确认/拒绝 */
    void patientConfirm(Long applyId, boolean agree);

    /** 家属解除绑定 */
    void unbind(Long patientId);

    /** 当前账号已绑定的患者关系 */
    java.util.List<Map<String, Object>> myRelations();

    /** 医生端：分页查询申请 */
    PageResult<MpBindApply> pageApply(Integer pageNum, Integer pageSize, String bindStatus);

    /** 医生端：审核 */
    void doctorAudit(Long applyId, BindAuditDTO dto);
}