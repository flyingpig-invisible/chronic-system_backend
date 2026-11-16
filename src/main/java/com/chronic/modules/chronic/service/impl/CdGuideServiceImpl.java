package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.mapper.CdGuideMapper;
import com.chronic.modules.chronic.query.GuideQuery;
import com.chronic.modules.chronic.service.CdGuideService;
import com.chronic.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CdGuideServiceImpl extends ServiceImpl<CdGuideMapper, CdGuide> implements CdGuideService {

    @Override
    public PageResult<CdGuide> queryPage(GuideQuery query) {
        LambdaQueryWrapper<CdGuide> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdGuide::getPatientId, query.getPatientId())
         .eq(query.getWarningId() != null, CdGuide::getWarningId, query.getWarningId())
         .eq(StringUtils.hasText(query.getGuideType()), CdGuide::getGuideType, query.getGuideType())
         .eq(StringUtils.hasText(query.getReadStatus()), CdGuide::getReadStatus, query.getReadStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdGuide::getContent, query.getKeyword())
         .orderByDesc(CdGuide::getGuideTime);
        Page<CdGuide> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void addGuide(CdGuide guide) {
        if (guide.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        if (!StringUtils.hasText(guide.getContent())) {
            throw new ServiceException("指导内容不能为空");
        }
        guide.setGuideId(null);
        guide.setDoctorId(SecurityUtils.getUserId());
        guide.setDoctorName(SecurityUtils.getLoginUser().getRealName());
        guide.setGuideTime(LocalDateTime.now());
        if (!StringUtils.hasText(guide.getReadStatus())) {
            guide.setReadStatus("0");
        }
        save(guide);
    }

    @Override
    public void removeGuide(Long guideId) {
        removeById(guideId);
    }

    @Override
    public void markRead(Long guideId) {
        CdGuide guide = getById(guideId);
        if (guide == null) {
            throw new ServiceException("指导不存在");
        }
        guide.setReadStatus("1");
        updateById(guide);
    }
}
