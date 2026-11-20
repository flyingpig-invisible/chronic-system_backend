package com.chronic.modules.chronic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.chronic.entity.CdImportLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 数据导入日志 Mapper
 *
 * @author chronic
 */
@Mapper
public interface CdImportLogMapper extends BaseMapper<CdImportLog> {
}
