package com.chronic.modules.system.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysRole;
import com.chronic.modules.system.service.SysRoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 角色管理
 *
 * @author chronic
 */
@Tag(name = "31-角色管理", description = "角色与菜单权限分配")
@RestController
@RequestMapping("/system/role")
@RequiredArgsConstructor
public class SysRoleController {

    private final SysRoleService roleService;

    @Operation(summary = "分页查询角色列表")
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public Result<PageResult<SysRole>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String roleName) {
        return Result.success(roleService.queryPage(pageNum, pageSize, roleName));
    }

    @Operation(summary = "获取角色详情")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{roleId}")
    public Result<SysRole> getInfo(@PathVariable Long roleId) {
        return Result.success(roleService.detail(roleId));
    }

    @Operation(summary = "查询角色已分配的菜单ID")
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{roleId}/menuIds")
    public Result<List<Long>> menuIds(@PathVariable Long roleId) {
        return Result.success(roleService.menuIds(roleId));
    }

    @Operation(summary = "新增角色")
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysRole role) {
        roleService.addRole(role);
        return Result.success();
    }

    @Operation(summary = "修改角色")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysRole role) {
        roleService.updateRole(role);
        return Result.success();
    }

    @Operation(summary = "删除角色")
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleId}")
    public Result<Void> remove(@PathVariable Long roleId) {
        roleService.removeRole(roleId);
        return Result.success();
    }

    @Operation(summary = "分配菜单权限")
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/{roleId}/menu")
    public Result<Void> setMenu(@PathVariable Long roleId, @RequestBody Map<String, List<Long>> body) {
        roleService.setMenuIds(roleId, body.get("menuIds"));
        return Result.success();
    }
}
