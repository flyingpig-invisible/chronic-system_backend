package com.chronic.modules.auth.service;

import com.chronic.modules.auth.dto.LoginDTO;
import com.chronic.modules.auth.vo.LoginVO;
import com.chronic.modules.auth.vo.UserInfoVO;

/**
 * 认证服务
 *
 * @author chronic
 */
public interface AuthService {

    /** 系统用户登录（医生端 Web） */
    LoginVO login(LoginDTO dto);

    /** 获取当前登录用户信息 */
    UserInfoVO getUserInfo();

    /** 退出登录 */
    void logout();
}
