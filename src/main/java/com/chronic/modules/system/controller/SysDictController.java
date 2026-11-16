package com.chronic.modules.system.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.system.entity.SysDictData;
import com.chronic.modules.system.entity.SysDictType;
import com.chronic.modules.system.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 字典管理
 *
 * @author chronic
 */
@Tag(name = "34-字典管理", description = "数据字典类型与数据")
@RestController
@RequestMapping("/system/dict")
@RequiredArgsConstructor
public class SysDictController {

    private final SysDictService dictService;

    @Operation(summary = "分页查询字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:list')")
    @GetMapping("/type/list")
    public Result<PageResult<SysDictType>> typeList(@RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "10") Integer pageSize,
                                                    @RequestParam(required = false) String dictName) {
        return Result.success(dictService.typePage(pageNum, pageSize, dictName));
    }

    @Operation(summary = "查询全部字典类型（下拉）")
    @GetMapping("/type/option")
    public Result<List<SysDictType>> typeOption() {
        return Result.success(dictService.typeList());
    }

    @Operation(summary = "按类型查询字典数据")
    @GetMapping("/data/type/{dictType}")
    public Result<List<SysDictData>> dataByType(@PathVariable String dictType) {
        return Result.success(dictService.dataByType(dictType));
    }

    @Operation(summary = "新增字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping("/type")
    public Result<Void> addType(@RequestBody SysDictType type) {
        dictService.addType(type);
        return Result.success();
    }

    @Operation(summary = "修改字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping("/type")
    public Result<Void> editType(@RequestBody SysDictType type) {
        dictService.updateType(type);
        return Result.success();
    }

    @Operation(summary = "删除字典类型")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/type/{dictId}")
    public Result<Void> removeType(@PathVariable Long dictId) {
        dictService.removeType(dictId);
        return Result.success();
    }

    @Operation(summary = "新增字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:add')")
    @PostMapping("/data")
    public Result<Void> addData(@RequestBody SysDictData data) {
        dictService.addData(data);
        return Result.success();
    }

    @Operation(summary = "修改字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:edit')")
    @PutMapping("/data")
    public Result<Void> editData(@RequestBody SysDictData data) {
        dictService.updateData(data);
        return Result.success();
    }

    @Operation(summary = "删除字典数据")
    @PreAuthorize("@ss.hasPermi('system:dict:remove')")
    @DeleteMapping("/data/{dictCode}")
    public Result<Void> removeData(@PathVariable Long dictCode) {
        dictService.removeData(dictCode);
        return Result.success();
    }
}
