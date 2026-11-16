package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.modules.system.entity.SysMenu;

import java.util.List;
import java.util.Map;

public interface SysMenuService extends IService<SysMenu> {

    List<SysMenu> tree();

    List<Map<String, Object>> routeTree();

    void addMenu(SysMenu menu);

    void updateMenu(SysMenu menu);

    void removeMenu(Long menuId);
}
