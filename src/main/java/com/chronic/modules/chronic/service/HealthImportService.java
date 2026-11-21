package com.chronic.modules.chronic.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.DeviceImportParam;
import com.chronic.modules.chronic.entity.CdImportLog;
import com.chronic.modules.chronic.support.huawei.ImportIssue;
import com.chronic.modules.chronic.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 华为运动健康导出包导入服务
 *
 * @author chronic
 */
public interface HealthImportService {

    /**
     * 导入导出包（ZIP 或单个 JSON）
     * <p>
     * dryRun=true 时只解析不入库，返回预览结果
     */
    ImportResultVO importData(MultipartFile file, DeviceImportParam param);

    /** 导入历史 */
    PageResult<CdImportLog> pageLogs(Long patientId, Integer pageNum, Integer pageSize);

    /** 某次导入的失败明细 */
    List<ImportIssue> issues(Long importId);
}
