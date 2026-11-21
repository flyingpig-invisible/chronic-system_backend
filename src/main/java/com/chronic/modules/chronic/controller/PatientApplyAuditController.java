package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.entity.MpPatientApply;
import com.chronic.modules.miniapp.service.MpPatientApplyService;
import com.chronic.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 患者端建档案审核（医生端）
 *
 * @author chronic
 */
@Tag(name = "23-建档审核", description = "审核患者端提交的建档申请")
@RestController
@RequestMapping("/chronic/patient-apply")
@RequiredArgsConstructor
public class PatientApplyAuditController {

    private final MpPatientApplyService applyService;

    @Operation(summary = "分页查询建档申请")
    @PreAuthorize("@ss.hasPermi('chronic:patient:list')")
    @GetMapping("/list")
    public Result<PageResult<MpPatientApply>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String applyStatus) {
        return Result.success(applyService.pageApply(pageNum, pageSize, applyStatus));
    }

    @Operation(summary = "待审核数量")
    @PreAuthorize("@ss.hasPermi('chronic:patient:list')")
    @GetMapping("/pending-count")
    public Result<Long> pendingCount() {
        return Result.success(applyService.pendingCount());
    }

    @Operation(summary = "审核建档申请")
    @PreAuthorize("@ss.hasPermi('chronic:patient:add')")
    @PutMapping("/{applyId}/audit")
    public Result<Long> audit(@PathVariable Long applyId,
                              @RequestParam String result,
                              @RequestParam(required = false) String auditOpinion) {
        Long patientId = applyService.audit(applyId, result, auditOpinion, SecurityUtils.getLoginUser().getUserId());
        return Result.success("审核完成", patientId);
    }
}