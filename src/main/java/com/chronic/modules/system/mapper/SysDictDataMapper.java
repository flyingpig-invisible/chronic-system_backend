package com.chronic.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.system.entity.SysDictData;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

/**
 * SysDictData Mapper
 *
 * @author chronic
 */
@Mapper
public interface SysDictDataMapper extends BaseMapper<SysDictData> {
    @Delete("DELETE FROM sys_dict_data WHERE dict_label = #{label}")
    void deleteByLabel(String label);
}
