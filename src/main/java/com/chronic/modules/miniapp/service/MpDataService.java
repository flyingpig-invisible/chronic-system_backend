package com.chronic.modules.miniapp.service;

import java.util.List;
import java.util.Map;

public interface MpDataService {

    /** 家属可查看的患者列表（含基础信息） */
    List<Map<String, Object>> myPatientList();

    /** 校验家属是否有权查看该患者 */
    void checkPermission(Long patientId);

    /** 患者概览：基础信息+最新指标+待处理预警数 */
    Map<String, Object> overview(Long patientId);

    /** 患者监测数据 */
    List<Map<String, Object>> measures(Long patientId, String indicatorCode, Integer limit);

    /** 患者预警列表 */
    List<Map<String, Object>> warnings(Long patientId);

    /** 患者健康计划 */
    List<Map<String, Object>> plans(Long patientId);

    /** 患者用药提醒 */
    List<Map<String, Object>> reminds(Long patientId);

    /** 患者远程指导 */
    List<Map<String, Object>> guides(Long patientId);

    /** 患者科普文章（按病种） */
    List<Map<String, Object>> articles(Long patientId);
}
