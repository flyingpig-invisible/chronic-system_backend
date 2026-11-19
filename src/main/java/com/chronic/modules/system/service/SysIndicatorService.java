package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.query.IndicatorQuery;

import java.util.List;

public interface SysIndicatorService extends IService<SysIndicator> {

    PageResult<SysIndicator> pageIndicator(IndicatorQuery query);

    /** 全部启用指标（按显示顺序），供录入、评分等模块下拉使用 */
    List<SysIndicator> listEnabled();

    void addIndicator(SysIndicator indicator);

    void updateIndicator(SysIndicator indicator);

    void removeIndicators(Long[] indicatorIds);
}