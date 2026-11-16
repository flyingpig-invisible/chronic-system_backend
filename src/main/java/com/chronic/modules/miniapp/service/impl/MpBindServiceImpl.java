package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.miniapp.dto.BindApplyDTO;
import com.chronic.modules.miniapp.dto.BindAuditDTO;
import com.chronic.modules.miniapp.entity.MpBindApply;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.mapper.MpBindApplyMapper;
import com.chronic.modules.miniapp.mapper.MpFamilyPatientMapper;
import com.chronic.modules.miniapp.mapper.MpUserMapper;
import com.chronic.modules.miniapp.service.MpBindService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 家属-患者绑定服务：家属发起 -> 患者同意 -> 医生审核 -> 绑定成功
 *
 * @author chronic
 */
@Service
public class MpBindServiceImpl extends ServiceImpl<MpBindApplyMapper, MpBindApply> implements MpBindService {

    @Autowired
    private MpFamilyPatientMapper familyPatientMapper;

    @Autowired
    private MpUserMapper mpUserMapper;

    @Autowired
    private CdPatientMapper patientMapper;

    @Override
    public Long applyBind(BindApplyDTO dto) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (loginUser == null || !"mp".equals(loginUser.getUserType())) {
            throw new ServiceException("仅小程序用户可发起绑定");
        }
        CdPatient patient = patientMapper.selectById(dto.getPatientId());
        if (patient == null) {
            throw new ServiceException("患者不存在");
        }
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<MpBindApply>()
                .eq(MpBindApply::getFamilyUserId, loginUser.getUserId())
                .eq(MpBindApply::getPatientId, dto.getPatientId())
                .in(MpBindApply::getBindStatus, "0", "1"));
        if (count != null && count > 0) {
            throw new ServiceException("已存在绑定申请或已绑定，请勿重复申请");
        }

        MpBindApply apply = new MpBindApply();
        apply.setFamilyUserId(loginUser.getUserId());
        MpUser family = mpUserMapper.selectById(loginUser.getUserId());
        apply.setFamilyName(family == null ? loginUser.getUsername() : family.getRealName());
        apply.setPatientId(dto.getPatientId());
        apply.setPatientName(patient.getName());
        apply.setRelation(dto.getRelation());
        apply.setApplyRemark(dto.getApplyRemark());
        apply.setPatientStatus("0");
        apply.setDoctorStatus("0");
        apply.setBindStatus("0");
        apply.setApplyTime(LocalDateTime.now());
        save(apply);
        return apply.getApplyId();
    }

    @Override
    public void patientConfirm(Long applyId, BindAuditDTO dto) {
        MpBindApply apply = getById(applyId);
        if (apply == null) {
            throw new ServiceException("绑定申请不存在");
        }
        if (!"0".equals(apply.getPatientStatus())) {
            throw new ServiceException("患者已确认，无法重复操作");
        }
        apply.setPatientStatus("1".equals(dto.getResult()) ? "1" : "2");
        apply.setPatientTime(LocalDateTime.now());
        if ("2".equals(apply.getPatientStatus())) {
            apply.setBindStatus("2");
        }
        updateById(apply);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doctorAudit(Long applyId, BindAuditDTO dto) {
        MpBindApply apply = getById(applyId);
        if (apply == null) {
            throw new ServiceException("绑定申请不存在");
        }
        if (!"1".equals(apply.getPatientStatus())) {
            throw new ServiceException("需患者先同意后才能医生审核");
        }
        if (!"0".equals(apply.getDoctorStatus())) {
            throw new ServiceException("该申请已审核");
        }
        LoginUser doctor = SecurityUtils.getLoginUser();
        apply.setDoctorStatus("1".equals(dto.getResult()) ? "1" : "2");
        apply.setDoctorId(doctor.getUserId());
        apply.setDoctorName(doctor.getRealName());
        apply.setAuditOpinion(dto.getAuditOpinion());
        apply.setAuditTime(LocalDateTime.now());

        if ("1".equals(apply.getDoctorStatus())) {
            apply.setBindStatus("1");
            Long exists = familyPatientMapper.selectCount(new LambdaQueryWrapper<MpFamilyPatient>()
                    .eq(MpFamilyPatient::getFamilyUserId, apply.getFamilyUserId())
                    .eq(MpFamilyPatient::getPatientId, apply.getPatientId()));
            if (exists == null || exists == 0) {
                MpFamilyPatient fp = new MpFamilyPatient();
                fp.setFamilyUserId(apply.getFamilyUserId());
                fp.setPatientId(apply.getPatientId());
                fp.setRelation(apply.getRelation());
                fp.setBindTime(LocalDateTime.now());
                familyPatientMapper.insert(fp);
            }
        } else {
            apply.setBindStatus("2");
        }
        updateById(apply);
    }

    @Override
    public PageResult<MpBindApply> pageApply(Integer pageNum, Integer pageSize, String bindStatus) {
        LambdaQueryWrapper<MpBindApply> w = new LambdaQueryWrapper<>();
        w.eq(StringUtils.hasText(bindStatus), MpBindApply::getBindStatus, bindStatus)
         .orderByDesc(MpBindApply::getApplyTime);
        Page<MpBindApply> page = page(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<MpBindApply> myApplies() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return list(new LambdaQueryWrapper<MpBindApply>()
                .eq(MpBindApply::getFamilyUserId, loginUser.getUserId())
                .orderByDesc(MpBindApply::getApplyTime));
    }

    @Override
    public List<MpFamilyPatient> myPatients() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return familyPatientMapper.selectList(new LambdaQueryWrapper<MpFamilyPatient>()
                .eq(MpFamilyPatient::getFamilyUserId, loginUser.getUserId()));
    }

    @Override
    public void unbind(Long id) {
        familyPatientMapper.deleteById(id);
    }
}
