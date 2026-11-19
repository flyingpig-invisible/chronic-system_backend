package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.system.entity.SysRole;
import com.chronic.modules.system.mapper.SysRoleMapper;
import com.chronic.modules.system.service.SysRoleService;
import com.chronic.security.LoginUser;
import com.chronic.security.PermissionService;
import com.chronic.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    /** 内置角色权限字符：不可删除 */
    public static final Set<String> BUILT_IN_ROLE_KEYS = Set.of("admin", "doctor", "family");

    private static final Pattern ROLE_KEY_PATTERN = Pattern.compile("^[A-Za-z][A-Za-z0-9_]{0,31}$");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PermissionService permissionService;

    @Override
    public PageResult<SysRole> queryPage(Integer pageNum, Integer pageSize, String roleName) {
        LambdaQueryWrapper<SysRole> w = new LambdaQueryWrapper<>();
        w.like(StringUtils.hasText(roleName), SysRole::getRoleName, roleName)
         .orderByAsc(SysRole::getOrderNum)
         .orderByAsc(SysRole::getRoleId);
        Page<SysRole> page = page(new Page<>(pageNum, pageSize), w);
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    @Override
    public SysRole detail(Long roleId) {
        return getById(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addRole(SysRole role) {
        requireSysAdmin();
        normalize(role);
        if (!StringUtils.hasText(role.getRoleName())) {
            throw new ServiceException("角色名称不能为空");
        }
        if (!StringUtils.hasText(role.getRoleKey())) {
            throw new ServiceException("权限字符不能为空");
        }
        if (!ROLE_KEY_PATTERN.matcher(role.getRoleKey()).matches()) {
            throw new ServiceException("权限字符需以字母开头，仅可包含字母、数字和下划线，长度不超过32位");
        }
        checkNameUnique(role.getRoleName(), null);
        checkKeyUnique(role.getRoleKey(), null);
        if (!StringUtils.hasText(role.getStatus())) {
            role.setStatus("0");
        }
        if (role.getOrderNum() == null) {
            role.setOrderNum(0);
        }
        save(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRole(SysRole role) {
        if (role == null || role.getRoleId() == null) {
            throw new ServiceException("角色ID不能为空");
        }
        SysRole exists = getById(role.getRoleId());
        if (exists == null) {
            throw new ServiceException("角色不存在");
        }
        normalize(role);
        if (StringUtils.hasText(role.getRoleName())) {
            checkNameUnique(role.getRoleName(), role.getRoleId());
        }
        if (StringUtils.hasText(role.getRoleKey())) {
            if (!ROLE_KEY_PATTERN.matcher(role.getRoleKey()).matches()) {
                throw new ServiceException("权限字符需以字母开头，仅可包含字母、数字和下划线，长度不超过32位");
            }
            // 内置角色的权限字符是代码里识别的依据，不允许改动
            if (BUILT_IN_ROLE_KEYS.contains(exists.getRoleKey())
                    && !exists.getRoleKey().equals(role.getRoleKey())) {
                throw new ServiceException("内置角色的权限字符不可修改");
            }
            checkKeyUnique(role.getRoleKey(), role.getRoleId());
        }
        updateById(role);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeRole(Long roleId) {
        if (roleId == null) {
            throw new ServiceException("角色ID不能为空");
        }
        SysRole role = getById(roleId);
        if (role == null) {
            throw new ServiceException("角色不存在");
        }
        if (BUILT_IN_ROLE_KEYS.contains(role.getRoleKey())) {
            throw new ServiceException("角色「" + role.getRoleName() + "」为系统内置角色，不允许删除");
        }
        Long used = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ?", Long.class, roleId);
        if (used != null && used > 0) {
            throw new ServiceException("该角色下仍有 " + used + " 个用户，请先调整用户角色后再删除");
        }
        removeById(roleId);
        baseMapper.deleteRoleMenus(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setMenuIds(Long roleId, List<Long> menuIds) {
        // 只有系统管理员可以给角色分配权限
        requireSysAdminForAuth();
        if (roleId == null || getById(roleId) == null) {
            throw new ServiceException("角色不存在");
        }
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE role_id = ?", roleId);
        if (menuIds != null) {
            for (Long menuId : menuIds) {
                jdbcTemplate.update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", roleId, menuId);
            }
        }
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return jdbcTemplate.queryForList("SELECT menu_id FROM sys_role_menu WHERE role_id = ?", Long.class, roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int ensureBuiltInRoles() {
        int created = 0;
        created += ensureRole("超级管理员", "admin", 1, "超级管理员，拥有全部权限");
        created += ensureRole("医生", "doctor", 2, "慢病管理医生");
        created += ensureRole("家属", "family", 3, "患者家属，查看绑定患者健康数据");
        return created;
    }

    private int ensureRole(String name, String key, int orderNum, String remark) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, key));
        if (count != null && count > 0) {
            return 0;
        }
        SysRole role = new SysRole();
        role.setRoleName(name);
        role.setRoleKey(key);
        role.setOrderNum(orderNum);
        role.setStatus("0");
        role.setRemark(remark);
        save(role);
        return 1;
    }

    /** 只有系统管理员可以增删角色 */
    /** 校验系统管理员：用于角色权限分配等高危操作 */
    private void requireSysAdminForAuth() {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (user == null || !permissionService.isSysAdmin(user)) {
            throw new ServiceException(403, "只有系统管理员可以分配角色权限");
        }
    }

    private void requireSysAdmin() {
        LoginUser user = SecurityUtils.getLoginUserOrNull();
        if (!permissionService.isSysAdmin(user)) {
            throw new ServiceException(403, "只有系统管理员可以新增角色");
        }
    }

    private void normalize(SysRole role) {
        if (role == null) {
            throw new ServiceException("角色信息不能为空");
        }
        if (StringUtils.hasText(role.getRoleName())) {
            role.setRoleName(role.getRoleName().trim());
        }
        if (StringUtils.hasText(role.getRoleKey())) {
            role.setRoleKey(role.getRoleKey().trim());
        }
        if (StringUtils.hasText(role.getRemark())) {
            role.setRemark(role.getRemark().trim());
        }
    }

    private void checkNameUnique(String roleName, Long selfId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleName, roleName)
                .ne(selfId != null, SysRole::getRoleId, selfId));
        if (count != null && count > 0) {
            throw new ServiceException("角色名称「" + roleName + "」已存在");
        }
    }

    private void checkKeyUnique(String roleKey, Long selfId) {
        Long count = baseMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleKey, roleKey)
                .ne(selfId != null, SysRole::getRoleId, selfId));
        if (count != null && count > 0) {
            throw new ServiceException("权限字符「" + roleKey + "」已存在");
        }
    }
}