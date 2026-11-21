package com.chronic.modules.miniapp.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.chronic.common.exception.ServiceException;
import com.chronic.modules.chronic.service.CdPatientDeptService;
import com.chronic.modules.miniapp.service.MpAccessService;
import com.chronic.modules.miniapp.service.MpDeptService;
import com.chronic.modules.miniapp.vo.MpDeptVO;
import com.chronic.modules.system.entity.SysDept;
import com.chronic.modules.system.mapper.SysDeptMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 小程序科室服务实现
 *
 * @author chronic
 */
@Service
@RequiredArgsConstructor
public class MpDeptServiceImpl implements MpDeptService {

    private final SysDeptMapper deptMapper;
    private final CdPatientDeptService patientDeptService;
    private final MpAccessService accessService;

    @Override
    public List<MpDeptVO> options() {
        List<MpDeptVO> list = new ArrayList<>();
        for (SysDept dept : deptMapper.selectList(Wrappers.<SysDept>lambdaQuery()
                .eq(SysDept::getStatus, "0")
                .orderByAsc(SysDept::getOrderNum)
                .orderByAsc(SysDept::getDeptId))) {
            MpDeptVO vo = new MpDeptVO();
            vo.setDeptId(dept.getDeptId());
            vo.setDeptName(dept.getDeptName());
            list.add(vo);
        }
        return list;
    }

    @Override
    public List<Long> selectedDeptIds(Long patientId) {
        return patientDeptService.deptIdsOf(accessService.resolvePatientId(patientId));
    }

    @Override
    public void setSelectedDeptIds(Long patientId, List<Long> deptIds) {
        Long id = accessService.resolvePatientId(patientId);
        // 家属只能查看，不能替患者修改可见科室
        if (accessService.isFamily()) {
            throw new ServiceException(403, "家属账号不能修改患者的科室可见范围");
        }
        if (deptIds == null || deptIds.stream().allMatch(java.util.Objects::isNull)) {
            throw new ServiceException("请至少选择一个科室");
        }
        patientDeptService.replace(id, deptIds.stream().filter(java.util.Objects::nonNull).collect(Collectors.toList()), "1");
    }
}