package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.PatientScoreWeightDTO;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.query.ScoreQuery;

import java.util.List;
import java.util.Map;

public interface CdScoreService extends IService<CdHealthScore> {

    PageResult<CdHealthScore> queryPage(ScoreQuery query);

    /**
     * 计算并保存患者健康评分（近 7 天窗口）
     */
    Map<String, Object> calcScore(Long patientId);

    /**
     * 患者权重配置：未配置时返回默认值
     */
    Map<String, Object> patientWeight(Long patientId);

    /**
     * 覆盖保存患者权重，要求四项之和为 1
     */
    void savePatientWeight(PatientScoreWeightDTO dto);

    /**
     * 物理删除评分记录
     */
    void deleteScore(Long scoreId);

    /**
     * 物理删除多条评分记录
     */
    int deleteScores(List<Long> scoreIds);

    Map<String, Object> trend(Long patientId);

    PageResult<CdHealthScore> pageAll(ScoreQuery query);
}