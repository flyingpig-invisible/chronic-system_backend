package com.chronic.modules.chronic.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdRemindLog;
import com.chronic.modules.chronic.query.RemindQuery;

import java.util.List;

public interface CdRemindService extends IService<CdRemind> {

    PageResult<CdRemind> queryPage(RemindQuery query);

    void saveRemind(CdRemind remind);

    void removeRemind(Long remindId);

    void changeStatus(Long remindId, String status);

    List<CdRemindLog> todayLog(Long patientId);

    void check(Long remindId, String checkStatus);
}
