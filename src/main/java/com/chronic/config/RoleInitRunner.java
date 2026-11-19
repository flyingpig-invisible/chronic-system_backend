package com.chronic.config;

import com.chronic.modules.system.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时补齐内置角色（超级管理员 / 医生 / 家属）
 * 内置角色是权限判定的基准，必须始终存在，且不通过界面删除
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RoleInitRunner implements ApplicationRunner {

    private final SysRoleService roleService;

    @Override
    public void run(ApplicationArguments args) {
        int created = roleService.ensureBuiltInRoles();
        if (created > 0) {
            log.info("已补齐 {} 个内置角色", created);
        }
    }
}