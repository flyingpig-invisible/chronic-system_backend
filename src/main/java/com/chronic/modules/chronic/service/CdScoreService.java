package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ScoreWeightSaveDTO;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdScoreWeight;
import com.chronic.modules.chronic.query.ScoreQuery;

import java.util.List;
import java.util.Map;

public interface CdScoreService extends IService<CdHealthScore> {

    PageResult<CdHealthScore> queryPage(ScoreQuery query);

    Map<String, Object> calcScore(Long patientId);

    List<CdScoreWeight> weightList(String diseaseCode);

    /**
     * 覆盖保存某个病种的全部指标权重，要求权重之和为 1
     */
    void saveDiseaseWeights(ScoreWeightSaveDTO dto);

    /**
     * 物理删除评分记录
     */
    void deleteScore(Long scoreId);

    Map<String, Object> trend(Long patientId);

    PageResult<CdHealthScore> pageAll(ScoreQuery query);
}
