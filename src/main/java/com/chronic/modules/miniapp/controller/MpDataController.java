package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.service.MpDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 小程序端患者数据查询
 *
 * @author chronic
 */
@Tag(name = "23-小程序数据", description = "家属/患者查看患者健康数据")
@RestController
@RequestMapping("/mp/data")
@RequiredArgsConstructor
public class MpDataController {

    private final MpDataService dataService;

    @Operation(summary = "查询我绑定的患者列表")
    @GetMapping("/patient/list")
    public Result<List<Map<String, Object>>> patientList() {
        return Result.success(dataService.myPatientList());
    }

    @Operation(summary = "患者健康概览")
    @GetMapping("/patient/{patientId}/overview")
    public Result<Map<String, Object>> overview(@PathVariable Long patientId) {
        return Result.success(dataService.overview(patientId));
    }

    @Operation(summary = "患者监测数据")
    @GetMapping("/patient/{patientId}/measure")
    public Result<List<Map<String, Object>>> measures(@PathVariable Long patientId,
                                                      @RequestParam(required = false) String indicatorCode,
                                                      @RequestParam(required = false) Integer limit) {
        return Result.success(dataService.measures(patientId, indicatorCode, limit));
    }

    @Operation(summary = "患者预警列表")
    @GetMapping("/patient/{patientId}/warning")
    public Result<List<Map<String, Object>>> warnings(@PathVariable Long patientId) {
        return Result.success(dataService.warnings(patientId));
    }

    @Operation(summary = "患者健康计划")
    @GetMapping("/patient/{patientId}/plan")
    public Result<List<Map<String, Object>>> plans(@PathVariable Long patientId) {
        return Result.success(dataService.plans(patientId));
    }

    @Operation(summary = "患者用药提醒")
    @GetMapping("/patient/{patientId}/remind")
    public Result<List<Map<String, Object>>> reminds(@PathVariable Long patientId) {
        return Result.success(dataService.reminds(patientId));
    }

    @Operation(summary = "患者远程指导")
    @GetMapping("/patient/{patientId}/guide")
    public Result<List<Map<String, Object>>> guides(@PathVariable Long patientId) {
        return Result.success(dataService.guides(patientId));
    }

    @Operation(summary = "科普文章")
    @GetMapping("/patient/{patientId}/article")
    public Result<List<Map<String, Object>>> articles(@PathVariable Long patientId) {
        return Result.success(dataService.articles(patientId));
    }
}
