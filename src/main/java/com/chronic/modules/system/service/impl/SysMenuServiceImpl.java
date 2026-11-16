package com.chronic.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.system.entity.SysMenu;
import com.chronic.modules.system.mapper.SysMenuMapper;
import com.chronic.modules.system.service.SysMenuService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public List<SysMenu> tree() {
        List<SysMenu> all = list(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getParentId).orderByAsc(SysMenu::getOrderNum));
        return buildTree(all, 0L);
    }

    private List<SysMenu> buildTree(List<SysMenu> all, Long parentId) {
        List<SysMenu> result = new ArrayList<>();
        for (SysMenu m : all) {
            Long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (pid.equals(parentId)) {
                m.setChildren(buildTree(all, m.getMenuId()));
                result.add(m);
            }
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> routeTree() {
        List<SysMenu> all = list(new LambdaQueryWrapper<SysMenu>()
                .in(SysMenu::getMenuType, "M", "C")
                .eq(SysMenu::getStatus, "0")
                .orderByAsc(SysMenu::getOrderNum));
        return buildRoute(all, 0L);
    }

    private List<Map<String, Object>> buildRoute(List<SysMenu> all, Long parentId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysMenu m : all) {
            Long pid = m.getParentId() == null ? 0L : m.getParentId();
            if (!pid.equals(parentId)) {
                continue;
            }
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("menuId", m.getMenuId());
            node.put("name", m.getMenuName());
            node.put("path", m.getPath());
            node.put("component", m.getComponent());
            node.put("icon", m.getIcon());
            node.put("visible", m.getVisible());
            node.put("menuType", m.getMenuType());
            node.put("children", buildRoute(all, m.getMenuId()));
            result.add(node);
        }
        return result;
    }

    @Override
    public void addMenu(SysMenu menu) {
        if (!StringUtils.hasText(menu.getMenuName())) {
            throw new ServiceException("菜单名称不能为空");
        }
        if (menu.getParentId() == null) {
            menu.setParentId(0L);
        }
        if (!StringUtils.hasText(menu.getStatus())) {
            menu.setStatus("0");
        }
        save(menu);
    }

    @Override
    public void updateMenu(SysMenu menu) {
        updateById(menu);
    }

    @Override
    public void removeMenu(Long menuId) {
        Long children = count(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getParentId, menuId));
        if (children != null && children > 0) {
            throw new ServiceException("存在子菜单，无法删除");
        }
        removeById(menuId);
        jdbcTemplate.update("DELETE FROM sys_role_menu WHERE menu_id = ?", menuId);
    }
}
