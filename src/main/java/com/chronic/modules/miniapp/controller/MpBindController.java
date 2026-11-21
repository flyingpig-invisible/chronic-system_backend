package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpBindApplyDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.service.MpBindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 小程序家属绑定
 *
 * @author chronic
 */
@Tag(name = "33-小程序-家属绑定", description = "家属申请 / 患者确认 / 解除绑定")
@RestController
@RequestMapping("/mp/bind")
@RequiredArgsConstructor
public class MpBindController {

    private final MpBindService bindService;

    @Operation(summary = "家属提交绑定申请")
    @PostMapping("/apply")
    public Result<Long> apply(@Valid @RequestBody MpBindApplyDTO dto) {
        return Result.success("申请已提交，等待患者确认", bindService.apply(dto));
    }

    @Operation(summary = "我的绑定申请")
    @GetMapping("/applies")
    public Result<List<MpBindApply>> applies() {
        return Result.success(bindService.myApplies());
    }

    @Operation(summary = "待患者确认的申请")
    @GetMapping("/pending")
    public Result<List<MpBindApply>> pending() {
        return Result.success(bindService.pendingForPatient());
    }

    @Operation(summary = "患者确认 / 拒绝")
    @PutMapping("/{applyId}/confirm")
    public Result<Void> confirm(@PathVariable Long applyId, @RequestParam boolean agree) {
        bindService.patientConfirm(applyId, agree);
        return Result.success(agree ? "已同意绑定" : "已拒绝绑定", null);
    }

    @Operation(summary = "已绑定亲属")
    @GetMapping("/relations")
    public Result<List<Map<String, Object>>> relations() {
        return Result.success(bindService.myRelations());
    }

    @Operation(summary = "解除绑定")
    @DeleteMapping("/relations/{patientId}")
    public Result<Void> unbind(@PathVariable Long patientId) {
        bindService.unbind(patientId);
        return Result.success("已解除绑定", null);
    }
}