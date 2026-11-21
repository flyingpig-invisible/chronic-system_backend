package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.DeviceImportParam;
import com.chronic.modules.chronic.entity.CdImportLog;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.service.CdDeviceService;
import com.chronic.modules.chronic.service.HealthImportService;
import com.chronic.modules.chronic.vo.DeviceCardVO;
import com.chronic.modules.chronic.vo.ImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 医生端设备管理
 *
 * @author chronic
 */
@Tag(name = "29-设备管理", description = "设备档案、强制解绑、数据导入")
@RestController
@RequestMapping("/device")
@RequiredArgsConstructor
public class CdDeviceController {

    private final CdDeviceService deviceService;
    private final HealthImportService importService;

    @Operation(summary = "分页查询设备")
    @PreAuthorize("@ss.hasPermi('chronic:device:list')")
    @GetMapping("/page")
    public Result<PageResult<DeviceCardVO>> page(@RequestParam(required = false) String keyword,
                                                 @RequestParam(required = false) Long patientId,
                                                 @RequestParam(defaultValue = "1") Integer pageNum,
                                                 @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(deviceService.page(pageNum, pageSize, keyword, patientId));
    }

    @Operation(summary = "某台设备的监测数据")
    @PreAuthorize("@ss.hasPermi('chronic:device:query')")
    @GetMapping("/{deviceId}/measures")
    public Result<List<CdMeasure>> measures(@PathVariable Long deviceId,
                                            @RequestParam(defaultValue = "50") Integer limit) {
        return Result.success(deviceService.deviceMeasures(deviceId, limit));
    }

    @Operation(summary = "强制解绑（设备绑在错误患者下时使用）")
    @PreAuthorize("@ss.hasPermi('chronic:device:edit')")
    @PostMapping("/{deviceId}/force-unbind")
    public Result<Void> forceUnbind(@PathVariable Long deviceId) {
        deviceService.unbind(deviceId);
        return Result.success();
    }

    @Operation(summary = "数据导入")
    @PreAuthorize("@ss.hasPermi('chronic:device:import')")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResultVO> importData(@RequestPart("file") MultipartFile file,
                                            @ParameterObject @ModelAttribute DeviceImportParam param) {
        return Result.success("导入完成", importService.importData(file, param));
    }

    @Operation(summary = "导入历史")
    @PreAuthorize("@ss.hasPermi('chronic:device:list')")
    @GetMapping("/import/logs")
    public Result<PageResult<CdImportLog>> importLogs(@RequestParam(required = false) Long patientId,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(importService.pageLogs(patientId, pageNum, pageSize));
    }
}
