package com.chronic.modules.system.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.query.UserQuery;
import com.chronic.modules.system.service.SysUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 系统用户管理
 *
 * @author chronic
 */
@Tag(name = "30-用户管理", description = "系统用户（医生/管理员）管理")
@RestController
@RequestMapping("/system/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService userService;

    @Operation(summary = "分页查询用户列表")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/list")
    public Result<PageResult<SysUser>> list(UserQuery query) {
        return Result.success(userService.queryPage(query));
    }

    @Operation(summary = "获取用户详情")
    @PreAuthorize("@ss.hasPermi('system:user:query')")
    @GetMapping("/{userId}")
    public Result<SysUser> getInfo(@PathVariable Long userId) {
        return Result.success(userService.detail(userId));
    }

    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermi('system:user:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysUser user) {
        userService.addUser(user);
        return Result.success();
    }

    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysUser user) {
        userService.updateUser(user);
        return Result.success();
    }

    @Operation(summary = "删除用户")
    @PreAuthorize("@ss.hasPermi('system:user:remove')")
    @DeleteMapping("/{userId}")
    public Result<Void> remove(@PathVariable Long userId) {
        userService.removeUser(userId);
        return Result.success();
    }

    @Operation(summary = "重置密码")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/{userId}/resetPwd")
    public Result<Void> resetPwd(@PathVariable Long userId, @RequestBody Map<String, String> body) {
        userService.resetPassword(userId, body.get("password"));
        return Result.success();
    }

    @Operation(summary = "启用/停用用户")
    @PreAuthorize("@ss.hasPermi('system:user:edit')")
    @PutMapping("/{userId}/status")
    public Result<Void> status(@PathVariable Long userId, @RequestParam String status) {
        userService.changeStatus(userId, status);
        return Result.success();
    }

    @Operation(summary = "查询医生列表（下拉）")
    @PreAuthorize("@ss.hasPermi('system:user:list')")
    @GetMapping("/doctor/list")
    public Result<List<SysUser>> doctorList() {
        return Result.success(userService.doctorList());
    }
}
