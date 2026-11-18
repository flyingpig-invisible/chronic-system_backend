package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.query.PatientQuery;
import com.chronic.modules.chronic.service.CdPatientService;
import com.chronic.modules.chronic.vo.DashboardVO;
import com.chronic.modules.chronic.vo.PatientVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 患者管理
 *
 * @author chronic
 */
@Tag(name = "02-患者管理", description = "患者列表、档案、看板")
@RestController
@RequestMapping("/chronic/patient")
@RequiredArgsConstructor
public class CdPatientController {

    private final CdPatientService patientService;

    @Operation(summary = "分页查询患者列表")
    @PreAuthorize("@ss.hasPermi('chronic:patient:list')")
    @GetMapping("/list")
    public Result<PageResult<PatientVO>> list(PatientQuery query) {
        return Result.success(patientService.pagePatient(query));
    }

    @Operation(summary = "全局看板统计")
    @PreAuthorize("@ss.hasPermi('chronic:patient:list')")
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview() {
        return Result.success(patientService.overview());
    }

    @Operation(summary = "患者健康档案汇总")
    @PreAuthorize("@ss.hasPermi('chronic:patient:query')")
    @GetMapping("/{patientId}/profile")
    public Result<Map<String, Object>> profile(@PathVariable Long patientId) {
        return Result.success(patientService.getProfile(patientId));
    }

    @Operation(summary = "获取患者详情")
    @PreAuthorize("@ss.hasPermi('chronic:patient:query')")
    @GetMapping("/{patientId}")
    public Result<PatientVO> getInfo(@PathVariable Long patientId) {
        return Result.success(patientService.getPatientDetail(patientId));
    }

    @Operation(summary = "新增患者档案")
    @PreAuthorize("@ss.hasPermi('chronic:patient:add')")
    @PostMapping
    public Result<Long> add(@RequestBody CdPatient patient) {
        return Result.success("新增成功", patientService.addPatient(patient));
    }

    @Operation(summary = "修改患者档案")
    @PreAuthorize("@ss.hasPermi('chronic:patient:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody CdPatient patient) {
        patientService.updatePatient(patient);
        return Result.success();
    }

    @Operation(summary = "删除患者档案")
    @PreAuthorize("@ss.hasPermi('chronic:patient:remove')")
    @DeleteMapping("/{patientIds}")
    public Result<Void> remove(@PathVariable Long[] patientIds) {
        patientService.deletePatient(patientIds);
        return Result.success();
    }

    @Operation(summary = "患者看板概览")
    @PreAuthorize("@ss.hasPermi('chronic:patient:query')")
    @GetMapping("/dashboard/{patientId}")
    public Result<DashboardVO> dashboard(@PathVariable Long patientId) {
        return Result.success(patientService.getDashboard(patientId));
    }

    @Operation(summary = "患者监测趋势")
    @PreAuthorize("@ss.hasPermi('chronic:patient:query')")
    @GetMapping("/{patientId}/trend")
    public Result<List<Map<String, Object>>> trend(@PathVariable Long patientId,
                                                   @RequestParam(required = false) String indicatorCode,
                                                   @RequestParam(required = false) String beginTime,
                                                   @RequestParam(required = false) String endTime) {
        return Result.success(patientService.getMeasureTrend(patientId, indicatorCode, beginTime, endTime));
    }

    @Operation(summary = "病种下拉选项")
    @GetMapping("/diseaseOptions")
    public Result<List<Map<String, String>>> diseaseOptions() {
        return Result.success(patientService.diseaseOptions());
    }

    @Operation(summary = "导出患者列表")
    @PreAuthorize("@ss.hasPermi('chronic:patient:export')")
    @PostMapping("/export")
    public void export(PatientQuery query, jakarta.servlet.http.HttpServletResponse response) {
        List<PatientVO> rows = patientService.listForExport(query);
        List<Object[]> data = new java.util.ArrayList<>();
        for (PatientVO v : rows) {
            data.add(new Object[]{v.getPatientNo(), v.getName(), genderName(v.getGender()), v.getPhone(),
                    v.getRiskLevel(), v.getRiskScore(), v.getDoctorName()});
        }
        com.chronic.common.utils.ExcelUtils.export(response, "患者列表", "患者列表",
                new String[]{"患者编号", "姓名", "性别", "手机号", "风险等级", "风险评分", "责任医生"}, data);
    }

    /** 性别：1男 0女 2未知 */
    private String genderName(String gender) {
        if ("1".equals(gender)) {
            return "男";
        }
        if ("0".equals(gender)) {
            return "女";
        }
        return "未知";
    }
}
