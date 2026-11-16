package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.ProfileSaveDTO;
import com.chronic.modules.chronic.service.CdPatientProfileService;
import com.chronic.modules.chronic.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 患者专属档案（小程序家属端）
 *
 * <p>仅家属可见/可编辑生活类字段；患者本人访问返回 403。</p>
 *
 * @author chronic
 */
@Tag(name = "24-小程序专属档案", description = "家属端患者专属档案：查看与补充生活类信息")
@RestController
@RequestMapping("/mp/profile")
@RequiredArgsConstructor
public class MpProfileController {

    private final CdPatientProfileService profileService;

    @Operation(summary = "家属获取患者专属档案")
    @GetMapping("/patient/{patientId}")
    public Result<ProfileVO> getInfo(@PathVariable Long patientId) {
        return Result.success(profileService.getProfileForFamily(patientId));
    }

    @Operation(summary = "家属补充患者专属档案（仅生活类字段生效）")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody ProfileSaveDTO dto) {
        profileService.saveProfile(dto, true);
        return Result.success();
    }
}
