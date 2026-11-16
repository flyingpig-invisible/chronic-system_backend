package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.query.GuideQuery;

import java.util.List;
import java.util.Map;

public interface CdGuideService extends IService<CdGuide> {

    PageResult<CdGuide> queryPage(GuideQuery query);

    void addGuide(CdGuide guide);

    void removeGuide(Long guideId);

    void markRead(Long guideId);

    /** 查询可用指导类型（字典 guide_type） */
    List<Map<String, String>> typeOptions();

    /** 新增指导类型，仅医生与超级管理员可操作 */
    void addType(String label);
}
