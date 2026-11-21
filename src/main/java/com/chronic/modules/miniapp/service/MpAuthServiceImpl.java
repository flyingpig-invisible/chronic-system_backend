package com.chronic.modules.miniapp.service.impl;

import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.utils.JwtUtils;
import com.chronic.common.utils.WxAuthHelper;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.miniapp.mapper.MpPatientMapper;
import com.chronic.modules.miniapp.dto.MpLoginDTO;
import com.chronic.modules.miniapp.dto.MpRegisterDTO;
import com.chronic.modules.miniapp.dto.MpResetDTO;
import com.chronic.modules.miniapp.entity.MpUser;
import com.chronic.modules.miniapp.mapper.MpUserMapper;
import com.chronic.modules.miniapp.service.MpAuthService;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import com.chronic.modules.miniapp.vo.MpUserVO;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 小程序认证服务实现
 * <p>
 * token 中 userType=mp，userId 为 mp_user.user_id；
 * 患者数据通过 cd_patient.mp_user_id 与医生端共用同一份档案。
 *
 * @author chronic
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MpAuthServiceImpl implements MpAuthService {

    private final MpUserMapper userMapper;
    private final MpPatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final WxAuthHelper wxAuthHelper;

    @Override
    public MpLoginVO login(MpLoginDTO dto) {
        MpUser user = null;
        if (StringUtils.hasText(dto.getCode()) && wxAuthHelper.enabled()) {
            user = loginByWxCode(dto.getCode());
        }
        if (user == null) {
            String account = dto.getUsername() == null ? "" : dto.getUsername().trim();
            if (!StringUtils.hasText(account)) {
                throw new ServiceException("请输入账号");
            }
            user = userMapper.selectOne(Wrappers.<MpUser>lambdaQuery()
                    .and(w -> w.eq(MpUser::getPhone, account).or().eq(MpUser::getUsername, account))
                    .last("limit 1"));
            if (user == null || !passwordEncoder.matches(dto.getPassword() == null ? "" : dto.getPassword(), user.getPassword())) {
                throw new ServiceException("账号或密码错误");
            }
        }
        if ("1".equals(user.getStatus())) {
            throw new ServiceException("账号已被停用，请联系医生");
        }
        return buildLogin(user);
    }

    private MpUser loginByWxCode(String code) {
        JSONObject session = wxAuthHelper.code2Session(code);
        if (session == null) {
            return null;
        }
        String openid = session.getStr("openid");
        if (!StringUtils.hasText(openid)) {
            return null;
        }
        MpUser user = userMapper.selectOne(Wrappers.<MpUser>lambdaQuery()
                .eq(MpUser::getOpenid, openid).last("limit 1"));
        if (user == null) {
            return null;
        }
        return user;
    }

    @Override
    public MpLoginVO register(MpRegisterDTO dto) {
        String phone = dto.getPhone().trim();
        Long exists = userMapper.selectCount(Wrappers.<MpUser>lambdaQuery().eq(MpUser::getPhone, phone));
        if (exists != null && exists > 0) {
            throw new ServiceException("该手机号已注册，请直接登录（账号为手机号）");
        }
        String role = "2".equals(dto.getUserRole()) ? "2" : "1";
        MpUser user = new MpUser();
        user.setUsername(phone);
        user.setPhone(phone);
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRealName(StringUtils.hasText(dto.getRealName()) ? dto.getRealName().trim() : "用户" + phone.substring(7));
        user.setGender(StringUtils.hasText(dto.getGender()) ? dto.getGender() : "2");
        user.setUserRole(role);
        user.setStatus("0");
        user.setCreateTime(LocalDateTime.now());
        userMapper.insert(user);

        // 患者注册：医生端已建档案且未关联小程序用户时，按手机号/患者编号自动对齐
        if ("1".equals(role)) {
            CdPatient patient = null;
            if (StringUtils.hasText(dto.getPatientNo())) {
                patient = patientMapper.selectByPatientNo(dto.getPatientNo().trim());
            }
            if (patient == null) {
                patient = patientMapper.selectUnlinkedByPhone(phone);
            }
            if (patient != null && (patient.getMpUserId() == null || patient.getMpUserId() == 0L)) {
                patient.setMpUserId(user.getUserId());
                patientMapper.linkUser(patient.getPatientId(), user.getUserId());
            }
        }
        return buildLogin(user);
    }

    @Override
    public void resetPassword(MpResetDTO dto) {
        MpUser user = userMapper.selectOne(Wrappers.<MpUser>lambdaQuery()
                .eq(MpUser::getPhone, dto.getPhone().trim()).last("limit 1"));
        if (user == null) {
            throw new ServiceException("该手机号未注册，请先注册");
        }
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public MpUserVO currentUser() {
        MpUser user = currentMpUser();
        return toVO(user);
    }

    @Override
    public MpUserVO updateProfile(String realName, String avatar, String gender) {
        MpUser user = currentMpUser();
        if (StringUtils.hasText(realName)) {
            user.setRealName(realName.trim());
        }
        if (StringUtils.hasText(avatar)) {
            user.setAvatar(avatar);
        }
        if (StringUtils.hasText(gender)) {
            user.setGender(gender);
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return toVO(user);
    }

    private MpUser currentMpUser() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (!"mp".equals(loginUser.getUserType())) {
            throw new ServiceException(403, "请使用小程序账号登录");
        }
        MpUser user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new ServiceException(401, "登录已失效，请重新登录");
        }
        return user;
    }

    private MpLoginVO buildLogin(MpUser user) {
        String token = jwtUtils.createToken(user.getUserId(), user.getUsername(), "mp",
                user.getRealName(), user.getUserRole(), null);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        MpLoginVO vo = new MpLoginVO();
        vo.setToken(token);
        vo.setUser(toVO(user));
        return vo;
    }

    private MpUserVO toVO(MpUser user) {
        MpUserVO vo = new MpUserVO();
        vo.setUserId(user.getUserId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setUserRole(user.getUserRole());
        vo.setUserRoleName("2".equals(user.getUserRole()) ? "家属" : "患者");
        CdPatient patient = patientMapper.selectByMpUserId(user.getUserId());
        if (patient != null) {
            vo.setPatientId(patient.getPatientId());
            vo.setPatientNo(patient.getPatientNo());
            vo.setPatientLinked(true);
        } else {
            vo.setPatientLinked(false);
        }
        return vo;
    }
}
