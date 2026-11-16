package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdPlan;
import com.chronic.modules.chronic.entity.CdPlanItem;
import com.chronic.modules.chronic.mapper.CdPlanItemMapper;
import com.chronic.modules.chronic.mapper.CdPlanMapper;
import com.chronic.modules.chronic.query.PlanQuery;
import com.chronic.modules.chronic.service.CdPlanService;
import com.chronic.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CdPlanServiceImpl extends ServiceImpl<CdPlanMapper, CdPlan> implements CdPlanService {

    @Autowired
    private CdPlanItemMapper planItemMapper;

    @Override
    public PageResult<CdPlan> queryPage(PlanQuery query) {
        LambdaQueryWrapper<CdPlan> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdPlan::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getDiseaseCode()), CdPlan::getDiseaseCode, query.getDiseaseCode())
         .eq(StringUtils.hasText(query.getPlanStatus()), CdPlan::getPlanStatus, query.getPlanStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdPlan::getPlanName, query.getKeyword())
         .orderByDesc(CdPlan::getCreateTime);
        Page<CdPlan> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public CdPlan getDetail(Long planId) {
        CdPlan plan = getById(planId);
        if (plan == null) {
            throw new ServiceException("计划不存在");
        }
        List<CdPlanItem> items = planItemMapper.selectList(
                new LambdaQueryWrapper<CdPlanItem>().eq(CdPlanItem::getPlanId, planId));
        plan.setItems(items);
        return plan;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePlan(CdPlan plan) {
        if (plan.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        if (!StringUtils.hasText(plan.getPlanName())) {
            throw new ServiceException("计划名称不能为空");
        }
        plan.setDoctorId(SecurityUtils.getUserId());
        plan.setDoctorName(SecurityUtils.getLoginUser().getRealName());
        if (!StringUtils.hasText(plan.getPlanStatus())) {
            plan.setPlanStatus("0");
        }
        if (plan.getExecuteRate() == null) {
            plan.setExecuteRate(0);
        }
        saveOrUpdate(plan);
        Long planId = plan.getPlanId();
        planItemMapper.delete(new LambdaQueryWrapper<CdPlanItem>().eq(CdPlanItem::getPlanId, planId));
        if (plan.getItems() != null) {
            for (CdPlanItem item : plan.getItems()) {
                item.setItemId(null);
                item.setPlanId(planId);
                planItemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePlan(Long planId) {
        removeById(planId);
        planItemMapper.delete(new LambdaQueryWrapper<CdPlanItem>().eq(CdPlanItem::getPlanId, planId));
    }

    @Override
    public void updateStatus(Long planId, String planStatus) {
        CdPlan plan = getById(planId);
        if (plan == null) {
            throw new ServiceException("计划不存在");
        }
        plan.setPlanStatus(planStatus);
        updateById(plan);
    }
}
