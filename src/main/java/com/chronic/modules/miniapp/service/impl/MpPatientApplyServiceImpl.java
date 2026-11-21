package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.service.CdPatientService;
import com.chronic.modules.miniapp.dto.MpPatientApplyDTO;
import com.chronic.modules.miniapp.entity.MpPatientApply;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.mapper.MpPatientApplyMapper;
import com.chronic.modules.miniapp.mapper.MpPatientMapper;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpPatientApplyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 患者端建档案申请实现
 * <p>
 * 患者在小程序注册后不会自动认领医生端档案，而是提交建档申请；
 * 医生端审核通过后才创建正式档案并关联小程序账号。
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpPatientApplyServiceImpl implements MpPatientApplyService {

    private static final Map<String, String> DISEASE_NAME = Map.of(
            "1", "高血压",
            "2", "2型糖尿病",
            "3", "冠心病",
            "4", "慢阻肺");

    private static final Map<String, String> STATUS_NAME = Map.of(
            "0", "待审核",
            "1", "已通过",
            "2", "已驳回");

    private final MpPatientApplyMapper applyMapper;
    private final MpPatientMapper patientMapper;
    private final MpAccessService accessService;
    private final CdPatientService patientService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submit(MpPatientApplyDTO dto) {
        MpUser user = accessService.currentUser();
        if ("2".equals(user.getUserRole())) {
            throw new ServiceException("家属账号请使用“绑定亲属”功能，不支持申请建档");
        }
        if (!StringUtils.hasText(dto.getRealName())) {
            throw new ServiceException("请填写真实姓名");
        }
        Long pending = applyMapper.selectCount(Wrappers.<MpPatientApply>lambdaQuery()
                .eq(MpPatientApply::getMpUserId, user.getUserId())
                .eq(MpPatientApply::getApplyStatus, "0"));
        if (pending != null && pending > 0) {
            throw new ServiceException("已提交过申请，请等待医生审核");
        }
        MpPatientApply apply = new MpPatientApply();
        apply.setMpUserId(user.getUserId());
        apply.setUserName(dto.getRealName().trim());
        apply.setUserPhone(user.getPhone());
        apply.setUserGender(StringUtils.hasText(dto.getGender()) ? dto.getGender() : "2");
        apply.setBirthday(dto.getBirthday());
        apply.setIdCard(dto.getIdCard());
        apply.setAddress(dto.getAddress());
        apply.setDiseaseCodes(normalizeDiseases(dto.getDiseaseCodes()));
        apply.setApplyRemark(dto.getApplyRemark());
        apply.setApplyStatus("0");
        apply.setApplyTime(LocalDateTime.now());
        applyMapper.insert(apply);
        return apply.getApplyId();
    }

    /** 允许 1,2 或 1,,2 之类的写法，统一成逗号分隔且仅保留已知病种 */
    private String normalizeDiseases(String codes) {
        if (!StringUtils.hasText(codes)) {
            return "";
        }
        return Arrays.stream(codes.split(","))
                .map(String::trim)
                .filter(c -> DISEASE_NAME.containsKey(c))
                .distinct()
                .collect(Collectors.joining(","));
    }

    @Override
    public MpPatientApply myLatest() {
        MpUser user = accessService.currentUser();
        MpPatientApply apply = applyMapper.selectOne(Wrappers.<MpPatientApply>lambdaQuery()
                .eq(MpPatientApply::getMpUserId, user.getUserId())
                .orderByDesc(MpPatientApply::getApplyId)
                .last("limit 1"));
        return apply == null ? null : decorate(apply);
    }

    @Override
    public PageResult<MpPatientApply> pageApply(Integer pageNum, Integer pageSize, String applyStatus) {
        Page<MpPatientApply> page = applyMapper.selectPage(new Page<>(pageNum, pageSize),
                Wrappers.<MpPatientApply>lambdaQuery()
                        .eq(StringUtils.hasText(applyStatus), MpPatientApply::getApplyStatus, applyStatus)
                        .orderByDesc(MpPatientApply::getApplyId));
        page.getRecords().forEach(this::decorate);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public long pendingCount() {
        Long count = applyMapper.selectCount(Wrappers.<MpPatientApply>lambdaQuery()
                .eq(MpPatientApply::getApplyStatus, "0"));
        return count == null ? 0L : count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long audit(Long applyId, String result, String auditOpinion, Long doctorId) {
        MpPatientApply apply = applyMapper.selectById(applyId);
        if (apply == null) {
            throw new ServiceException("建档申请不存在");
        }
        if (!"0".equals(apply.getApplyStatus())) {
            throw new ServiceException("该申请已审核");
        }
        boolean pass = "1".equals(result) || "true".equalsIgnoreCase(result) || "通过".equals(result);
        apply.setDoctorId(doctorId);
        apply.setAuditOpinion(auditOpinion);
        apply.setAuditTime(LocalDateTime.now());
        if (!pass) {
            apply.setApplyStatus("2");
            applyMapper.updateById(apply);
            return null;
        }

        // 该小程序账号已关联档案时不允许重复建档
        if (patientMapper.selectByMpUserId(apply.getMpUserId()) != null) {
            throw new ServiceException("该小程序账号已关联健康档案，无需重复建档");
        }
        CdPatient existing = patientMapper.selectByPhone(apply.getUserPhone());
        Long patientId;
        if (existing != null) {
            // 手机号已存在档案：不再新建，直接把档案关联到小程序账号
            if (existing.getMpUserId() != null && existing.getMpUserId() != 0L) {
                throw new ServiceException("该手机号已有档案且已关联其他账号，请核对后再处理");
            }
            patientMapper.linkUser(existing.getPatientId(), apply.getMpUserId());
            patientId = existing.getPatientId();
        } else {
            CdPatient patient = new CdPatient();
            patient.setName(apply.getUserName());
            patient.setPhone(apply.getUserPhone());
            patient.setGender(apply.getUserGender());
            patient.setBirthday(apply.getBirthday());
            patient.setIdCard(apply.getIdCard());
            patient.setAddress(apply.getAddress());
            patient.setDoctorId(doctorId);
            patient.setMpUserId(apply.getMpUserId());
            patient.setRemark("患者端申请建档");
            if (StringUtils.hasText(apply.getDiseaseCodes())) {
                patient.setDiseaseCodes(Arrays.asList(apply.getDiseaseCodes().split(",")));
            }
            patient.setCreateBy(apply.getUserName());
            patientId = patientService.addPatient(patient);
            patientMapper.linkUser(patientId, apply.getMpUserId());
        }

        apply.setApplyStatus("1");
        apply.setPatientId(patientId);
        applyMapper.updateById(apply);
        return patientId;
    }

    /** 补齐展示字段：病种名称、年龄、状态名称 */
    private MpPatientApply decorate(MpPatientApply apply) {
        apply.setApplyStatusName(STATUS_NAME.getOrDefault(apply.getApplyStatus(), "-"));
        if (StringUtils.hasText(apply.getDiseaseCodes())) {
            apply.setDiseaseNames(Arrays.stream(apply.getDiseaseCodes().split(","))
                    .map(String::trim)
                    .map(c -> DISEASE_NAME.getOrDefault(c, c))
                    .collect(Collectors.joining("、")));
        } else {
            apply.setDiseaseNames("");
        }
        if (apply.getBirthday() != null) {
            apply.setAge(Period.between(apply.getBirthday(), LocalDate.now()).getYears());
        }
        return apply;
    }
}