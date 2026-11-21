package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.BindAuditDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.service.MpBindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 绑定审核（医生端）
 *
 * @author chronic
 */
@Tag(name = "22-绑定审核", description = "医生审核家属-患者绑定申请")
@RestController("bindAuditController")
@RequestMapping("/chronic/bind")
@RequiredArgsConstructor
public class BindAuditController {

    private final MpBindService bindService;

    @Operation(summary = "分页查询绑定申请")
    @PreAuthorize("@ss.hasPermi('chronic:bind:list')")
    @GetMapping("/list")
    public Result<PageResult<MpBindApply>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                @RequestParam(defaultValue = "10") Integer pageSize,
                                                @RequestParam(required = false) String bindStatus) {
        return Result.success(bindService.pageApply(pageNum, pageSize, bindStatus));
    }

    @Operation(summary = "医生审核绑定申请")
    @PreAuthorize("@ss.hasPermi('chronic:bind:audit')")
    @PutMapping("/{applyId}/audit")
    public Result<Void> audit(@PathVariable Long applyId, @Valid @RequestBody BindAuditDTO dto) {
        bindService.doctorAudit(applyId, dto);
        return Result.success();
    }
}