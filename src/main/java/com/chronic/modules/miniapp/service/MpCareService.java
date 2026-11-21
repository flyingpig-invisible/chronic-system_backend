package com.chronic.modules.miniapp.service;

import com.chronic.modules.miniapp.vo.MpGuideVO;
import com.chronic.modules.miniapp.vo.MpMedicationVO;
import com.chronic.modules.miniapp.vo.MpPlanVO;
import com.chronic.modules.miniapp.vo.MpWarningVO;

import java.util.List;

/**
 * 预警 / 指导 / 计划 / 用药
 *
 * @author chronic
 */
public interface MpCareService {

    List<MpWarningVO> warnings(Long patientId, String level);

    MpWarningVO warningDetail(Long warningId);

    List<MpGuideVO> guides(Long patientId);

    void readGuide(Long guideId);

    List<MpPlanVO> plans(Long patientId);

    MpPlanVO planDetail(Long planId);

    /** 今日用药清单 */
    List<MpMedicationVO> todayMedication(Long patientId);

    /** 用药打卡 */
    void checkMedication(Long patientId, Long remindId, String planTime);

    /** 某天用药记录（默认今天） */
    List<MpMedicationVO> medicationOfDay(Long patientId, String date);
}