package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysDictData;
import com.chronic.modules.system.entity.SysDictType;

import java.util.List;

public interface SysDictService extends IService<SysDictType> {

    PageResult<SysDictType> typePage(Integer pageNum, Integer pageSize, String dictName);

    List<SysDictType> typeList();

    void addType(SysDictType type);

    void updateType(SysDictType type);

    void removeType(Long dictId);

    List<SysDictData> dataByType(String dictType);

    void addData(SysDictData data);

    void updateData(SysDictData data);

    void removeData(Long dictCode);
}
