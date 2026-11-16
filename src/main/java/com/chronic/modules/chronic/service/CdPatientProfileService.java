package com.chronic.modules.chronic.service;

import com.chronic.modules.chronic.dto.ProfileSaveDTO;
import com.chronic.modules.chronic.vo.ProfileVO;
import com.chronic.modules.chronic.entity.CdPatientProfile;

/**
 * 患者专属档案服务
 *
 * <p>可见范围：医生端与小程序家属端；患者本人不可见。</p>
 * <p>编辑权限：医疗类字段仅医生可改，生活类字段医生与家属均可改。</p>
 *
 * @author chronic
 */
public interface CdPatientProfileService {

    /** 医生端获取患者专属档案（含可编辑字段与留痕） */
    ProfileVO getProfileForDoctor(Long patientId);

    /** 家属端获取患者专属档案（校验绑定关系） */
    ProfileVO getProfileForFamily(Long patientId);

    /** 保存档案（自动按角色过滤可编辑字段，并记录留痕） */
    void saveProfile(ProfileSaveDTO dto, boolean familyOnly);

    /** 供 AI 使用的档案上下文文本（仅取已填写内容） */
    String buildProfileContext(Long patientId);

    /** 获取档案主体（不存在返回 null） */
    CdPatientProfile getByPatientId(Long patientId);
}
