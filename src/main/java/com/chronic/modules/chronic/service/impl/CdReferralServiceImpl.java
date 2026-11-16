package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ReferralAuditDTO;
import com.chronic.modules.chronic.entity.CdReferral;
import com.chronic.modules.chronic.mapper.CdReferralMapper;
import com.chronic.modules.chronic.query.ReferralQuery;
import com.chronic.modules.chronic.service.CdReferralService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CdReferralServiceImpl extends ServiceImpl<CdReferralMapper, CdReferral> implements CdReferralService {

    @Override
    public PageResult<CdReferral> queryPage(ReferralQuery query) {
        LambdaQueryWrapper<CdReferral> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdReferral::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getUrgency()), CdReferral::getUrgency, query.getUrgency())
         .eq(StringUtils.hasText(query.getReferralStatus()), CdReferral::getReferralStatus, query.getReferralStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdReferral::getPatientName, query.getKeyword())
         .orderByDesc(CdReferral::getCreateTime);
        Page<CdReferral> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void apply(CdReferral referral) {
        if (referral.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        if (!StringUtils.hasText(referral.getTargetHospital())) {
            throw new ServiceException("接收医院不能为空");
        }
        referral.setReferralId(null);
        referral.setReferralStatus("0");
        referral.setApplyTime(LocalDateTime.now());
        save(referral);
    }

    @Override
    public void audit(Long referralId, ReferralAuditDTO dto) {
        CdReferral referral = getById(referralId);
        if (referral == null) {
            throw new ServiceException("转诊记录不存在");
        }
        if (!"0".equals(referral.getReferralStatus())) {
            throw new ServiceException("该转诊已审核，无法重复操作");
        }
        referral.setReferralStatus(dto.getReferralStatus());
        referral.setAuditOpinion(dto.getAuditOpinion());
        referral.setReceiveHospital(dto.getReceiveHospital());
        referral.setReceiveDept(dto.getReceiveDept());
        referral.setAuditTime(LocalDateTime.now());
        updateById(referral);
    }
}
