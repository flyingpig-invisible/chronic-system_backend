package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysDictData;
import com.chronic.modules.system.entity.SysDictType;
import com.chronic.modules.system.mapper.SysDictDataMapper;
import com.chronic.modules.system.mapper.SysDictTypeMapper;
import com.chronic.modules.system.service.SysDictService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class SysDictServiceImpl extends ServiceImpl<SysDictTypeMapper, SysDictType> implements SysDictService {

    @Autowired
    private SysDictDataMapper dictDataMapper;

    @Override
    public PageResult<SysDictType> typePage(Integer pageNum, Integer pageSize, String dictName) {
        LambdaQueryWrapper<SysDictType> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(dictName), SysDictType::getDictName, dictName)
         .orderByDesc(SysDictType::getDictId);
        Page<SysDictType> page = page(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public List<SysDictType> typeList() {
        return list(new LambdaQueryWrapper<SysDictType>().eq(SysDictType::getStatus, "0"));
    }

    @Override
    public void addType(SysDictType type) {
        if (!StringUtils.hasText(type.getDictType())) {
            throw new ServiceException("字典类型不能为空");
        }
        if (!StringUtils.hasText(type.getStatus())) {
            type.setStatus("0");
        }
        save(type);
    }

    @Override
    public void updateType(SysDictType type) {
        updateById(type);
    }

    @Override
    public void removeType(Long dictId) {
        removeById(dictId);
    }

    @Override
    public List<SysDictData> dataByType(String dictType) {
        return dictDataMapper.selectList(new LambdaQueryWrapper<SysDictData>()
                .eq(SysDictData::getDictType, dictType)
                .eq(SysDictData::getStatus, "0")
                .orderByAsc(SysDictData::getDictSort));
    }

    @Override
    public void addData(SysDictData data) {
        if (!StringUtils.hasText(data.getDictType()) || !StringUtils.hasText(data.getDictValue())) {
            throw new ServiceException("字典类型与键值不能为空");
        }
        dictDataMapper.insert(data);
    }

    @Override
    public void updateData(SysDictData data) {
        dictDataMapper.updateById(data);
    }

    @Override
    public void removeData(Long dictCode) {
        dictDataMapper.deleteById(dictCode);
    }
}
