package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.service.MpAuthService;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 小程序认证
 *
 * @author chronic
 */
@Tag(name = "20-小程序认证", description = "家属注册、登录、获取当前用户")
@RestController
@RequestMapping("/mp/auth")
@RequiredArgsConstructor
public class MpAuthController {

    private final MpAuthService authService;

    @Operation(summary = "家属注册（新用户默认家属角色）")
    @PostMapping("/register")
    public Result<MpLoginVO> register(@Valid @RequestBody MpRegisterDTO dto) {
        return Result.success(authService.register(dto));
    }

    @Operation(summary = "小程序登录")
    @PostMapping("/login")
    public Result<MpLoginVO> login(@Valid @RequestBody MpLoginDTO dto) {
        return Result.success(authService.login(dto));
    }

    @Operation(summary = "获取当前登录用户")
    @GetMapping("/info")
    public Result<MpUser> info() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}
