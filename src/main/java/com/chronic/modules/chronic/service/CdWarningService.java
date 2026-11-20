package com.chronic.modules.chronic.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.EmergencyLinkDTO;
import com.chronic.modules.chronic.dto.WarningHandleDTO;
import com.chronic.modules.chronic.entity.CdEmergencyLink;
import com.chronic.modules.chronic.entity.CdWarningRule;
import com.chronic.modules.chronic.entity.CdWarning;
import com.chronic.modules.chronic.query.WarningQuery;

import java.util.List;
import java.util.Map;

/**
 * 预警服务
 *
 * @author chronic
 */
public interface CdWarningService {

    PageResult<CdWarning> pageWarning(WarningQuery query);

    CdWarning getWarning(Long warningId);

    void handleWarning(Long warningId, WarningHandleDTO dto);

    void linkEmergency(Long warningId, EmergencyLinkDTO dto);

    List<CdEmergencyLink> listEmergencyByWarning(Long warningId);

    PageResult<CdEmergencyLink> pageEmergency(WarningQuery query);

    List<Map<String, Object>> statByLevel(WarningQuery query);

    Map<String, Object> statResponse(WarningQuery query);

    List<CdWarning> listForExport(WarningQuery query);

    /** 根据监测数据判定是否触发预警 */
    void checkAndCreateWarning(Long patientId, String patientName, String indicatorCode,
                               java.math.BigDecimal value, String unit, Long measureId);

    /** 按指标编码与数值匹配最严重的启用规则，未命中返回 null */
    CdWarningRule matchRuleCode(String indicatorCode, java.math.BigDecimal value);
}
