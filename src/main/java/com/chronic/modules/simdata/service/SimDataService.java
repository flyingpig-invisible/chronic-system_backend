package com.chronic.modules.simdata.service;

import com.chronic.modules.simdata.dto.SimImportDTO;
import com.chronic.modules.simdata.vo.SimDeviceVO;
import com.chronic.modules.simdata.vo.SimImportResultVO;

/**
 * 设备模拟数据导入
 * <p>
 * 蓝牙设备封闭、无法真实连接，这里按整点生成仿真采集数据入库，
 * 数据进入与手动录入完全相同的监测/预警/评分链路。
 *
 * @author chronic
 */
public interface SimDataService {

    /** 某台已绑定设备支持导入的指标 */
    SimDeviceVO deviceOptions(Long patientId, Long deviceId);

    /** 按选定的指标与时间范围生成并导入模拟数据 */
    SimImportResultVO importData(SimImportDTO dto);
}