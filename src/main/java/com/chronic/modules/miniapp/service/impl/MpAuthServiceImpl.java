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
import com.chronic.modules.miniapp.service.MpPatientApplyService;
import com.chronic.modules.miniapp.vo.MpLoginVO;
import com.chronic.modules.miniapp.vo.MpUserVO;
import com.chronic.security.LoginUser;
import com.chronic.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

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

    /** 用户名：字母或数字开头，仅允许字母、数字、下划线 */
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_]{3,19}$");

    /** 纯手机号：用户名不允许与手机号同形，避免与手机号登录混淆 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1\\d{10}$");

    private final MpUserMapper userMapper;
    private final MpPatientMapper patientMapper;
    private final PasswordEncoder passwordEncoder;
    private final MpPatientApplyService applyService;
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

    /**
     * 注册时选中的可见科室暂存在 mp_user 上：此时患者还没有 cd_patient 档案，
     * 由医生审核建档通过时写入 cd_patient_dept（注册 → 申请建档 → 审核）。
     */
    @Transactional(rollbackFor = Exception.class)
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
        if ("1".equals(role) && dto.getDeptIds() != null && !dto.getDeptIds().isEmpty()) {
            // 科室直接落库：记在该账号的建档申请上，医生审核建档时写入 cd_patient_dept
            applyService.saveRegisterDepts(user, dto.getDeptIds());
        }

        // 患者注册后不再按手机号自动认领医生端档案，统一由医生端审核建档申请（mp_patient_apply）
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public MpUserVO updateUsername(String username) {
        MpUser user = currentMpUser();
        if (!StringUtils.hasText(username)) {
            throw new ServiceException("用户名不能为空");
        }
        String value = username.trim();
        if (value.length() < 4 || value.length() > 20) {
            throw new ServiceException("用户名长度需为 4~20 个字符");
        }
        if (!USERNAME_PATTERN.matcher(value).matches()) {
            throw new ServiceException("用户名只能包含字母、数字和下划线，且需以字母或数字开头");
        }
        if (PHONE_PATTERN.matcher(value).matches()) {
            throw new ServiceException("用户名不能是纯手机号");
        }
        if (value.equalsIgnoreCase(user.getUsername())) {
            return toVO(user);
        }
        // 用户名与手机号都能登录，因此两者之间也不能撞号
        Long exists = userMapper.selectCount(Wrappers.<MpUser>lambdaQuery()
                .ne(MpUser::getUserId, user.getUserId())
                .and(w -> w.eq(MpUser::getUsername, value).or().eq(MpUser::getPhone, value)));
        if (exists != null && exists > 0) {
            throw new ServiceException("用户名已被占用，请更换");
        }
        user.setUsername(value);
        user.setUpdateTime(LocalDateTime.now());
        try {
            userMapper.updateById(user);
        } catch (DuplicateKeyException e) {
            // 并发修改时由唯一索引兜底
            throw new ServiceException("用户名已被占用，请更换");
        }
        log.info("小程序用户 {} 修改用户名 {} -> {}", user.getUserId(), user.getPhone(), value);
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
