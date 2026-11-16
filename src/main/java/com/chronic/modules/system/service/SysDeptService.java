package com.chronic.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.modules.system.entity.SysDept;

import java.util.List;

public interface SysDeptService extends IService<SysDept> {

    List<SysDept> tree();

    void addDept(SysDept dept);

    void updateDept(SysDept dept);

    void removeDept(Long deptId);
}
