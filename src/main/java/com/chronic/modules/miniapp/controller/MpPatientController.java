package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.miniapp.service.MpPatientService;
import com.chronic.modules.miniapp.vo.MpDashboardVO;
import com.chronic.modules.miniapp.vo.MpDiseaseVO;
import com.chronic.modules.miniapp.vo.MpIndicatorVO;
import com.chronic.modules.miniapp.vo.MpPatientVO;
import com.chronic.modules.miniapp.vo.MpTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序患者档案与监测数据
 * <p>
 * 患者ID均可省略：患者账号默认取本人档案，家属账号默认取第一个已绑定亲属。
 *
 * @author chronic
 */
@Tag(name = "31-小程序-患者档案", description = "档案 / 指标 / 趋势 / 首页概览 / 手动录入")
@RestController
@RequestMapping("/mp/patient")
@RequiredArgsConstructor
public class MpPatientController {

    private final MpPatientService patientService;

    @Operation(summary = "可查看的患者（本人或已绑定亲属）")
    @GetMapping("/list")
    public Result<List<MpPatientVO>> list() {
        return Result.success(patientService.listPatients());
    }

    @Operation(summary = "患者档案")
    @GetMapping({"/detail", "/{patientId}"})
    public Result<MpPatientVO> detail(@PathVariable(required = false) Long patientId) {
        return Result.success(patientService.patientDetail(patientId));
    }

    @Operation(summary = "确诊病种")
    @GetMapping({"/diseases", "/{patientId}/diseases"})
    public Result<List<MpDiseaseVO>> diseases(@PathVariable(required = false) Long patientId) {
        return Result.success(patientService.diseases(patientId));
    }

    @Operation(summary = "最新指标快照")
    @GetMapping({"/indicators", "/{patientId}/indicators"})
    public Result<List<MpIndicatorVO>> indicators(@PathVariable(required = false) Long patientId) {
        return Result.success(patientService.latestIndicators(patientId));
    }

    @Operation(summary = "首页概览")
    @GetMapping({"/dashboard", "/{patientId}/dashboard"})
    public Result<MpDashboardVO> dashboard(@PathVariable(required = false) Long patientId) {
        return Result.success(patientService.dashboard(patientId));
    }

    @Operation(summary = "指标趋势")
    @GetMapping({"/trend", "/{patientId}/trend"})
    public Result<List<MpTrendVO>> trend(@PathVariable(required = false) Long patientId,
                                          @RequestParam(required = false, defaultValue = "7") Integer days) {
        return Result.success(patientService.trend(patientId, days));
    }

    @Operation(summary = "手动录入监测数据")
    @PostMapping("/measure")
    public Result<Long> addMeasure(@RequestBody CdMeasure measure) {
        return Result.success(patientService.addMeasure(measure));
    }
}