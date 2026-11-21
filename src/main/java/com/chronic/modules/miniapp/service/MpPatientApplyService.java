package com.chronic.modules.miniapp.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.miniapp.dto.MpPatientApplyDTO;
import com.chronic.modules.miniapp.entity.MpPatientApply;

import java.util.List;

/**
 * 患者端建档案申请：小程序提交，医生端审核建档
 *
 * @author chronic
 */
public interface MpPatientApplyService {

    /** 患者端提交建档申请（同一账号只会存在一条待审核申请） */
    Long submit(MpPatientApplyDTO dto);

    /** 患者端查看自己的最新申请 */
    MpPatientApply myLatest();

    /** 医生端分页查询 */
    PageResult<MpPatientApply> pageApply(Integer pageNum, Integer pageSize, String applyStatus);

    /** 医生端审核：通过则按申请信息创建档案并关联小程序账号 */
    Long audit(Long applyId, String result, String auditOpinion, Long doctorId);

    /** 待审核数量 */
    long pendingCount();
}