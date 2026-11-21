package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.service.MpCareService;
import com.chronic.modules.miniapp.vo.MpGuideVO;
import com.chronic.modules.miniapp.vo.MpMedicationVO;
import com.chronic.modules.miniapp.vo.MpPlanVO;
import com.chronic.modules.miniapp.vo.MpWarningVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序预警 / 指导 / 计划 / 用药
 *
 * @author chronic
 */
@Tag(name = "32-小程序-健康管理", description = "预警 / 医生指导 / 管理计划 / 用药打卡")
@RestController
@RequestMapping("/mp")
@RequiredArgsConstructor
public class MpCareController {

    private final MpCareService careService;

    @Operation(summary = "预警列表")
    @GetMapping("/warning/list")
    public Result<List<MpWarningVO>> warnings(@RequestParam(required = false) Long patientId,
                                               @RequestParam(required = false) String level) {
        return Result.success(careService.warnings(patientId, level));
    }

    @Operation(summary = "预警详情")
    @GetMapping("/warning/{warningId}")
    public Result<MpWarningVO> warning(@PathVariable Long warningId) {
        return Result.success(careService.warningDetail(warningId));
    }

    @Operation(summary = "医生指导列表")
    @GetMapping("/guide/list")
    public Result<List<MpGuideVO>> guides(@RequestParam(required = false) Long patientId) {
        return Result.success(careService.guides(patientId));
    }

    @Operation(summary = "标记指导已读")
    @PutMapping("/guide/{guideId}/read")
    public Result<Void> readGuide(@PathVariable Long guideId) {
        careService.readGuide(guideId);
        return Result.success();
    }

    @Operation(summary = "健康管理计划")
    @GetMapping("/plan/list")
    public Result<List<MpPlanVO>> plans(@RequestParam(required = false) Long patientId) {
        return Result.success(careService.plans(patientId));
    }

    @Operation(summary = "计划详情")
    @GetMapping("/plan/{planId}")
    public Result<MpPlanVO> plan(@PathVariable Long planId) {
        return Result.success(careService.planDetail(planId));
    }

    @Operation(summary = "今日用药")
    @GetMapping("/medication/today")
    public Result<List<MpMedicationVO>> todayMedication(@RequestParam(required = false) Long patientId) {
        return Result.success(careService.todayMedication(patientId));
    }

    @Operation(summary = "指定日期用药记录")
    @GetMapping("/medication/day")
    public Result<List<MpMedicationVO>> medicationOfDay(@RequestParam(required = false) Long patientId,
                                                         @RequestParam(required = false) String date) {
        return Result.success(careService.medicationOfDay(patientId, date));
    }

    @Operation(summary = "用药打卡")
    @PostMapping("/medication/check")
    public Result<Void> check(@RequestParam(required = false) Long patientId,
                              @RequestParam Long remindId,
                              @RequestParam String planTime) {
        careService.checkMedication(patientId, remindId, planTime);
        return Result.success("打卡成功", null);
    }
}