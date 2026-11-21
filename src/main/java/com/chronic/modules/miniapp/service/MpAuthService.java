package com.chronic.modules.miniapp.service;

import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.dto.MpResetDTO;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import com.chronic.modules.miniapp.vo.MpUserVO;

/**
 * 小程序认证服务
 *
 * @author chronic
 */
public interface MpAuthService {

    MpLoginVO login(MpLoginDTO dto);

    MpLoginVO register(MpRegisterDTO dto);

    void resetPassword(MpResetDTO dto);

    MpUserVO currentUser();

    MpUserVO updateProfile(String realName, String avatar, String gender);
}