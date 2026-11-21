package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.ClinicFinishDTO;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.chronic.query.ClinicQuery;
import com.chronic.modules.chronic.service.CdClinicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 线上就诊
 *
 * @author chronic
 */
@Tag(name = "08-线上就诊", description = "图文/视频问诊、接诊、完成就诊")
@RestController
@RequestMapping("/chronic/clinic")
@RequiredArgsConstructor
public class CdClinicController {

    private final CdClinicService clinicService;

    @Operation(summary = "分页查询就诊列表")
    @PreAuthorize("@ss.hasPermi('chronic:clinic:list')")
    @GetMapping("/list")
    public Result<PageResult<CdClinic>> list(ClinicQuery query) {
        return Result.success(clinicService.queryPage(query));
    }

    @Operation(summary = "获取就诊详情")
    @PreAuthorize("@ss.hasPermi('chronic:clinic:query')")
    @GetMapping("/{visitId}")
    public Result<CdClinic> getInfo(@PathVariable Long visitId) {
        return Result.success(clinicService.getById(visitId));
    }


    @Operation(summary = "医生接诊")
    @PreAuthorize("@ss.hasPermi('chronic:clinic:handle')")
    @PutMapping("/{visitId}/accept")
    public Result<Void> accept(@PathVariable Long visitId) {
        clinicService.accept(visitId);
        return Result.success();
    }

    @Operation(summary = "完成就诊")
    @PreAuthorize("@ss.hasPermi('chronic:clinic:handle')")
    @PutMapping("/{visitId}/finish")
    public Result<Void> finish(@PathVariable Long visitId, @Valid @RequestBody ClinicFinishDTO dto) {
        clinicService.finish(visitId, dto);
        return Result.success();
    }

    @Operation(summary = "取消就诊")
    @PreAuthorize("@ss.hasPermi('chronic:clinic:handle')")
    @PutMapping("/{visitId}/cancel")
    public Result<Void> cancel(@PathVariable Long visitId) {
        clinicService.cancel(visitId);
        return Result.success();
    }
}
