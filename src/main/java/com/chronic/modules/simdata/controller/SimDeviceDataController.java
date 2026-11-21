package com.chronic.modules.simdata.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.simdata.dto.SimImportDTO;
import com.chronic.modules.simdata.service.SimDataService;
import com.chronic.modules.simdata.vo.SimDeviceVO;
import com.chronic.modules.simdata.vo.SimImportResultVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序：模拟导入设备数据
 * <p>
 * 真实蓝牙设备封闭无法连接，按选定指标与时间范围生成整点仿真数据。
 *
 * @author chronic
 */
@Tag(name = "35-小程序-模拟设备数据", description = "设备可导入指标 / 按时间范围模拟导入")
@RestController
@RequestMapping("/mp/device")
@RequiredArgsConstructor
public class SimDeviceDataController {

    private final SimDataService simDataService;

    @Operation(summary = "设备可导入的指标")
    @GetMapping("/{deviceId}/indicators")
    public Result<SimDeviceVO> indicators(@PathVariable Long deviceId,
                                          @RequestParam(required = false) Long patientId) {
        return Result.success(simDataService.deviceOptions(patientId, deviceId));
    }

    @Operation(summary = "按时间范围模拟导入设备数据")
    @PostMapping("/import")
    public Result<SimImportResultVO> importData(@Valid @RequestBody SimImportDTO dto) {
        SimImportResultVO result = simDataService.importData(dto);
        String msg = result.getSuccessRows() == null || result.getSuccessRows() == 0
                ? "所选时间段已有数据，未新增记录"
                : "导入成功，新增 " + result.getSuccessRows() + " 条";
        return Result.success(msg, result);
    }
}