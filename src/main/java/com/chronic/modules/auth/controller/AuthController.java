package com.chronic.modules.auth.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.auth.dto.LoginDTO;
import com.chronic.modules.auth.service.AuthService;
import com.chronic.modules.auth.vo.LoginVO;
import com.chronic.modules.auth.vo.UserInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证接口（医生端 Web）
 *
 * @author chronic
 */
@Tag(name = "01-认证管理", description = "登录、登出、用户信息")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success("登录成功", authService.login(dto));
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(authService.getUserInfo());
    }

    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }
}
