package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdPlan;
import com.chronic.modules.chronic.query.PlanQuery;

public interface CdPlanService extends IService<CdPlan> {

    PageResult<CdPlan> queryPage(PlanQuery query);

    CdPlan getDetail(Long planId);

    void savePlan(CdPlan plan);

    void removePlan(Long planId);

    void updateStatus(Long planId, String planStatus);
}
