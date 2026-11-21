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

    /**
     * 一次测量录入多个指标：共用同一测量时刻，指标必须来自指标字典
     *
     * @param patientId   患者ID
     * @param diseaseCode 病种编码，可空
     * @param measureTime 测量时刻，空则取当前时间
     * @param dataSource  数据来源：1设备 2手动
     * @param entries     指标与数值
     * @return 写入的监测记录ID
     */
    List<Long> addMeasureOnce(Long patientId, String diseaseCode, java.time.LocalDateTime measureTime,
                              String dataSource, List<com.chronic.modules.chronic.dto.MeasureEntryDTO> entries);

    void deleteMeasure(Long[] measureIds);

    List<CdMeasure> listForExport(MeasureQuery query);

    /** 按「患者 + 测量时刻」聚合，每组即一次测量，未录入的指标补空行 */
    com.chronic.common.result.PageResult<MeasureGroupVO> pageGrouped(MeasureQuery query);

    /** 删除一整组（一次测量的全部指标） */
    void deleteGroup(Long patientId, String measureTime);
}
