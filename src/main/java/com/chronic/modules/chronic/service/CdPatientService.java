package com.chronic.modules.chronic.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.query.PatientQuery;
import com.chronic.modules.chronic.vo.DashboardVO;
import com.chronic.modules.chronic.vo.PatientVO;

import java.util.List;

/**
 * 患者服务
 *
 * @author chronic
 */
public interface CdPatientService {

    /** 分页查询患者队列 */
    PageResult<PatientVO> pagePatient(PatientQuery query);

    /** 查询患者详情 */
    PatientVO getPatientDetail(Long patientId);

    /**
     * 查询患者详情，不做医生数据范围校验。
     * <p>仅供小程序端使用：调用方（{@code MpAccessService}）已按小程序归属规则校验过访问权。</p>
     */
    PatientVO getPatientDetailUnscoped(Long patientId);

    /** 新增患者 */
    Long addPatient(CdPatient patient);

    /** 修改患者 */
    void updatePatient(CdPatient patient);

    /** 删除患者 */
    void deletePatient(Long[] patientIds);

    /** 患者看板概览 */
    DashboardVO getDashboard(Long patientId);

    /** 查询患者监测趋势 */
    List<java.util.Map<String, Object>> getMeasureTrend(Long patientId, String indicatorCode,
                                                       String beginTime, String endTime);

    /** 病种下拉选项 */
    List<java.util.Map<String, String>> diseaseOptions();

    /** 导出数据 */
    /** 全局看板统计 */
    java.util.Map<String, Object> overview();

    /** 患者健康档案汇总 */
    java.util.Map<String, Object> getProfile(Long patientId);

    List<PatientVO> listForExport(PatientQuery query);
}
