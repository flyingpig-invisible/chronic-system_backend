package com.chronic.modules.chronic.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.DeviceBindDTO;
import com.chronic.modules.chronic.entity.CdMeasure;
import com.chronic.modules.chronic.vo.DeviceCardVO;

import java.util.List;

/**
 * 设备绑定服务
 *
 * @author chronic
 */
public interface CdDeviceService {

    /** 模拟扫描附近可绑定设备 */
    List<DeviceCardVO> scan(Long patientId);

    /** 某患者已绑定的设备 */
    List<DeviceCardVO> listByPatient(Long patientId);

    /** 绑定设备（蓝牙名未建档时自动建档） */
    DeviceCardVO bind(DeviceBindDTO dto);

    /** 解绑 */
    void unbind(Long deviceId);

    /** 修改别名 */
    void updateAlias(Long deviceId, String alias);

    /** 医生端分页 */
    PageResult<DeviceCardVO> page(Integer pageNum, Integer pageSize, String keyword, Long patientId);

    /** 某台设备上传的监测数据 */
    List<CdMeasure> deviceMeasures(Long deviceId, Integer limit);
}
