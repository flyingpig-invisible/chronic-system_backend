package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdPlan;
import com.chronic.modules.chronic.query.PlanQuery;
import com.chronic.modules.chronic.service.CdPlanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 健康管理计划
 *
 * @author chronic
 */
@Tag(name = "06-健康管理计划", description = "个性化健康管理计划与执行项")
@RestController
@RequestMapping("/chronic/plan")
@RequiredArgsConstructor
public class CdPlanController {

    private final CdPlanService planService;

    @Operation(summary = "分页查询计划列表")
    @PreAuthorize("@ss.hasPermi('chronic:plan:list')")
    @GetMapping("/list")
    public Result<PageResult<CdPlan>> list(PlanQuery query) {
        return Result.success(planService.queryPage(query));
    }

    @Operation(summary = "获取计划详情（含执行项）")
    @PreAuthorize("@ss.hasPermi('chronic:plan:query')")
    @GetMapping("/{planId}")
    public Result<CdPlan> getInfo(@PathVariable Long planId) {
        return Result.success(planService.getDetail(planId));
    }

    @Operation(summary = "新增或修改计划")
    @PreAuthorize("@ss.hasPermi('chronic:plan:edit')")
    @PostMapping
    public Result<Void> save(@RequestBody CdPlan plan) {
        planService.savePlan(plan);
        return Result.success();
    }

    @Operation(summary = "修改计划状态")
    @PreAuthorize("@ss.hasPermi('chronic:plan:edit')")
    @PutMapping("/{planId}/status")
    public Result<Void> updateStatus(@PathVariable Long planId, @RequestParam String planStatus) {
        planService.updateStatus(planId, planStatus);
        return Result.success();
    }

    @Operation(summary = "删除计划")
    @PreAuthorize("@ss.hasPermi('chronic:plan:remove')")
    @DeleteMapping("/{planIds}")
    public Result<Void> remove(@PathVariable Long[] planIds) {
        for (Long id : planIds) {
            planService.removePlan(id);
        }
        return Result.success();
    }
}
