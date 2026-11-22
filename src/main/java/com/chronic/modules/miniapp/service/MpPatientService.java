package com.chronic.modules.miniapp.service;

import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.miniapp.vo.MpDashboardVO;
import com.chronic.modules.miniapp.vo.MpDiseaseVO;
import com.chronic.modules.miniapp.vo.MpHighRiskItemVO;
import com.chronic.modules.miniapp.vo.MpIndicatorVO;
import com.chronic.modules.miniapp.vo.MpPatientVO;
import com.chronic.modules.miniapp.vo.MpTrendVO;

import java.util.List;

/**
 * 小程序患者档案服务
 *
 * @author chronic
 */
public interface MpPatientService {

    /** 当前账号可查看的患者列表（患者本人 / 已绑定亲属） */
    List<MpPatientVO> listPatients();

    MpPatientVO patientDetail(Long patientId);

    List<MpDiseaseVO> diseases(Long patientId);

    List<MpIndicatorVO> latestIndicators(Long patientId);

    /**
     * 高危指标提醒：各指标「最近一次」测量命中高危规则时返回，用于打开小程序后弹窗告警。
     * 与总体健康评分无关，只看单个指标。
     */
    List<MpHighRiskItemVO> highRiskItems(Long patientId);

    MpDashboardVO dashboard(Long patientId);

    /** 趋势数据：days 为天数，0 表示全部 */
    List<MpTrendVO> trend(Long patientId, Integer days);

    /** 录入用的指标字典（仅启用指标） */
    List<com.chronic.modules.system.entity.SysIndicator> indicatorOptions();

    /** 一次录入多个指标：共用同一测量时刻，指标必须来自指标字典 */
    List<Long> addMeasures(com.chronic.modules.chronic.dto.MeasureOnceDTO dto);
    /** 手写测量数据，写入医生端同一张 cd_measure 表并参与预警评估 */
    Long addMeasure(CdMeasure measure);
}
