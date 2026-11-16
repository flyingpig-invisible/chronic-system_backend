package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.common.utils.ExcelUtils;
import com.chronic.modules.chronic.dto.EmergencyLinkDTO;
import com.chronic.modules.chronic.dto.WarningHandleDTO;
import com.chronic.modules.chronic.entity.CdEmergencyLink;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.query.WarningQuery;
import com.chronic.modules.chronic.service.CdWarningService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 预警中心
 *
 * @author chronic
 */
@Tag(name = "04-预警中心", description = "分级预警、处置、急救联动")
@RestController
@RequestMapping("/chronic/warning")
@RequiredArgsConstructor
public class CdWarningController {

    private final CdWarningService warningService;

    @Operation(summary = "分页查询预警列表")
    @PreAuthorize("@ss.hasPermi('chronic:warning:list')")
    @GetMapping("/list")
    public Result<PageResult<CdWarning>> list(WarningQuery query) {
        return Result.success(warningService.pageWarning(query));
    }

    @Operation(summary = "获取预警详情")
    @PreAuthorize("@ss.hasPermi('chronic:warning:query')")
    @GetMapping("/{warningId}")
    public Result<CdWarning> getInfo(@PathVariable Long warningId) {
        return Result.success(warningService.getWarning(warningId));
    }

    @Operation(summary = "处置预警")
    @PreAuthorize("@ss.hasPermi('chronic:warning:handle')")
    @PutMapping("/{warningId}/handle")
    public Result<Void> handle(@PathVariable Long warningId, @Valid @RequestBody WarningHandleDTO dto) {
        warningService.handleWarning(warningId, dto);
        return Result.success();
    }

    @Operation(summary = "发起急救联动")
    @PreAuthorize("@ss.hasPermi('chronic:warning:handle')")
    @PostMapping("/{warningId}/emergency")
    public Result<Void> linkEmergency(@PathVariable Long warningId, @Valid @RequestBody EmergencyLinkDTO dto) {
        warningService.linkEmergency(warningId, dto);
        return Result.success();
    }

    @Operation(summary = "查询预警的急救联动记录")
    @PreAuthorize("@ss.hasPermi('chronic:warning:query')")
    @GetMapping("/{warningId}/emergency/list")
    public Result<List<CdEmergencyLink>> emergencyByWarning(@PathVariable Long warningId) {
        return Result.success(warningService.listEmergencyByWarning(warningId));
    }

    @Operation(summary = "分页查询急救联动记录")
    @PreAuthorize("@ss.hasPermi('chronic:warning:query')")
    @GetMapping("/emergency/list")
    public Result<PageResult<CdEmergencyLink>> emergencyList(WarningQuery query) {
        return Result.success(warningService.pageEmergency(query));
    }

    @Operation(summary = "按级别统计预警")
    @PreAuthorize("@ss.hasPermi('chronic:warning:list')")
    @GetMapping("/stat/level")
    public Result<List<Map<String, Object>>> statLevel(WarningQuery query) {
        return Result.success(warningService.statByLevel(query));
    }

    @Operation(summary = "统计预警响应时效")
    @PreAuthorize("@ss.hasPermi('chronic:warning:list')")
    @GetMapping("/stat/response")
    public Result<Map<String, Object>> statResponse(WarningQuery query) {
        return Result.success(warningService.statResponse(query));
    }

    @Operation(summary = "导出预警列表")
    @PreAuthorize("@ss.hasPermi('chronic:warning:list')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, WarningQuery query) {
        List<CdWarning> list = warningService.listForExport(query);
        List<Object[]> rows = new ArrayList<>();
        Map<String, String> levelMap = Map.of("1", "低危", "2", "中危", "3", "高危");
        Map<String, String> statusMap = Map.of("0", "待处理", "1", "处理中", "2", "已处理");
        for (CdWarning w : list) {
            rows.add(new Object[]{
                    w.getPatientName(), w.getIndicatorName(), w.getIndicatorValue(), w.getUnit(),
                    w.getThresholdDesc(),
                    levelMap.getOrDefault(w.getWarningLevel(), ""),
                    statusMap.getOrDefault(w.getHandleStatus(), ""),
                    w.getTriggerTime(), w.getHandleBy()
            });
        }
        ExcelUtils.export(response, "预警列表", "预警列表",
                new String[]{"患者姓名", "指标名称", "数值", "单位", "阈值", "预警级别", "处置状态", "触发时间", "处置人"}, rows);
    }
}
