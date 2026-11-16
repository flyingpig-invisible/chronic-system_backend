package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.BindApplyDTO;
import com.chronic.modules.miniapp.dto.BindAuditDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.service.MpBindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 家属-患者绑定（小程序端）
 *
 * @author chronic
 */
@Tag(name = "21-家属绑定", description = "家属发起绑定、患者同意")
@RestController
@RequestMapping("/mp/bind")
@RequiredArgsConstructor
public class MpBindController {

    private final MpBindService bindService;

    @Operation(summary = "家属发起绑定申请")
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody BindApplyDTO dto) {
        return Result.success(bindService.applyBind(dto));
    }

    @Operation(summary = "患者确认绑定")
    @PutMapping("/{applyId}/confirm")
    public Result<Void> confirm(@PathVariable Long applyId, @Valid @RequestBody BindAuditDTO dto) {
        bindService.patientConfirm(applyId, dto);
        return Result.success();
    }

    @Operation(summary = "查询我的绑定申请")
    @GetMapping("/my/apply")
    public Result<List<MpBindApply>> myApplies() {
        return Result.success(bindService.myApplies());
    }

    @Operation(summary = "查询我绑定的患者")
    @GetMapping("/my/patients")
    public Result<List<MpFamilyPatient>> myPatients() {
        return Result.success(bindService.myPatients());
    }

    @Operation(summary = "解除绑定")
    @DeleteMapping("/{id}")
    public Result<Void> unbind(@PathVariable Long id) {
        bindService.unbind(id);
        return Result.success();
    }
}
