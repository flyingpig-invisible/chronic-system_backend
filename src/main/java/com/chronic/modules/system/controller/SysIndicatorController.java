package com.chronic.modules.system.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.query.IndicatorQuery;
import com.chronic.modules.system.service.SysIndicatorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 指标管理（系统指标字典，与病种无关）
 *
 * @author chronic
 */
@Tag(name = "35-指标管理", description = "系统指标字典维护")
@RestController
@RequestMapping("/system/indicator")
@RequiredArgsConstructor
public class SysIndicatorController {

    private final SysIndicatorService indicatorService;

    @Operation(summary = "分页查询指标")
    @PreAuthorize("@ss.hasPermi('system:indicator:list')")
    @GetMapping("/list")
    public Result<PageResult<SysIndicator>> list(IndicatorQuery query) {
        return Result.success(indicatorService.pageIndicator(query));
    }

    @Operation(summary = "查询全部启用指标（下拉）")
    @GetMapping("/options")
    public Result<List<SysIndicator>> options() {
        return Result.success(indicatorService.listEnabled());
    }

    @Operation(summary = "获取指标详情")
    @PreAuthorize("@ss.hasPermi('system:indicator:query')")
    @GetMapping("/{indicatorId}")
    public Result<SysIndicator> getInfo(@PathVariable Long indicatorId) {
        return Result.success(indicatorService.getById(indicatorId));
    }

    @Operation(summary = "新增指标")
    @PreAuthorize("@ss.hasPermi('system:indicator:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysIndicator indicator) {
        indicatorService.addIndicator(indicator);
        return Result.success();
    }

    @Operation(summary = "修改指标")
    @PreAuthorize("@ss.hasPermi('system:indicator:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysIndicator indicator) {
        indicatorService.updateIndicator(indicator);
        return Result.success();
    }

    @Operation(summary = "删除指标")
    @PreAuthorize("@ss.hasPermi('system:indicator:remove')")
    @DeleteMapping("/{indicatorIds}")
    public Result<Void> remove(@PathVariable Long[] indicatorIds) {
        indicatorService.removeIndicators(indicatorIds);
        return Result.success();
    }
}