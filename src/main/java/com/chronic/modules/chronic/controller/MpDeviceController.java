package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.DeviceBindDTO;
import com.chronic.modules.chronic.dto.DeviceImportParam;
import com.chronic.modules.chronic.entity.CdImportLog;
import com.chronic.modules.chronic.service.CdDeviceService;
import com.chronic.modules.chronic.service.HealthImportService;
import com.chronic.modules.chronic.support.huawei.ImportIssue;
import com.chronic.modules.chronic.vo.DeviceCardVO;
import com.chronic.modules.chronic.vo.ImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 小程序端设备绑定与数据导入
 *
 * @author chronic
 */
@Tag(name = "28-小程序设备", description = "设备扫描、绑定、解绑、华为运动健康数据导入")
@RestController
@RequestMapping("/mp/device")
@RequiredArgsConstructor
public class MpDeviceController {

    private final CdDeviceService deviceService;
    private final HealthImportService importService;

    @Operation(summary = "模拟扫描附近可绑定设备")
    @GetMapping("/scan")
    public Result<List<DeviceCardVO>> scan(@RequestParam Long patientId) {
        return Result.success(deviceService.scan(patientId));
    }

    @Operation(summary = "已绑定设备列表")
    @GetMapping("/list")
    public Result<List<DeviceCardVO>> list(@RequestParam Long patientId) {
        return Result.success(deviceService.listByPatient(patientId));
    }

    @Operation(summary = "绑定设备（按蓝牙名，未建档自动建档）")
    @PostMapping("/bind")
    public Result<DeviceCardVO> bind(@RequestBody @Valid DeviceBindDTO dto) {
        return Result.success("绑定成功", deviceService.bind(dto));
    }

    @Operation(summary = "解绑设备")
    @PostMapping("/unbind")
    public Result<Void> unbind(@RequestParam Long deviceId) {
        deviceService.unbind(deviceId);
        return Result.success();
    }

    @Operation(summary = "修改设备别名")
    @PutMapping("/{deviceId}/alias")
    public Result<Void> updateAlias(@PathVariable Long deviceId, @RequestParam String alias) {
        deviceService.updateAlias(deviceId, alias);
        return Result.success();
    }

    @Operation(summary = "数据导入")
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<ImportResultVO> importData(@RequestPart("file") MultipartFile file,
                                            @ParameterObject @ModelAttribute DeviceImportParam param) {
        return Result.success("导入完成", importService.importData(file, param));
    }

    @Operation(summary = "导入历史")
    @GetMapping("/import/logs")
    public Result<PageResult<CdImportLog>> importLogs(@RequestParam(required = false) Long patientId,
                                                      @RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize) {
        return Result.success(importService.pageLogs(patientId, pageNum, pageSize));
    }

    @Operation(summary = "某次导入的失败明细")
    @GetMapping("/import/{importId}/issues")
    public Result<List<ImportIssue>> issues(@PathVariable Long importId) {
        return Result.success(importService.issues(importId));
    }
}
