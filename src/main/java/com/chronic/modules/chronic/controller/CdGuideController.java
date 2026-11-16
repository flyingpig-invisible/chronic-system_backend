package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.query.GuideQuery;
import com.chronic.modules.chronic.service.CdGuideService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 远程指导
 *
 * @author chronic
 */
@Tag(name = "05-远程指导", description = "医生远程健康指导")
@RestController
@RequestMapping("/chronic/guide")
@RequiredArgsConstructor
public class CdGuideController {

    private final CdGuideService guideService;

    @Operation(summary = "分页查询指导列表")
    @PreAuthorize("@ss.hasPermi('chronic:guide:list')")
    @GetMapping("/list")
    public Result<PageResult<CdGuide>> list(GuideQuery query) {
        return Result.success(guideService.queryPage(query));
    }

    @Operation(summary = "获取指导详情")
    @PreAuthorize("@ss.hasPermi('chronic:guide:query')")
    @GetMapping("/{guideId}")
    public Result<CdGuide> getInfo(@PathVariable Long guideId) {
        return Result.success(guideService.getById(guideId));
    }

    @Operation(summary = "按患者查询指导")
    @PreAuthorize("@ss.hasPermi('chronic:guide:list')")
    @GetMapping("/patient/{patientId}")
    public Result<List<CdGuide>> byPatient(@PathVariable Long patientId) {
        GuideQuery query = new GuideQuery();
        query.setPatientId(patientId);
        query.setPageSize(100);
        return Result.success(guideService.queryPage(query).getRows());
    }

    @Operation(summary = "新增指导")
    @PreAuthorize("@ss.hasPermi('chronic:guide:add')")
    @PostMapping
    public Result<Void> add(@RequestBody CdGuide guide) {
        guideService.addGuide(guide);
        return Result.success();
    }

    @Operation(summary = "修改指导")
    @PreAuthorize("@ss.hasPermi('chronic:guide:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody CdGuide guide) {
        guideService.updateById(guide);
        return Result.success();
    }

    @Operation(summary = "删除指导")
    @PreAuthorize("@ss.hasPermi('chronic:guide:remove')")
    @DeleteMapping("/{guideIds}")
    public Result<Void> remove(@PathVariable Long[] guideIds) {
        for (Long id : guideIds) {
            guideService.removeGuide(id);
        }
        return Result.success();
    }

    @Operation(summary = "标记已读")
    @PreAuthorize("@ss.hasPermi('chronic:guide:edit')")
    @PutMapping("/{guideId}/read")
    public Result<Void> markRead(@PathVariable Long guideId) {
        guideService.markRead(guideId);
        return Result.success();
    }
}
