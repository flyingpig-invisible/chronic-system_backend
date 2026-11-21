package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ClinicFinishDTO;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.chronic.mapper.CdClinicMapper;
import com.chronic.modules.chronic.query.ClinicQuery;
import com.chronic.modules.chronic.service.CdClinicService;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Service
public class CdClinicServiceImpl extends ServiceImpl<CdClinicMapper, CdClinic> implements CdClinicService {

    @Override
    public PageResult<CdClinic> queryPage(ClinicQuery query) {
        LambdaQueryWrapper<CdClinic> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdClinic::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getVisitType()), CdClinic::getVisitType, query.getVisitType())
         .eq(StringUtils.hasText(query.getVisitStatus()), CdClinic::getVisitStatus, query.getVisitStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdClinic::getPatientName, query.getKeyword())
         .orderByDesc(CdClinic::getCreateTime);
        Page<CdClinic> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void book(CdClinic clinic) {
        if (clinic.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        clinic.setVisitId(null);
        clinic.setVisitStatus("0");
        clinic.setApplyTime(LocalDateTime.now());
        save(clinic);
    }

    @Override
    public void accept(Long visitId) {
        CdClinic clinic = getById(visitId);
        if (clinic == null) {
            throw new ServiceException("就诊记录不存在");
        }
        if (!"0".equals(clinic.getVisitStatus())) {
            throw new ServiceException("该就诊当前状态无法接诊");
        }
        LoginUser user = SecurityUtils.getLoginUser();
        clinic.setVisitStatus("1");
        clinic.setDoctorId(user.getUserId());
        clinic.setDoctorName(user.getRealName());
        updateById(clinic);
    }

    @Override
    public void finish(Long visitId, ClinicFinishDTO dto) {
        CdClinic clinic = getById(visitId);
        if (clinic == null) {
            throw new ServiceException("就诊记录不存在");
        }
        clinic.setDiagnosis(dto.getDiagnosis());
        clinic.setAdvice(dto.getAdvice());
        clinic.setPrescription(dto.getPrescription());
        clinic.setAdviceImages(limitImages(dto.getAdviceImages()));
        clinic.setVisitStatus("2");
        clinic.setFinishTime(LocalDateTime.now());
        if (clinic.getDoctorId() == null) {
            clinic.setDoctorId(SecurityUtils.getUserId());
            clinic.setDoctorName(SecurityUtils.getLoginUser().getRealName());
        }
        updateById(clinic);
    }

    /** 诊断附图最多 9 张，且只接受本系统上传的相对路径 */
    private String limitImages(String images) {
        if (!StringUtils.hasText(images)) {
            return null;
        }
        String joined = java.util.Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(u -> u.startsWith("/upload/"))
                .limit(9)
                .collect(java.util.stream.Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }

    @Override
    public void cancel(Long visitId) {
        CdClinic clinic = getById(visitId);
        if (clinic == null) {
            throw new ServiceException("就诊记录不存在");
        }
        clinic.setVisitStatus("3");
        updateById(clinic);
    }
}
