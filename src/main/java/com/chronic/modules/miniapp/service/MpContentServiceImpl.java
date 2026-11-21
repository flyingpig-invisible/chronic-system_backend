package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdArticle;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.chronic.entity.CdDevice;
import com.chronic.modules.chronic.mapper.CdArticleMapper;
import com.chronic.modules.chronic.mapper.CdClinicMapper;
import com.chronic.modules.chronic.mapper.CdDeviceMapper;
import com.chronic.modules.miniapp.dto.MpClinicDTO;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpContentService;
import com.chronic.modules.miniapp.vo.MpArticleVO;
import com.chronic.modules.miniapp.vo.MpDeviceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 设备 / 科普 / 在线问诊服务实现
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class MpContentServiceImpl implements MpContentService {

    private final MpAccessService accessService;
    private final CdDeviceMapper deviceMapper;
    private final CdArticleMapper articleMapper;
    private final CdClinicMapper clinicMapper;

    private static final Map<String, String> DEVICE_TYPE = Map.of(
            "1", "智能手表", "2", "血压计", "3", "血氧仪", "4", "血糖仪");

    @Override
    public List<MpDeviceVO> devices(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        return deviceMapper.selectList(Wrappers.<CdDevice>lambdaQuery()
                        .eq(CdDevice::getPatientId, id)
                        .eq(CdDevice::getStatus, "0")
                        .orderByDesc(CdDevice::getBindTime))
                .stream().map(d -> {
                    MpDeviceVO vo = new MpDeviceVO();
                    vo.setDeviceId(d.getDeviceId());
                    vo.setDeviceNo(d.getDeviceNo());
                    vo.setDeviceName(StringUtils.hasText(d.getAlias()) ? d.getAlias() : d.getDeviceName());
                    vo.setDeviceType(d.getDeviceType());
                    vo.setDeviceTypeName(DEVICE_TYPE.getOrDefault(d.getDeviceType(), "健康设备"));
                    vo.setVendor(d.getVendor());
                    vo.setModelName(StringUtils.hasText(d.getModelName()) ? d.getModelName() : d.getModel());
                    vo.setAlias(d.getAlias());
                    vo.setOnlineStatus(d.getOnlineStatus());
                    vo.setBattery(d.getBattery());
                    vo.setLastOnline(d.getLastOnline());
                    vo.setLastSyncTime(d.getLastSyncTime());
                    vo.setBindTime(d.getBindTime());
                    return vo;
                }).collect(Collectors.toList());
    }

    @Override
    public void unbindDevice(Long patientId, Long deviceId) {
        Long id = accessService.resolvePatientId(patientId);
        CdDevice device = deviceMapper.selectById(deviceId);
        if (device == null || !Objects.equals(device.getPatientId(), id)) {
            throw new ServiceException("设备不存在或未绑定该患者");
        }
        device.setPatientId(null);
        device.setPatientName(null);
        device.setBindUserId(null);
        device.setBindTime(null);
        device.setLastSyncTime(null);
        deviceMapper.updateById(device);
    }

    @Override
    public PageResult<MpArticleVO> articles(Integer pageNum, Integer pageSize, String keyword, String diseaseCode) {
        LambdaQueryWrapper<CdArticle> wrapper = Wrappers.<CdArticle>lambdaQuery()
                .eq(CdArticle::getPublishStatus, "1")
                .eq(StringUtils.hasText(diseaseCode), CdArticle::getDiseaseCode, diseaseCode)
                .and(StringUtils.hasText(keyword), w -> w.like(CdArticle::getTitle, keyword)
                        .or().like(CdArticle::getSummary, keyword))
                .orderByDesc(CdArticle::getPublishTime);
        Page<CdArticle> page = articleMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<MpArticleVO> rows = page.getRecords().stream().map(a -> {
            MpArticleVO vo = toVO(a);
            vo.setContent(null);
            return vo;
        }).collect(Collectors.toList());
        return PageResult.of(page.getTotal(), rows);
    }

    @Override
    public MpArticleVO article(Long articleId) {
        CdArticle article = articleMapper.selectById(articleId);
        if (article == null || !"1".equals(article.getPublishStatus())) {
            throw new ServiceException("文章不存在或已下架");
        }
        CdArticle update = new CdArticle();
        update.setArticleId(articleId);
        update.setViewCount((article.getViewCount() == null ? 0 : article.getViewCount()) + 1);
        articleMapper.updateById(update);
        return toVO(article);
    }

    @Override
    public List<CdClinic> clinics(Long patientId) {
        Long id = accessService.resolvePatientId(patientId);
        return clinicMapper.selectList(Wrappers.<CdClinic>lambdaQuery()
                .eq(CdClinic::getPatientId, id)
                .orderByDesc(CdClinic::getApplyTime));
    }

    @Override
    public Long bookClinic(MpClinicDTO dto) {
        Long id = accessService.resolvePatientId(dto.getPatientId());
        long ongoing = clinicMapper.selectCount(Wrappers.<CdClinic>lambdaQuery()
                .eq(CdClinic::getPatientId, id)
                .in(CdClinic::getVisitStatus, "0", "1"));
        if (ongoing > 0) {
            throw new ServiceException("您已有进行中的问诊，请等待医生接诊");
        }
        CdClinic clinic = new CdClinic();
        clinic.setPatientId(id);
        clinic.setPatientName(accessService.requirePatient(id).getName());
        clinic.setVisitType(StringUtils.hasText(dto.getVisitType()) ? dto.getVisitType() : "1");
        clinic.setVisitStatus("0");
        clinic.setChiefComplaint(dto.getChiefComplaint());
        clinic.setImageUrls(limitImages(dto.getImageUrls()));
        clinic.setApplyTime(LocalDateTime.now());
        clinicMapper.insert(clinic);
        return clinic.getVisitId();
    }

    /** 主诉附图最多保留 6 张，且只接受本系统上传的相对路径 */
    private String limitImages(String imageUrls) {
        if (!StringUtils.hasText(imageUrls)) {
            return null;
        }
        return Arrays.stream(imageUrls.split(","))
                .map(String::trim)
                .filter(u -> u.startsWith("/upload/"))
                .limit(6)
                .collect(Collectors.joining(","));
    }

    @Override
    public void cancelClinic(Long visitId) {
        CdClinic clinic = clinicMapper.selectById(visitId);
        if (clinic == null) {
            throw new ServiceException("问诊记录不存在");
        }
        accessService.requirePatient(clinic.getPatientId());
        if (!"0".equals(clinic.getVisitStatus())) {
            throw new ServiceException("当前状态无法取消");
        }
        clinic.setVisitStatus("3");
        clinicMapper.updateById(clinic);
    }

    private MpArticleVO toVO(CdArticle a) {
        MpArticleVO vo = new MpArticleVO();
        vo.setArticleId(a.getArticleId());
        vo.setTitle(a.getTitle());
        vo.setCategoryName(a.getCategoryName());
        vo.setDiseaseCode(a.getDiseaseCode());
        vo.setSummary(a.getSummary());
        vo.setContent(a.getContent());
        vo.setCoverImage(a.getCoverImage());
        vo.setAuthor(a.getAuthor());
        vo.setViewCount(a.getViewCount());
        vo.setPublishTime(a.getPublishTime());
        return vo;
    }
}