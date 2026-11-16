package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.dto.ClinicFinishDTO;
import com.chronic.modules.chronic.entity.CdClinic;
import com.chronic.modules.chronic.query.ClinicQuery;

public interface CdClinicService extends IService<CdClinic> {

    PageResult<CdClinic> queryPage(ClinicQuery query);

    void book(CdClinic clinic);

    void accept(Long visitId);

    void finish(Long visitId, ClinicFinishDTO dto);

    void cancel(Long visitId);
}
