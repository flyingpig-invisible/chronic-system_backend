package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdHealthScore;
import com.chronic.modules.chronic.entity.CdScoreWeight;
import com.chronic.modules.chronic.query.ScoreQuery;

import java.util.List;
import java.util.Map;

public interface CdScoreService extends IService<CdHealthScore> {

    PageResult<CdHealthScore> queryPage(ScoreQuery query);

    Map<String, Object> calcScore(Long patientId);

    List<CdScoreWeight> weightList(String diseaseCode);

    void saveWeight(CdScoreWeight weight);

    Map<String, Object> trend(Long patientId);

    PageResult<CdHealthScore> pageAll(ScoreQuery query);
}
