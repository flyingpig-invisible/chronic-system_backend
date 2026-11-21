package com.chronic.modules.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.miniapp.entity.MpFamilyPatient;
import org.apache.ibatis.annotations.Mapper;

/**
 * 家属-患者关联 Mapper
 *
 * @author chronic
 */
@Mapper
public interface MpFamilyPatientMapper extends BaseMapper<MpFamilyPatient> {
}