package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdGuide;
import com.chronic.modules.chronic.mapper.CdGuideMapper;
import com.chronic.modules.chronic.query.GuideQuery;
import com.chronic.modules.chronic.service.CdGuideService;
import com.chronic.modules.system.entity.SysDictData;
import com.chronic.modules.system.entity.SysUser;
import com.chronic.modules.system.mapper.SysDictDataMapper;
import com.chronic.modules.system.mapper.SysUserMapper;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class CdGuideServiceImpl extends ServiceImpl<CdGuideMapper, CdGuide> implements CdGuideService {

    /** 指导类型字典标识 */
    public static final String DICT_GUIDE_TYPE = "guide_type";

    private final SysDictDataMapper dictDataMapper;
    private final SysUserMapper userMapper;

    @Override
    public PageResult<CdGuide> queryPage(GuideQuery query) {
        LambdaQueryWrapper<CdGuide> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdGuide::getPatientId, query.getPatientId())
         .eq(query.getWarningId() != null, CdGuide::getWarningId, query.getWarningId())
         .eq(StringUtils.hasText(query.getGuideType()), CdGuide::getGuideType, query.getGuideType())
         .eq(StringUtils.hasText(query.getReadStatus()), CdGuide::getReadStatus, query.getReadStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdGuide::getContent, query.getKeyword())
         .orderByDesc(CdGuide::getGuideTime);
        Page<CdGuide> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public void addGuide(CdGuide guide) {
        if (guide.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        if (!StringUtils.hasText(guide.getContent())) {
            throw new ServiceException("指导内容不能为空");
        }
        guide.setGuideId(null);
        guide.setDoctorId(SecurityUtils.getUserId());
        guide.setDoctorName(SecurityUtils.getLoginUser().getRealName());
        guide.setGuideTime(LocalDateTime.now());
        if (!StringUtils.hasText(guide.getReadStatus())) {
            guide.setReadStatus("0");
        }
        save(guide);
    }

    @Override
    public void removeGuide(Long guideId) {
        removeById(guideId);
    }

    @Override
    public void markRead(Long guideId) {
        CdGuide guide = getById(guideId);
        if (guide == null) {
            throw new ServiceException("指导不存在");
        }
        guide.setReadStatus("1");
        updateById(guide);
    }

    @Override
    public List<Map<String, String>> typeOptions() {
        List<SysDictData> list = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, DICT_GUIDE_TYPE)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort));
        List<Map<String, String>> result = new ArrayList<>();
        for (SysDictData d : list) {
            Map<String, String> m = new LinkedHashMap<>();
            m.put("value", d.getDictValue());
            m.put("label", d.getDictLabel());
            result.add(m);
        }
        return result;
    }

    @Override
    public void addType(String label) {
        checkTypeManagePermission();
        if (!StringUtils.hasText(label)) {
            throw new ServiceException("指导类型名称不能为空");
        }
        String name = label.trim();
        Long exists = dictDataMapper.selectCount(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, DICT_GUIDE_TYPE)
                .eq(SysDictData::getDictLabel, name));
        if (exists != null && exists > 0) {
            throw new ServiceException("该指导类型已存在");
        }
        List<SysDictData> all = dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, DICT_GUIDE_TYPE));
        int maxSort = all.stream().map(SysDictData::getDictSort)
                .filter(Objects::nonNull).max(Integer::compareTo).orElse(0);
        SysDictData data = new SysDictData();
        data.setDictType(DICT_GUIDE_TYPE);
        data.setDictLabel(name);
        data.setDictValue(String.valueOf(nextTypeValue(all)));
        data.setDictSort(maxSort + 1);
        data.setIsDefault("N");
        data.setStatus("0");
        dictDataMapper.insert(data);
    }
    @Override
    public void deleteType(String label){
        dictDataMapper.deleteByLabel(label);
    }

    private void checkTypeManagePermission() {
        Long userId = SecurityUtils.getUserId();
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException(401, "登录用户不存在");
        }
        if (!"1".equals(user.getUserType()) && !"2".equals(user.getUserType())) {
            throw new ServiceException(403, "仅医生或超级管理员可新增指导类型");
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException(403, "账号已被停用，无法新增指导类型");
        }
    }

    private int nextTypeValue(List<SysDictData> all) {
        int max = 0;
        for (SysDictData d : all) {
            try {
                max = Math.max(max, Integer.parseInt(d.getDictValue()));
            } catch (Exception ignored) {
            }
        }
        return max + 1;
    }
}
