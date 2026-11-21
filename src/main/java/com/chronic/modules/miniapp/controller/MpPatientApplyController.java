package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpPatientApplyDTO;
import com.chronic.modules.miniapp.entity.MpPatientApply;
import com.chronic.modules.miniapp.service.MpPatientApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序患者建档申请
 *
 * @author chronic
 */
@Tag(name = "34-小程序-建档申请", description = "注册后提交建档申请，等待医生端审核")
@RestController
@RequestMapping("/mp/patient-apply")
@RequiredArgsConstructor
public class MpPatientApplyController {

    private final MpPatientApplyService applyService;

    @Operation(summary = "提交建档申请")
    @PostMapping
    public Result<Long> submit(@Valid @RequestBody MpPatientApplyDTO dto) {
        return Result.success("申请已提交，请等待医生审核建档", applyService.submit(dto));
    }

    @Operation(summary = "我的最新建档申请")
    @GetMapping("/mine")
    public Result<MpPatientApply> mine() {
        return Result.success(applyService.myLatest());
    }
}