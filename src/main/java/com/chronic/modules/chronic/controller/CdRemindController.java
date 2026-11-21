package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdRemindLog;
import com.chronic.modules.chronic.query.RemindQuery;
import com.chronic.modules.chronic.service.CdRemindService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用药提醒
 *
 * @author chronic
 */
@Tag(name = "07-用药提醒", description = "用药计划与服药打卡")
@RestController
@RequestMapping("/chronic/remind")
@RequiredArgsConstructor
public class CdRemindController {

    private final CdRemindService remindService;

    @Operation(summary = "分页查询用药提醒列表")
    @PreAuthorize("@ss.hasPermi('chronic:remind:list')")
    @GetMapping("/list")
    public Result<PageResult<CdRemind>> list(RemindQuery query) {
        return Result.success(remindService.queryPage(query));
    }

    @Operation(summary = "获取用药提醒详情")
    @PreAuthorize("@ss.hasPermi('chronic:remind:query')")
    @GetMapping("/{remindId}")
    public Result<CdRemind> getInfo(@PathVariable Long remindId) {
        return Result.success(remindService.getById(remindId));
    }

    @Operation(summary = "新增或修改用药提醒")
    @PreAuthorize("@ss.hasPermi('chronic:remind:edit')")
    @PostMapping
    public Result<Void> save(@RequestBody CdRemind remind) {
        remindService.saveRemind(remind);
        return Result.success();
    }

    @Operation(summary = "启用/停用提醒")
    @PreAuthorize("@ss.hasPermi('chronic:remind:edit')")
    @PutMapping("/{remindId}/status")
    public Result<Void> changeStatus(@PathVariable Long remindId, @RequestParam String status) {
        remindService.changeStatus(remindId, status);
        return Result.success();
    }

    @Operation(summary = "删除用药提醒")
    @PreAuthorize("@ss.hasPermi('chronic:remind:remove')")
    @DeleteMapping("/{remindIds}")
    public Result<Void> remove(@PathVariable Long[] remindIds) {
        for (Long id : remindIds) {
            remindService.removeRemind(id);
        }
        return Result.success();
    }

    @Operation(summary = "查询今日用药打卡")
    @PreAuthorize("@ss.hasPermi('chronic:remind:list')")
    @GetMapping("/today/{patientId}")
    public Result<List<CdRemindLog>> today(@PathVariable Long patientId) {
        return Result.success(remindService.todayLog(patientId));
    }

    @Operation(summary = "用药提醒的打卡记录")
    @PreAuthorize("@ss.hasPermi('chronic:remind:list')")
    @GetMapping("/{remindId}/logs")
    public Result<List<CdRemindLog>> logs(@PathVariable Long remindId,
                                       @RequestParam(required = false) Integer days) {
        return Result.success(remindService.logOfRemind(remindId, days));
    }

    @Operation(summary = "服药打卡")
    @PreAuthorize("@ss.hasPermi('chronic:remind:edit')")
    @PutMapping("/{remindId}/check")
    public Result<Void> check(@PathVariable Long remindId, @RequestParam String checkStatus) {
        remindService.check(remindId, checkStatus);
        return Result.success();
    }
}
