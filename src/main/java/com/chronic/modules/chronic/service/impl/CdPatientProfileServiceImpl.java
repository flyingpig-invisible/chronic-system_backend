package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.dto.ProfileSaveDTO;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.entity.CdPatientProfile;
import com.chronic.modules.chronic.entity.CdPatientProfileLog;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdPatientProfileLogMapper;
import com.chronic.modules.chronic.mapper.CdPatientProfileMapper;
import com.chronic.modules.chronic.service.CdPatientProfileService;
import com.chronic.modules.chronic.service.PatientDataScopeService;
import com.chronic.modules.chronic.vo.ProfileVO;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 患者专属档案服务实现
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CdPatientProfileServiceImpl implements CdPatientProfileService {

    private final CdPatientProfileMapper profileMapper;
    private final CdPatientProfileLogMapper logMapper;
    private final CdPatientMapper patientMapper;
    private final MpFamilyPatientMapper familyPatientMapper;
    private final PatientDataScopeService dataScopeService;

    /** 医疗类字段（仅医生可编辑） */
    private static final List<String> MEDICAL_FIELDS = List.of(
            "bloodType", "allergyHistory", "familyHistory", "pastHistory",
            "surgeryHistory", "longMedication", "disabilityNote");

    /** 生活类字段（医生与家属均可编辑） */
    private static final List<String> LIFE_FIELDS = List.of(
            "dietHabit", "sleepHabit", "moodStatus", "exerciseHabit", "homeObservation");

    /** 通用字段（均可不填，医生与家属均可编辑） */
    private static final List<String> COMMON_FIELDS = List.of("extraNote");

    /** 字段中文名 */
    private static final Map<String, String> FIELD_CN = Map.ofEntries(
            Map.entry("bloodType", "血型"),
            Map.entry("allergyHistory", "过敏史"),
            Map.entry("familyHistory", "家族史"),
            Map.entry("pastHistory", "既往病史"),
            Map.entry("surgeryHistory", "既往手术史"),
            Map.entry("longMedication", "长期用药"),
            Map.entry("disabilityNote", "残疾/行动不便说明"),
            Map.entry("dietHabit", "饮食习惯"),
            Map.entry("sleepHabit", "作息与睡眠"),
            Map.entry("moodStatus", "情绪与心理状态"),
            Map.entry("exerciseHabit", "运动习惯"),
            Map.entry("homeObservation", "家属在家观察"),
            Map.entry("extraNote", "补充说明"));

    private static final int LOG_LIMIT = 20;

    @Override
    public ProfileVO getProfileForDoctor(Long patientId) {
        // 医生只能读取自己科室可见患者的专属档案
        dataScopeService.checkAccess(patientId);
        requirePatient(patientId);
        return buildVO(patientId, "doctor");
    }

    @Override
    public ProfileVO getProfileForFamily(Long patientId) {
        LoginUser user = SecurityUtils.getLoginUser();
        if (user == null) {
            throw new ServiceException(401, "未登录");
        }
        // 患者本人不可查看专属档案
        if ("1".equals(user.getMpRole())) {
            throw new ServiceException(403, "该档案不对患者本人开放");
        }
        Long count = familyPatientMapper.selectCount(new LambdaQueryWrapper<MpFamilyPatient>()
                .eq(MpFamilyPatient::getFamilyUserId, user.getUserId())
                .eq(MpFamilyPatient::getPatientId, patientId));
        if (count == null || count == 0) {
            throw new ServiceException(403, "无权查看该患者数据");
        }
        return buildVO(patientId, "family");
    }

    private ProfileVO buildVO(Long patientId, String role) {
        CdPatientProfile profile = getByPatientId(patientId);
        ProfileVO vo = new ProfileVO();
        vo.setProfile(profile);
        vo.setViewerRole(role);
        vo.setEditableFields(editableFields(role));
        if (profile != null) {
            vo.setLogs(logMapper.selectList(Wrappers.<CdPatientProfileLog>lambdaQuery()
                    .eq(CdPatientProfileLog::getPatientId, patientId)
                    .orderByDesc(CdPatientProfileLog::getLogId)
                    .last("limit " + LOG_LIMIT)));
        } else {
            vo.setLogs(Collections.emptyList());
        }
        return vo;
    }

    /** 当前角色可编辑的字段集合 */
    private List<String> editableFields(String role) {
        List<String> list = new ArrayList<>();
        if ("doctor".equals(role)) {
            list.addAll(MEDICAL_FIELDS);
        }
        list.addAll(LIFE_FIELDS);
        list.addAll(COMMON_FIELDS);
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(ProfileSaveDTO dto, boolean familyOnly) {
        Long patientId = dto.getPatientId();
        if (!familyOnly) {
            dataScopeService.checkAccess(patientId);
        }
        CdPatient patient = requirePatient(patientId);

        LoginUser user = SecurityUtils.getLoginUser();
        String editorName = resolveEditorName(user);
        String role = familyOnly ? "2" : "1";

        CdPatientProfile old = getByPatientId(patientId);
        CdPatientProfile target = old == null ? new CdPatientProfile() : old;
        List<String> editable = editableFields(familyOnly ? "family" : "doctor");

        List<CdPatientProfileLog> logs = new ArrayList<>();
        // 逐个字段处理：仅允许编辑者可编辑的字段；记录变更留痕
        List<String> allFields = new ArrayList<>();
        allFields.addAll(MEDICAL_FIELDS);
        allFields.addAll(LIFE_FIELDS);
        allFields.addAll(COMMON_FIELDS);

        for (String f : allFields) {
            String newVal = readField(dto, f);
            if (newVal == null) {
                continue; // 未提交该字段
            }
            if (!editable.contains(f)) {
                continue; // 当前角色无权编辑，静默忽略
            }
            String oldVal = old == null ? "" : nvl(readField(old, f));
            if (newVal.equals(oldVal)) {
                continue; // 无变化
            }
            writeField(target, f, newVal);
            CdPatientProfileLog logRow = new CdPatientProfileLog();
            logRow.setPatientId(patientId);
            logRow.setEditor(editorName);
            logRow.setEditorRole(role);
            logRow.setFieldKey(f);
            logRow.setFieldName(FIELD_CN.getOrDefault(f, f));
            logRow.setOldValue(trunc(oldVal));
            logRow.setNewValue(trunc(newVal));
            logRow.setEditTime(LocalDateTime.now());
            logs.add(logRow);
        }

        if (old == null && logs.isEmpty()) {
            // 没有任何字段被填写，不创建空档案
            return;
        }

        target.setPatientId(patientId);
        target.setPatientName(patient.getName());
        target.setLastEditor(editorName);
        target.setLastEditorRole(role);
        target.setLastEditTime(LocalDateTime.now());

        if (old == null) {
            profileMapper.insert(target);
        } else {
            profileMapper.updateById(target);
        }
        for (CdPatientProfileLog l : logs) {
            l.setProfileId(target.getProfileId());
            logMapper.insert(l);
        }
    }

    @Override
    public String buildProfileContext(Long patientId) {
        CdPatientProfile p = getByPatientId(patientId);
        if (p == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        appendIf(sb, "血型", p.getBloodType());
        appendIf(sb, "过敏史", p.getAllergyHistory());
        appendIf(sb, "家族史", p.getFamilyHistory());
        appendIf(sb, "既往病史", p.getPastHistory());
        appendIf(sb, "既往手术史", p.getSurgeryHistory());
        appendIf(sb, "长期用药", p.getLongMedication());
        appendIf(sb, "残疾/行动不便", p.getDisabilityNote());
        appendIf(sb, "饮食习惯", p.getDietHabit());
        appendIf(sb, "作息与睡眠", p.getSleepHabit());
        appendIf(sb, "情绪与心理状态", p.getMoodStatus());
        appendIf(sb, "运动习惯", p.getExerciseHabit());
        appendIf(sb, "家属在家观察", p.getHomeObservation());
        appendIf(sb, "补充说明", p.getExtraNote());
        if (sb.length() == 0) {
            return "";
        }
        return "\n【患者专属档案】\n" + sb;
    }

    @Override
    public CdPatientProfile getByPatientId(Long patientId) {
        if (patientId == null) {
            return null;
        }
        return profileMapper.selectOne(Wrappers.<CdPatientProfile>lambdaQuery()
                .eq(CdPatientProfile::getPatientId, patientId)
                .last("limit 1"));
    }

    private void appendIf(StringBuilder sb, String label, String val) {
        if (StringUtils.hasText(val)) {
            sb.append("- ").append(label).append("：").append(val).append("\n");
        }
    }

    private CdPatient requirePatient(Long patientId) {
        if (patientId == null) {
            throw new ServiceException("患者不能为空");
        }
        CdPatient patient = patientMapper.selectById(patientId);
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        return patient;
    }

    /** 解析当前填写人名称：医生取真实姓名/用户名，家属取小程序昵称 */
    private String resolveEditorName(LoginUser user) {
        if (user == null) {
            return "system";
        }
        if (StringUtils.hasText(user.getRealName())) {
            return user.getRealName();
        }
        return StringUtils.hasText(user.getUsername()) ? user.getUsername() : "未知";
    }

    private String readField(Object obj, String name) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            Object v = f.get(obj);
            return v == null ? null : v.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private void writeField(Object obj, String name, String value) {
        try {
            Field f = obj.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception e) {
            log.warn("写入档案字段失败: {}", name);
        }
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String trunc(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 480 ? s.substring(0, 480) : s;
    }
}
