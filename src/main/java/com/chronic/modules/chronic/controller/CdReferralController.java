package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.ReferralAuditDTO;
import com.chronic.modules.chronic.entity.CdReferral;
import com.chronic.modules.chronic.query.ReferralQuery;
import com.chronic.modules.chronic.service.CdReferralService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 转诊管理
 *
 * @author chronic
 */
@Tag(name = "09-转诊管理", description = "转诊申请与审核")
@RestController
@RequestMapping("/chronic/referral")
@RequiredArgsConstructor
public class CdReferralController {

    private final CdReferralService referralService;

    @Operation(summary = "分页查询转诊列表")
    @PreAuthorize("@ss.hasPermi('chronic:referral:list')")
    @GetMapping("/list")
    public Result<PageResult<CdReferral>> list(ReferralQuery query) {
        return Result.success(referralService.queryPage(query));
    }

    @Operation(summary = "获取转诊详情")
    @PreAuthorize("@ss.hasPermi('chronic:referral:list')")
    @GetMapping("/{referralId}")
    public Result<CdReferral> getInfo(@PathVariable Long referralId) {
        return Result.success(referralService.getById(referralId));
    }

    @Operation(summary = "发起转诊申请")
    @PreAuthorize("@ss.hasPermi('chronic:referral:list')")
    @PostMapping
    public Result<Void> apply(@RequestBody CdReferral referral) {
        referralService.apply(referral);
        return Result.success();
    }

    @Operation(summary = "审核转诊")
    @PreAuthorize("@ss.hasPermi('chronic:referral:audit')")
    @PutMapping("/{referralId}/audit")
    public Result<Void> audit(@PathVariable Long referralId, @Valid @RequestBody ReferralAuditDTO dto) {
        referralService.audit(referralId, dto);
        return Result.success();
    }
}
