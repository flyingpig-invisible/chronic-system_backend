package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.utils.JwtUtils;
import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.miniapp.mapper.MpUserMapper;
import com.chronic.modules.miniapp.service.MpAuthService;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 小程序认证服务
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class MpAuthServiceImpl extends ServiceImpl<MpUserMapper, MpUser> implements MpAuthService {

    private final MpUserMapper userMapper;
    private final CdPatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Override
    public MpLoginVO register(MpRegisterDTO dto) {
        Long exists = userMapper.selectCount(new LambdaQueryWrapper<MpUser>()
                .eq(MpUser::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new ServiceException("用户名已存在");
        }
        MpUser user = new MpUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(dto.getRealName());
        user.setPhone(dto.getPhone());
        // 角色：1 患者 / 2 家属，默认家属
        String role = StringUtils.hasText(dto.getUserRole()) ? dto.getUserRole() : "2";
        if (!"1".equals(role) && !"2".equals(role)) {
            role = "2";
        }
        user.setUserRole(role);
        user.setStatus("0");
        userMapper.insert(user);

        // 患者注册：自动匹配同名患者档案并建立关联
        if ("1".equals(role)) {
            bindPatientArchive(user);
        }
        return buildLoginVO(user);
    }

    /**
     * 患者注册后，按姓名 + 手机号匹配已有档案，建立 mp_user_id 关联。
     * 匹配不到时不报错，等待医生建档后再关联。
     */
    private void bindPatientArchive(MpUser user) {
        if (!StringUtils.hasText(user.getRealName())) {
            return;
        }
        LambdaQueryWrapper<CdPatient> w = new LambdaQueryWrapper<CdPatient>()
                .eq(CdPatient::getName, user.getRealName())
                .isNull(CdPatient::getMpUserId);
        if (StringUtils.hasText(user.getPhone())) {
            w.eq(CdPatient::getPhone, user.getPhone());
        }
        w.last("limit 1");
        CdPatient patient = patientMapper.selectOne(w);
        if (patient == null) {
            return;
        }
        patient.setMpUserId(user.getUserId());
        patientMapper.updateById(patient);
    }

    @Override
    public MpLoginVO login(MpLoginDTO dto) {
        MpUser user = userMapper.selectOne(new LambdaQueryWrapper<MpUser>()
                .eq(MpUser::getUsername, dto.getUsername()));
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException("账号已被停用");
        }
        return buildLoginVO(user);
    }

    @Override
    public MpUser currentUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return userMapper.selectById(loginUser.getUserId());
    }

    private MpLoginVO buildLoginVO(MpUser user) {
        String role = user.getUserRole();
        String token = jwtUtils.createToken(user.getUserId(), user.getUsername(), "mp",
                user.getRealName(), role);
        MpLoginVO vo = new MpLoginVO();
        vo.setToken(token);
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setUserRole(role);
        vo.setUserRoleName("2".equals(role) ? "家属" : "患者");
        return vo;
    }
}
