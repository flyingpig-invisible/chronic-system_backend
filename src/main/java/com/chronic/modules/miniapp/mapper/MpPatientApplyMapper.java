package com.chronic.modules.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.miniapp.entity.MpPatientApply;
import org.apache.ibatis.annotations.Mapper;

/**
 * 患者端建档申请
 *
 * @author chronic
 */
@Mapper
public interface MpPatientApplyMapper extends BaseMapper<MpPatientApply> {
}