package com.chronic.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * SysRole Mapper
 *
 * @author chronic
 */
@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /** 查询用户通过角色获得的全部菜单权限标识（去重、排除空值） */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0' AND r.status = '0'
            INNER JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            INNER JOIN sys_menu m ON m.menu_id = rm.menu_id AND m.status = '0'
            WHERE ur.user_id = #{userId} AND m.perms IS NOT NULL AND m.perms <> ''
            """)
    List<String> selectPermissionsByUserId(@Param("userId") Long userId);

    /** 查询用户的角色权限字符（仅正常状态角色） */
    @Select("""
            SELECT DISTINCT r.role_key
            FROM sys_user_role ur
            INNER JOIN sys_role r ON r.role_id = ur.role_id AND r.del_flag = '0' AND r.status = '0'
            WHERE ur.user_id = #{userId} AND r.role_key IS NOT NULL AND r.role_key <> ''
            """)
    List<String> selectRoleKeysByUserId(@Param("userId") Long userId);

    /** 删除角色的菜单授权 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    void deleteRoleMenus(@Param("roleId") Long roleId);
}