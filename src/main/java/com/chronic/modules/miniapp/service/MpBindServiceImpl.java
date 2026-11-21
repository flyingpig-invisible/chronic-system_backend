package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.BindAuditDTO;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.miniapp.dto.MpBindApplyDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.mapper.MpBindApplyMapper;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.modules.miniapp.mapper.MpUserMapper;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpBindService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 家属-患者绑定服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpBindServiceImpl implements MpBindService {

    private final MpBindApplyMapper applyMapper;
    private final MpFamilyPatientMapper familyPatientMapper;
    private final MpUserMapper userMapper;
    private final CdPatientMapper patientMapper;
    private final MpAccessService accessService;

    @Override
    public Long apply(MpBindApplyDTO dto) {
        MpUser family = accessService.currentUser();
        if (!"2".equals(family.getUserRole())) {
            throw new ServiceException("仅家属账号可以发起绑定申请");
        }
        String keyword = dto.getPatientKeyword().trim();
        CdPatient patient = patientMapper.selectOne(Wrappers.<CdPatient>lambdaQuery()
                .and(w -> w.eq(CdPatient::getPhone, keyword).or().eq(CdPatient::getPatientNo, keyword))
                .last("limit 1"));
        if (patient == null) {
            throw new ServiceException("未找到该患者，请核对手机号或患者编号");
        }
        Long bound = familyPatientMapper.selectCount(Wrappers.<MpFamilyPatient>lambdaQuery()
                .eq(MpFamilyPatient::getFamilyUserId, family.getUserId())
                .eq(MpFamilyPatient::getPatientId, patient.getPatientId()));
        if (bound != null && bound > 0) {
            throw new ServiceException("您已绑定该患者");
        }
        Long pending = applyMapper.selectCount(Wrappers.<MpBindApply>lambdaQuery()
                .eq(MpBindApply::getFamilyUserId, family.getUserId())
                .eq(MpBindApply::getPatientId, patient.getPatientId())
                .eq(MpBindApply::getBindStatus, "0"));
        if (pending != null && pending > 0) {
            throw new ServiceException("已提交过申请，请等待患者确认");
        }
        MpBindApply apply = new MpBindApply();
        apply.setFamilyUserId(family.getUserId());
        apply.setFamilyName(family.getRealName());
        apply.setPatientId(patient.getPatientId());
        apply.setPatientName(patient.getName());
        apply.setRelation(dto.getRelation());
        apply.setApplyRemark(dto.getApplyRemark());
        apply.setPatientStatus("0");
        apply.setDoctorStatus("0");
        apply.setBindStatus("0");
        apply.setApplyTime(LocalDateTime.now());
        applyMapper.insert(apply);
        return apply.getApplyId();
    }

    @Override
    public List<MpBindApply> myApplies() {
        MpUser user = accessService.currentUser();
        List<MpBindApply> list = "2".equals(user.getUserRole())
                ? applyMapper.selectList(Wrappers.<MpBindApply>lambdaQuery()
                        .eq(MpBindApply::getFamilyUserId, user.getUserId())
                        .orderByDesc(MpBindApply::getApplyTime))
                : applyMapper.selectList(Wrappers.<MpBindApply>lambdaQuery()
                        .eq(MpBindApply::getPatientId, accessService.resolvePatientId(null))
                        .orderByDesc(MpBindApply::getApplyTime));
        fillNames(list);
        return list;
    }

    @Override
    public List<MpBindApply> pendingForPatient() {
        Long patientId = accessService.resolvePatientId(null);
        List<MpBindApply> list = applyMapper.selectList(Wrappers.<MpBindApply>lambdaQuery()
                .eq(MpBindApply::getPatientId, patientId)
                .eq(MpBindApply::getPatientStatus, "0")
                .eq(MpBindApply::getBindStatus, "0")
                .orderByDesc(MpBindApply::getApplyTime));
        fillNames(list);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void patientConfirm(Long applyId, boolean agree) {
        MpBindApply apply = requireApply(applyId);
        Long patientId = accessService.resolvePatientId(null);
        if (!Objects.equals(apply.getPatientId(), patientId)) {
            throw new ServiceException(403, "无权处理该申请");
        }
        if (!"0".equals(apply.getPatientStatus())) {
            throw new ServiceException("该申请已处理");
        }
        apply.setPatientStatus(agree ? "1" : "2");
        apply.setPatientTime(LocalDateTime.now());
        if (!agree) {
            apply.setBindStatus("2");
        }
        applyMapper.updateById(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(Long patientId) {
        MpUser family = accessService.currentUser();
        familyPatientMapper.delete(Wrappers.<MpFamilyPatient>lambdaQuery()
                .eq(MpFamilyPatient::getFamilyUserId, family.getUserId())
                .eq(MpFamilyPatient::getPatientId, patientId));
    }

    @Override
    public List<Map<String, Object>> myRelations() {
        MpUser user = accessService.currentUser();
        List<MpFamilyPatient> relations = familyPatientMapper.selectList(Wrappers.<MpFamilyPatient>lambdaQuery()
                .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                .orderByAsc(MpFamilyPatient::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (MpFamilyPatient rel : relations) {
            CdPatient patient = patientMapper.selectById(rel.getPatientId());
            if (patient == null) {
                continue;
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("patientId", patient.getPatientId());
            m.put("patientNo", patient.getPatientNo());
            m.put("name", patient.getName());
            m.put("relation", rel.getRelation());
            m.put("bindTime", rel.getBindTime());
            m.put("riskLevel", patient.getRiskLevel());
            result.add(m);
        }
        return result;
    }

    @Override
    public PageResult<MpBindApply> pageApply(Integer pageNum, Integer pageSize, String bindStatus) {
        Page<MpBindApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<MpBindApply>lambdaQuery()
                        .eq(StringUtils.hasText(bindStatus), MpBindApply::getBindStatus, bindStatus)
                        .orderByDesc(MpBindApply::getApplyTime));
        fillNames(page.getRecords());
        return PageResult.of(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doctorAudit(Long applyId, BindAuditDTO dto) {
        MpBindApply apply = requireApply(applyId);
        if (!"1".equals(apply.getPatientStatus())) {
            throw new ServiceException("患者尚未确认该申请");
        }
        if (!"0".equals(apply.getDoctorStatus())) {
            throw new ServiceException("该申请已审核");
        }
        LoginUser doctor = SecurityUtils.getLoginUser();
        boolean pass = "1".equals(dto.getResult()) || "true".equalsIgnoreCase(dto.getResult())
                || "通过".equals(dto.getResult());
        apply.setDoctorStatus(pass ? "1" : "2");
        apply.setBindStatus(pass ? "1" : "2");
        apply.setAuditOpinion(dto.getAuditOpinion());
        apply.setDoctorId(doctor.getUserId());
        apply.setDoctorName(doctor.getRealName());
        apply.setAuditTime(LocalDateTime.now());
        applyMapper.updateById(apply);

        if (pass) {
            Long exists = familyPatientMapper.selectCount(Wrappers.<MpFamilyPatient>lambdaQuery()
                    .eq(MpFamilyPatient::getFamilyUserId, apply.getFamilyUserId())
                    .eq(MpFamilyPatient::getPatientId, apply.getPatientId()));
            if (exists == null || exists == 0) {
                MpFamilyPatient rel = new MpFamilyPatient();
                rel.setFamilyUserId(apply.getFamilyUserId());
                rel.setPatientId(apply.getPatientId());
                rel.setRelation(apply.getRelation());
                rel.setBindTime(LocalDateTime.now());
                familyPatientMapper.insert(rel);
            }
        }
    }

    private MpBindApply requireApply(Long applyId) {
        MpBindApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new ServiceException("绑定申请不存在");
        }
        return apply;
    }

    private void fillNames(List<MpBindApply> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> userIds = list.stream().map(MpBindApply::getFamilyUserId)
                .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        Map<Long, String> phoneMap = userIds.isEmpty() ? new LinkedHashMap<>()
                : userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(MpUser::getUserId, u -> u.getPhone() == null ? "" : u.getPhone()));
        list.forEach(a -> a.setFamilyPhone(phoneMap.get(a.getFamilyUserId())));
    }
}