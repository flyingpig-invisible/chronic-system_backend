package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.miniapp.dto.MpClinicDTO;
import com.chronic.modules.miniapp.service.MpContentService;
import com.chronic.modules.miniapp.vo.MpArticleVO;
import com.chronic.modules.miniapp.vo.MpDeviceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 小程序设备 / 科普 / 在线问诊
 *
 * @author chronic
 */
@Tag(name = "34-小程序-设备与内容", description = "设备 / 健康科普 / 在线问诊")
@RestController
@RequestMapping("/mp")
@RequiredArgsConstructor
public class MpContentController {

    private final MpContentService contentService;

    @Operation(summary = "模拟扫描附近蓝牙设备")
    @GetMapping("/device/scan")
    public Result<List<MpDeviceVO>> scan(@RequestParam(required = false) Long patientId) {
        return Result.success(contentService.scanDevices(patientId));
    }

    @Operation(summary = "已绑定设备")
    @GetMapping("/device/list")
    public Result<List<MpDeviceVO>> devices(@RequestParam(required = false) Long patientId) {
        return Result.success(contentService.devices(patientId));
    }

    @Operation(summary = "解绑设备")
    @DeleteMapping("/device/{deviceId}")
    public Result<Void> unbind(@PathVariable Long deviceId, @RequestParam(required = false) Long patientId) {
        contentService.unbindDevice(patientId, deviceId);
        return Result.success("已解绑", null);
    }

    @Operation(summary = "健康科普列表")
    @GetMapping("/article/list")
    public Result<PageResult<MpArticleVO>> articles(@RequestParam(defaultValue = "1") Integer pageNum,
                                                     @RequestParam(defaultValue = "10") Integer pageSize,
                                                     @RequestParam(required = false) String keyword,
                                                     @RequestParam(required = false) String diseaseCode) {
        return Result.success(contentService.articles(pageNum, pageSize, keyword, diseaseCode));
    }

    @Operation(summary = "文章详情")
    @GetMapping("/article/{articleId}")
    public Result<MpArticleVO> article(@PathVariable Long articleId) {
        return Result.success(contentService.article(articleId));
    }

    @Operation(summary = "问诊记录")
    @GetMapping("/clinic/list")
    public Result<List<CdClinic>> clinics(@RequestParam(required = false) Long patientId) {
        return Result.success(contentService.clinics(patientId));
    }

    @Operation(summary = "发起在线问诊")
    @PostMapping("/clinic")
    public Result<Long> book(@Valid @RequestBody MpClinicDTO dto) {
        return Result.success("已提交，等待医生接诊", contentService.bookClinic(dto));
    }

    @Operation(summary = "取消问诊")
    @PutMapping("/clinic/{visitId}/cancel")
    public Result<Void> cancel(@PathVariable Long visitId) {
        contentService.cancelClinic(visitId);
        return Result.success("已取消", null);
    }
}