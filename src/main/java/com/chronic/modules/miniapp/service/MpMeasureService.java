package com.chronic.modules.miniapp.service;

import com.chronic.modules.miniapp.dto.MpMeasureDTO;
import com.chronic.modules.miniapp.vo.MpMeasureVO;

/**
 * 小程序端监测数据录入
 *
 * @author chronic
 */
public interface MpMeasureService {

    /** 一次提交一组指标，空值自动跳过 */
    MpMeasureVO submit(MpMeasureDTO dto);
}
