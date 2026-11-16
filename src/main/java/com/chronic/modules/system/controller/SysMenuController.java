package com.chronic.modules.system.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysMenu;
import com.chronic.modules.system.service.SysMenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜单管理
 *
 * @author chronic
 */
@Tag(name = "33-菜单管理", description = "菜单树与路由")
@RestController
@RequestMapping("/system/menu")
@RequiredArgsConstructor
public class SysMenuController {

    private final SysMenuService menuService;

    @Operation(summary = "查询菜单树")
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/tree")
    public Result<List<SysMenu>> tree() {
        return Result.success(menuService.tree());
    }

    @Operation(summary = "查询路由树（前端动态路由）")
    @GetMapping("/route")
    public Result<List<Map<String, Object>>> route() {
        return Result.success(menuService.routeTree());
    }

    @Operation(summary = "新增菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysMenu menu) {
        menuService.addMenu(menu);
        return Result.success();
    }

    @Operation(summary = "修改菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysMenu menu) {
        menuService.updateMenu(menu);
        return Result.success();
    }

    @Operation(summary = "删除菜单")
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public Result<Void> remove(@PathVariable Long menuId) {
        menuService.removeMenu(menuId);
        return Result.success();
    }
}
