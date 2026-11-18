package com.chronic.modules.chronic.controller;

import com.chronic.common.result.PageResult;
import com.chronic.common.result.Result;
import com.chronic.modules.chronic.dto.ScoreWeightSaveDTO;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdScoreWeight;
import com.chronic.modules.chronic.query.ScoreQuery;
import com.chronic.modules.chronic.service.CdScoreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 健康评分
 *
 * @author chronic
 */
@Tag(name = "11-健康评分", description = "风险评分计算、趋势与权重配置")
@RestController
@RequestMapping("/chronic/score")
@RequiredArgsConstructor
public class CdScoreController {

    private final CdScoreService scoreService;

    @Operation(summary = "分页查询评分记录")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @GetMapping("/list")
    public Result<PageResult<CdHealthScore>> list(ScoreQuery query) {
        return Result.success(scoreService.queryPage(query));
    }

    @Operation(summary = "计算患者健康评分")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @PostMapping("/calc/{patientId}")
    public Result<Map<String, Object>> calc(@PathVariable Long patientId) {
        return Result.success(scoreService.calcScore(patientId));
    }

    @Operation(summary = "删除评分记录")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @DeleteMapping("/{scoreId}")
    public Result<Void> delete(@PathVariable Long scoreId) {
        scoreService.deleteScore(scoreId);
        return Result.success();
    }

    @Operation(summary = "批量删除评分记录")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @DeleteMapping("/batch")
    public Result<Integer> batchDelete(@RequestBody List<Long> scoreIds) {
        return Result.success(scoreService.deleteScores(scoreIds));
    }

    @Operation(summary = "查询患者评分趋势")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @GetMapping("/trend/{patientId}")
    public Result<Map<String, Object>> trend(@PathVariable Long patientId) {
        return Result.success(scoreService.trend(patientId));
    }

    @Operation(summary = "查询评分权重配置")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @GetMapping("/weight/list")
    public Result<List<CdScoreWeight>> weightList(@RequestParam(required = false) String diseaseCode) {
        return Result.success(scoreService.weightList(diseaseCode));
    }

    @Operation(summary = "修改评分权重（按病种整体覆盖，权重之和须为1）")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @PostMapping("/weight/save")
    public Result<Void> saveWeights(@Valid @RequestBody ScoreWeightSaveDTO dto) {
        scoreService.saveDiseaseWeights(dto);
        return Result.success();
    }

    @Operation(summary = "查询病种综合权重（多病共存时各病种占比，未配置则平分）")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @GetMapping("/weight/combined")
    public Result<List<Map<String, Object>>> combinedWeights(@RequestParam List<String> diseaseCodes) {
        return Result.success(scoreService.diseaseWeightList(diseaseCodes));
    }

    @Operation(summary = "修改病种综合权重（权重之和须为1）")
    @PreAuthorize("@ss.hasPermi('chronic:score:list')")
    @PostMapping("/weight/combined/save")
    public Result<Void> saveCombinedWeights(@RequestBody List<ScoreWeightSaveDTO.DiseaseItem> items) {
        scoreService.saveCombinedDiseaseWeights(items);
        return Result.success();
    }
}