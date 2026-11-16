package com.chronic.modules.miniapp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.miniapp.dto.BindApplyDTO;
import com.chronic.modules.miniapp.dto.BindAuditDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;

import java.util.List;

public interface MpBindService extends IService<MpBindApply> {

    Long applyBind(BindApplyDTO dto);

    void patientConfirm(Long applyId, BindAuditDTO dto);

    void doctorAudit(Long applyId, BindAuditDTO dto);

    PageResult<MpBindApply> pageApply(Integer pageNum, Integer pageSize, String bindStatus);

    List<MpBindApply> myApplies();

    List<MpFamilyPatient> myPatients();

    void unbind(Long id);
}
