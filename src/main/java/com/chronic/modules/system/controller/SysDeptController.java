package com.chronic.modules.system.controller;

import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.service.SysDeptService;
import com.chronic.modules.system.service.SysUserService;
import com.chronic.modules.system.entity.SysUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 科室管理
 *
 * @author chronic
 */
@Tag(name = "32-科室管理", description = "科室树维护")
@RestController
@RequestMapping("/system/dept")
@RequiredArgsConstructor
public class SysDeptController {

    private final SysDeptService deptService;
    private final SysUserService userService;

    @Operation(summary = "查询科室树")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/tree")
    public Result<List<SysDept>> tree() {
        return Result.success(deptService.tree());
    }

    @Operation(summary = "获取科室详情")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/{deptId}")
    public Result<SysDept> getInfo(@PathVariable Long deptId) {
        return Result.success(deptService.getById(deptId));
    }

    @Operation(summary = "新增科室")
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public Result<Void> add(@RequestBody SysDept dept) {
        deptService.addDept(dept);
        return Result.success();
    }

    @Operation(summary = "修改科室")
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody SysDept dept) {
        deptService.updateDept(dept);
        return Result.success();
    }

    @Operation(summary = "删除科室")
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public Result<Void> remove(@PathVariable Long deptId) {
        deptService.removeDept(deptId);
        return Result.success();
    }

    @Operation(summary = "查询科室下医生")
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/{deptId}/doctors")
    public Result<List<SysUser>> doctors(@PathVariable Long deptId) {
        return Result.success(userService.doctorsOfDept(deptId));
    }
}
