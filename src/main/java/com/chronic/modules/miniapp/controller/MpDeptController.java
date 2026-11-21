package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpDeptSelectDTO;
import com.chronic.modules.miniapp.service.MpDeptService;
import com.chronic.modules.miniapp.vo.MpDeptVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 小程序科室
 * <p>
 * 科室是患者可见范围的唯一依据：勾选了某科室后，该科室的医生才能查看患者信息。
 *
 * @author chronic
 */
@Tag(name = "35-小程序-科室", description = "科室选项 / 本人可见科室")
@RestController
@RequiredArgsConstructor
public class MpDeptController {

    private final MpDeptService deptService;

    @Operation(summary = "科室选项（启用中的科室）")
    @GetMapping("/mp/dept/options")
    public Result<List<MpDeptVO>> options() {
        return Result.success(deptService.options());
    }

    @Operation(summary = "本人档案已选科室")
    @GetMapping("/mp/patient/depts")
    public Result<List<Long>> selected(@RequestParam(required = false) Long patientId) {
        return Result.success(deptService.selectedDeptIds(patientId));
    }

    @Operation(summary = "修改本人档案可见科室")
    @PutMapping("/mp/patient/depts")
    public Result<List<Long>> update(@RequestBody MpDeptSelectDTO dto, @RequestParam(required = false) Long patientId) {
        deptService.setSelectedDeptIds(patientId, dto == null ? null : dto.getDeptIds());
        return Result.success("科室已更新", deptService.selectedDeptIds(patientId));
    }
}