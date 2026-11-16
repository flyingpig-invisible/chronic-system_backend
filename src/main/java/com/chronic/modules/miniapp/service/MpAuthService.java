package com.chronic.modules.miniapp.service;

import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.vo.MpLoginVO;

public interface MpAuthService {

    MpLoginVO register(MpRegisterDTO dto);

    MpLoginVO login(MpLoginDTO dto);

    MpUser currentUser();
}
