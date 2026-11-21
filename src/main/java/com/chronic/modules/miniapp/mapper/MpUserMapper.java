package com.chronic.modules.miniapp.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.miniapp.entity.MpUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 小程序用户 Mapper
 *
 * @author chronic
 */
@Mapper
public interface MpUserMapper extends BaseMapper<MpUser> {
}