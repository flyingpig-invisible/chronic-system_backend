package com.chronic.modules.miniapp.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.dto.MpResetDTO;
import com.chronic.modules.miniapp.service.MpAuthService;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import com.chronic.modules.miniapp.vo.MpUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 小程序认证
 *
 * @author chronic
 */
@Tag(name = "30-小程序-认证", description = "登录 / 注册 / 重置密码 / 个人信息")
@RestController
@RequestMapping("/mp/auth")
@RequiredArgsConstructor
public class MpAuthController {

    private final MpAuthService authService;

    @Operation(summary = "登录（手机号+密码）")
    @PostMapping("/login")
    public Result<MpLoginVO> login(@RequestBody MpLoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @Operation(summary = "注册（手机号必填，可自动关联医生端已有档案）")
    @PostMapping("/register")
    public Result<MpLoginVO> register(@Valid @RequestBody MpRegisterDTO dto) {
        return Result.success("注册成功", authService.register(dto));
    }

    @Operation(summary = "忘记密码：校验手机号后重置")
    @PostMapping("/reset-password")
    public Result<Void> reset(@Valid @RequestBody MpResetDTO dto) {
        authService.resetPassword(dto);
        return Result.success("密码已重置", null);
    }

    @Operation(summary = "当前登录用户")
    @GetMapping("/info")
    public Result<MpUserVO> info() {
        return Result.success(authService.currentUser());
    }

    @Operation(summary = "修改个人资料")
    @PutMapping("/profile")
    public Result<MpUserVO> updateProfile(@RequestParam(required = false) String realName,
                                           @RequestParam(required = false) String avatar,
                                           @RequestParam(required = false) String gender) {
        return Result.success(authService.updateProfile(realName, avatar, gender));
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}