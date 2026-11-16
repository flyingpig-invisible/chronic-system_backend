package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ReferralAuditDTO;
import com.chronic.modules.chronic.entity.CdReferral;
import com.chronic.modules.chronic.query.ReferralQuery;

public interface CdReferralService extends IService<CdReferral> {

    PageResult<CdReferral> queryPage(ReferralQuery query);

    void apply(CdReferral referral);

    void audit(Long referralId, ReferralAuditDTO dto);
}
