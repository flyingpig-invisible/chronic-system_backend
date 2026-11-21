package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.modules.miniapp.mapper.MpPatientMapper;
import com.chronic.modules.miniapp.mapper.MpUserMapper;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 小程序数据权限实现
 * <p>
 * 患者账号：仅本人档案；家属账号：已完成绑定的亲属档案。
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class MpAccessServiceImpl implements MpAccessService {

    private final MpUserMapper userMapper;
    private final MpFamilyPatientMapper familyPatientMapper;
    private final MpPatientMapper patientMapper;

    @Override
    public MpUser currentUser() {
        Long userId = SecurityUtils.getLoginUser().getUserId();
        MpUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    @Override
    public boolean isFamily() {
        return "2".equals(currentUser().getUserRole());
    }

    @Override
    public Long ownPatientId() {
        if (isFamily()) {
            return null;
        }
        CdPatient patient = patientMapper.selectByMpUserId(currentUser().getUserId());
        return patient == null ? null : patient.getPatientId();
    }

    @Override
    public List<Long> accessiblePatientIds() {
        MpUser user = currentUser();
        Set<Long> ids = new LinkedHashSet<>();
        if (!"2".equals(user.getUserRole())) {
            CdPatient own = patientMapper.selectByMpUserId(user.getUserId());
            if (own != null) {
                ids.add(own.getPatientId());
            }
        }
        familyPatientMapper.selectList(Wrappers.<MpFamilyPatient>lambdaQuery()
                        .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                        .orderByAsc(MpFamilyPatient::getId))
                .forEach(rel -> ids.add(rel.getPatientId()));
        return new ArrayList<>(ids);
    }

    @Override
    public CdPatient requirePatient(Long patientId) {
        if (patientId == null) {
            throw new ServiceException("缺少患者信息");
        }
        if (!accessiblePatientIds().contains(patientId)) {
            throw new ServiceException(403, "无权查看该患者数据");
        }
        CdPatient patient = patientMapper.selectPatient(patientId);
        if (patient == null) {
            throw new ServiceException("患者档案不存在");
        }
        return patient;
    }

    @Override
    public Long resolvePatientId(Long patientId) {
        if (patientId != null) {
            requirePatient(patientId);
            return patientId;
        }
        List<Long> ids = accessiblePatientIds();
        if (ids.isEmpty()) {
            throw new ServiceException(404, "尚未关联患者档案，请联系医生");
        }
        return ids.get(0);
    }
}