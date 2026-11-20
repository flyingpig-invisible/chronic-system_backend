package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysIndicator;
import com.chronic.modules.system.mapper.SysIndicatorMapper;
import com.chronic.modules.system.query.IndicatorQuery;
import com.chronic.modules.system.service.SysIndicatorService;
import com.chronic.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 系统指标字典服务
 *
 * @author chronic
 */
@Service
public class SysIndicatorServiceImpl extends ServiceImpl<SysIndicatorMapper, SysIndicator>
        implements SysIndicatorService {

    /** 指标编码：字母开头，仅允许字母、数字、下划线 */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,31}$");

    @Override
    public PageResult<SysIndicator> pageIndicator(IndicatorQuery query) {
        LambdaQueryWrapper<SysIndicator> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(SysIndicator::getIndicatorName, keyword)
                    .or().like(SysIndicator::getIndicatorCode, keyword));
        }
        wrapper.eq(StringUtils.hasText(query.getStatus()), SysIndicator::getStatus, query.getStatus())
               .orderByAsc(SysIndicator::getSort)
               .orderByAsc(SysIndicator::getIndicatorId);
        Page<SysIndicator> page = page(new Page<>(query.getPageNum(), query.getPageSize()), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysIndicator> listEnabled() {
        return list(new LambdaQueryWrapper<SysIndicator>()
                .eq(SysIndicator::getStatus, "0")
                .orderByAsc(SysIndicator::getSort)
                .orderByAsc(SysIndicator::getIndicatorId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addIndicator(SysIndicator indicator) {
        normalizeAndValidate(indicator);
        checkCodeUnique(indicator.getIndicatorCode(), null);
        indicator.setCreateBy(SecurityUtils.getUsername());
        indicator.setUpdateBy(SecurityUtils.getUsername());
        save(indicator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateIndicator(SysIndicator indicator) {
        if (indicator.getIndicatorId() == null) {
            throw new ServiceException("指标ID不能为空");
        }
        SysIndicator exists = getById(indicator.getIndicatorId());
        if (exists == null) {
            throw new ServiceException("指标不存在");
        }
        normalizeAndValidate(indicator);
        checkCodeUnique(indicator.getIndicatorCode(), indicator.getIndicatorId());
        indicator.setUpdateBy(SecurityUtils.getUsername());
        updateById(indicator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeIndicators(Long[] indicatorIds) {
        if (indicatorIds == null || indicatorIds.length == 0) {
            throw new ServiceException("请选择要删除的指标");
        }
        removeByIds(Arrays.asList(indicatorIds));
    }

    /** 去空格 + 校验必填与格式，补齐默认值 */
    private void normalizeAndValidate(SysIndicator indicator) {
        if (indicator == null) {
            throw new ServiceException("指标信息不能为空");
        }
        if (!StringUtils.hasText(indicator.getIndicatorName())) {
            throw new ServiceException("指标名称不能为空");
        }
        indicator.setIndicatorName(indicator.getIndicatorName().trim());

        if (!StringUtils.hasText(indicator.getIndicatorCode())) {
            throw new ServiceException("指标编码不能为空");
        }
        indicator.setIndicatorCode(indicator.getIndicatorCode().trim());
        if (!CODE_PATTERN.matcher(indicator.getIndicatorCode()).matches()) {
            throw new ServiceException("指标编码需以字母开头，仅可包含字母、数字和下划线，长度不超过32位");
        }

        indicator.setUnit(StringUtils.hasText(indicator.getUnit()) ? indicator.getUnit().trim() : "");
        if (indicator.getSort() == null) {
            indicator.setSort(0);
        }
        if (!StringUtils.hasText(indicator.getStatus())) {
            indicator.setStatus("0");
        }
    }

    /** 指标编码全局唯一（编辑时排除自身） */
    private void checkCodeUnique(String indicatorCode, Long selfId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysIndicator>()
                .eq(SysIndicator::getIndicatorCode, indicatorCode)
                .ne(selfId != null, SysIndicator::getIndicatorId, selfId));
        if (count != null && count > 0) {
            throw new ServiceException("指标编码[" + indicatorCode + "]已存在");
        }
    }
}