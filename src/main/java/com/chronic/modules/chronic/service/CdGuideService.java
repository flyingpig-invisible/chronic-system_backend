package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.query.GuideQuery;

public interface CdGuideService extends IService<CdGuide> {

    PageResult<CdGuide> queryPage(GuideQuery query);

    void addGuide(CdGuide guide);

    void removeGuide(Long guideId);

    void markRead(Long guideId);
}
