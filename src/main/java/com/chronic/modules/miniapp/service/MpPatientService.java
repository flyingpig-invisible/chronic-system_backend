package com.chronic.modules.miniapp.service;

import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.miniapp.vo.MpDashboardVO;
import com.chronic.modules.miniapp.vo.MpDiseaseVO;
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

    MpDashboardVO dashboard(Long patientId);

    /** 趋势数据：days 为天数，0 表示全部 */
    List<MpTrendVO> trend(Long patientId, Integer days);

    /** 手写测量数据，写入医生端同一张 cd_measure 表并参与预警评估 */
    Long addMeasure(CdMeasure measure);
}