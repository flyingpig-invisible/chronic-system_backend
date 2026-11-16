package com.chronic.modules.chronic.controller;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.mapper.CdWarningRuleMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 预警规则管理
 *
 * @author chronic
 */
@Tag(name = "05-预警规则", description = "预警规则的增删改查")
@RestController
@RequestMapping("/chronic/rule")
@RequiredArgsConstructor
public class CdWarningRuleController {

    private final CdWarningRuleMapper ruleMapper;

    @Operation(summary = "分页查询预警规则")
    @PreAuthorize("@ss.hasPermi('chronic:rule:list')")
    @GetMapping("/list")
    public Result<PageResult<CdWarningRule>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                  @RequestParam(defaultValue = "10") Integer pageSize,
                                                  CdWarningRule query) {
        Page<CdWarningRule> page = ruleMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<CdWarningRule>lambdaQuery()
                        .like(StringUtils.hasText(query.getRuleName()), CdWarningRule::getRuleName, query.getRuleName())
                        .eq(StringUtils.hasText(query.getDiseaseCode()), CdWarningRule::getDiseaseCode, query.getDiseaseCode())
                        .eq(StringUtils.hasText(query.getWarningLevel()), CdWarningRule::getWarningLevel, query.getWarningLevel())
                        .eq(StringUtils.hasText(query.getStatus()), CdWarningRule::getStatus, query.getStatus())
                        .orderByAsc(CdWarningRule::getDiseaseCode)
                        .orderByAsc(CdWarningRule::getRuleId));
        return Result.success(PageResult.of(page));
    }

    @Operation(summary = "查询全部启用规则")
    @GetMapping("/all")
    public Result<List<CdWarningRule>> all() {
        return Result.success(ruleMapper.selectList(Wrappers.<CdWarningRule>lambdaQuery()
                .eq(CdWarningRule::getStatus, "0")));
    }

    @Operation(summary = "获取规则详情")
    @PreAuthorize("@ss.hasPermi('chronic:rule:list')")
    @GetMapping("/{ruleId}")
    public Result<CdWarningRule> getInfo(@PathVariable Long ruleId) {
        return Result.success(ruleMapper.selectById(ruleId));
    }

    @Operation(summary = "新增预警规则")
    @PreAuthorize("@ss.hasPermi('chronic:rule:edit')")
    @PostMapping
    public Result<Long> add(@RequestBody CdWarningRule rule) {
        if (!StringUtils.hasText(rule.getStatus())) {
            rule.setStatus("0");
        }
        ruleMapper.insert(rule);
        return Result.success("新增成功", rule.getRuleId());
    }

    @Operation(summary = "修改预警规则")
    @PreAuthorize("@ss.hasPermi('chronic:rule:edit')")
    @PutMapping
    public Result<Void> edit(@RequestBody CdWarningRule rule) {
        ruleMapper.updateById(rule);
        return Result.success();
    }

    @Operation(summary = "删除预警规则")
    @PreAuthorize("@ss.hasPermi('chronic:rule:edit')")
    @DeleteMapping("/{ruleIds}")
    public Result<Void> remove(@PathVariable Long[] ruleIds) {
        ruleMapper.deleteByIds(Arrays.asList(ruleIds));
        return Result.success();
    }

    @Operation(summary = "启用/停用预警规则")
    @PreAuthorize("@ss.hasPermi('chronic:rule:edit')")
    @PutMapping("/{ruleId}/status")
    public Result<Void> changeStatus(@PathVariable Long ruleId, @RequestParam String status) {
        CdWarningRule rule = new CdWarningRule();
        rule.setRuleId(ruleId);
        rule.setStatus(status);
        ruleMapper.updateById(rule);
        return Result.success();
    }
}
