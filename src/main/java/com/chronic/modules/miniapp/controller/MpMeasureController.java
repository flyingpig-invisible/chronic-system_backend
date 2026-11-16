package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpMeasureDTO;
import com.chronic.modules.miniapp.service.MpMeasureService;
import com.chronic.modules.miniapp.service.impl.MpMeasureServiceImpl;
import com.chronic.modules.miniapp.vo.MpMeasureVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 小程序端健康指标录入
 *
 * @author chronic
 */
@Tag(name = "24-小程序健康指标录入", description = "患者/家属一次性录入一组指标，允许留空")
@RestController
@RequestMapping("/mp/measure")
@RequiredArgsConstructor
public class MpMeasureController {

    private final MpMeasureService measureService;

    @Operation(summary = "可录入的指标选项")
    @GetMapping("/indicators")
    public Result<List<Map<String, String>>> indicators() {
        List<Map<String, String>> list = new ArrayList<>();
        MpMeasureServiceImpl.INDICATORS.forEach((code, meta) -> {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("code", code);
            m.put("name", meta[0]);
            m.put("unit", meta[1]);
            list.add(m);
        });
        return Result.success(list);
    }

    @Operation(summary = "一次性录入一组指标（空值自动跳过）")
    @PostMapping("/submit")
    public Result<MpMeasureVO> submit(@RequestBody MpMeasureDTO dto) {
        return Result.success("录入成功", measureService.submit(dto));
    }
}
