package com.chronic.modules.chronic.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.ProfileSaveDTO;
import com.chronic.modules.chronic.service.CdPatientProfileService;
import com.chronic.modules.chronic.vo.ProfileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 患者专属档案（医生端）
 *
 * @author chronic
 */
@Tag(name = "27-患者专属档案", description = "医生端患者专属档案：结构化字段 + 家属补充 + 填写留痕")
@RestController
@RequestMapping("/chronic/profile")
@RequiredArgsConstructor
public class CdPatientProfileController {

    private final CdPatientProfileService profileService;

    @Operation(summary = "获取患者专属档案")
    @PreAuthorize("@ss.hasPermi('chronic:profile:list')")
    @GetMapping("/patient/{patientId}")
    public Result<ProfileVO> getInfo(@PathVariable Long patientId) {
        return Result.success(profileService.getProfileForDoctor(patientId));
    }

    @Operation(summary = "保存患者专属档案（医生可编辑全部字段）")
    @PreAuthorize("@ss.hasPermi('chronic:profile:edit')")
    @PostMapping
    public Result<Void> save(@Valid @RequestBody ProfileSaveDTO dto) {
        profileService.saveProfile(dto, false);
        return Result.success();
    }
}
