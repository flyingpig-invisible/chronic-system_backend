package com.chronic.modules.miniapp.service;

import com.chronic.modules.chronic.entity.CdPatient;

import java.util.List;

/**
 * 小程序数据权限：所有患者数据的唯一入口校验
 *
 * @author chronic
 */
public interface MpAccessService {

    /** 当前小程序用户 */
    com.chronic.modules.miniapp.entity.MpUser currentUser();

    /** 当前用户是否为家属 */
    boolean isFamily();

    /** 患者本人档案ID；家属调用时返回 null */
    Long ownPatientId();

    /** 当前账号可查看的全部患者ID（患者本人 + 已绑定亲属） */
    List<Long> accessiblePatientIds();

    /** 校验并返回患者档案，无权访问时抛出异常 */
    CdPatient requirePatient(Long patientId);

    /** 患者ID：为空时默认取当前账号可访问的第一个患者 */
    Long resolvePatientId(Long patientId);
}