package com.chronic.modules.chronic.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.query.MeasureQuery;
import com.chronic.modules.chronic.vo.MeasureGroupVO;

import java.util.List;

/**
 * 监测数据服务
 *
 * @author chronic
 */
public interface CdMeasureService {

    PageResult<CdMeasure> pageMeasure(MeasureQuery query);

    CdMeasure getMeasure(Long measureId);

    Long addMeasure(CdMeasure measure);

    void batchAdd(List<CdMeasure> measures);

    void deleteMeasure(Long[] measureIds);

    List<CdMeasure> listForExport(MeasureQuery query);

    /** 按「患者 + 测量时刻」聚合，每组即一次测量，未录入的指标补空行 */
    com.chronic.common.result.PageResult<MeasureGroupVO> pageGrouped(MeasureQuery query);

    /** 删除一整组（一次测量的全部指标） */
    void deleteGroup(Long patientId, String measureTime);
}
