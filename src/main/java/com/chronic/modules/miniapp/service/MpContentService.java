package com.chronic.modules.miniapp.service;

import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.miniapp.dto.MpClinicDTO;
import com.chronic.modules.miniapp.vo.MpArticleVO;
import com.chronic.modules.miniapp.vo.MpDeviceVO;

import java.util.List;

/**
 * 设备 / 科普 / 在线问诊
 *
 * @author chronic
 */
public interface MpContentService {

    List<MpDeviceVO> devices(Long patientId);

    void unbindDevice(Long patientId, Long deviceId);

    PageResult<MpArticleVO> articles(Integer pageNum, Integer pageSize, String keyword, String diseaseCode);

    MpArticleVO article(Long articleId);

    List<CdClinic> clinics(Long patientId);

    Long bookClinic(MpClinicDTO dto);

    void cancelClinic(Long visitId);
}