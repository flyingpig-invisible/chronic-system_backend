package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.common.utils.ExcelUtils;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.query.MeasureQuery;
import com.chronic.modules.chronic.vo.MeasureGroupVO;
import com.chronic.modules.chronic.service.CdMeasureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

/**
 * 监测数据管理
 *
 * @author chronic
 */
@Tag(name = "03-监测数据", description = "监测记录查询、录入、导出")
@RestController
@RequestMapping("/chronic/measure")
@RequiredArgsConstructor
public class CdMeasureController {

    private final CdMeasureService measureService;

    @Operation(summary = "分页查询监测数据")
    @PreAuthorize("@ss.hasPermi('chronic:measure:list')")
    @GetMapping("/list")
    public Result<PageResult<CdMeasure>> list(MeasureQuery query) {
        return Result.success(measureService.pageMeasure(query));
    }

    @Operation(summary = "按次聚合查询监测数据（每组一个表格）")
    @PreAuthorize("@ss.hasPermi('chronic:measure:list')")
    @GetMapping("/grouped")
    public Result<PageResult<MeasureGroupVO>> grouped(MeasureQuery query) {
        return Result.success(measureService.pageGrouped(query));
    }

    @Operation(summary = "删除一整组监测数据")
    @PreAuthorize("@ss.hasPermi('chronic:measure:remove')")
    @DeleteMapping("/group/delete")
    public Result<Void> removeGroup(@RequestParam Long patientId, @RequestParam String measureTime) {
        measureService.deleteGroup(patientId, measureTime);
        return Result.success();
    }

    @Operation(summary = "获取监测记录详情")
    @PreAuthorize("@ss.hasPermi('chronic:measure:query')")
    @GetMapping("/{measureId:\\d+}")
    public Result<CdMeasure> getInfo(@PathVariable Long measureId) {
        return Result.success(measureService.getMeasure(measureId));
    }

    @Operation(summary = "手动录入监测数据")
    @PreAuthorize("@ss.hasPermi('chronic:measure:add')")
    @PostMapping
    public Result<Long> add(@RequestBody CdMeasure measure) {
        return Result.success("录入成功", measureService.addMeasure(measure));
    }

    @Operation(summary = "批量上报监测数据")
    @PreAuthorize("@ss.hasPermi('chronic:measure:add')")
    @PostMapping("/batch")
    public Result<Void> batch(@RequestBody List<CdMeasure> measures) {
        measureService.batchAdd(measures);
        return Result.success();
    }

    @Operation(summary = "删除监测记录")
    @PreAuthorize("@ss.hasPermi('chronic:measure:remove')")
    @DeleteMapping("/{measureIds:\\d+(,\\d+)*}")
    public Result<Void> remove(@PathVariable Long[] measureIds) {
        measureService.deleteMeasure(measureIds);
        return Result.success();
    }

    @Operation(summary = "导出监测记录")
    @PreAuthorize("@ss.hasPermi('chronic:measure:list')")
    @PostMapping("/export")
    public void export(HttpServletResponse response, MeasureQuery query) {
        List<CdMeasure> list = measureService.listForExport(query);
        List<Object[]> rows = new ArrayList<>();
        for (CdMeasure m : list) {
            rows.add(new Object[]{
                    m.getPatientName(), m.getIndicatorName(), m.getValue(), m.getUnit(),
                    m.getMeasureTime(),
                    "1".equals(m.getAbnormalFlag()) ? "异常" : "正常",
                    "1".equals(m.getDataSource()) ? "设备" : "手动"
            });
        }
        ExcelUtils.export(response, "监测数据", "监测数据",
                new String[]{"患者姓名", "指标名称", "数值", "单位", "监测时间", "状态", "数据来源"}, rows);
    }
}
