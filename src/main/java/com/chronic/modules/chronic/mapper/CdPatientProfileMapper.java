package com.chronic.modules.chronic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.chronic.entity.CdPatientProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * 患者专属档案 Mapper
 *
 * @author chronic
 */
@Mapper
public interface CdPatientProfileMapper extends BaseMapper<CdPatientProfile> {
}
