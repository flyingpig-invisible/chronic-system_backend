package com.chronic.modules.chronic.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.chronic.common.exception.ServiceException;
import com.chronic.common.result.PageResult;
import com.chronic.modules.chronic.entity.CdRemind;
import com.chronic.modules.chronic.entity.CdRemindLog;
import com.chronic.modules.chronic.mapper.CdRemindLogMapper;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.mapper.CdRemindMapper;
import com.chronic.modules.chronic.query.RemindQuery;
import com.chronic.modules.chronic.service.CdRemindService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CdRemindServiceImpl extends ServiceImpl<CdRemindMapper, CdRemind> implements CdRemindService {

    @Autowired
    private CdRemindLogMapper remindLogMapper;

    @Autowired
    private CdPatientMapper patientMapper;

    /** 漏服状态码 */
    private static final String MISSED = "2";

    @Override
    public PageResult<CdRemind> queryPage(RemindQuery query) {
        LambdaQueryWrapper<CdRemind> w = new LambdaQueryWrapper<>();
        w.eq(query.getPatientId() != null, CdRemind::getPatientId, query.getPatientId())
         .eq(StringUtils.hasText(query.getStatus()), CdRemind::getStatus, query.getStatus())
         .like(StringUtils.hasText(query.getKeyword()), CdRemind::getMedicineName, query.getKeyword())
         .orderByDesc(CdRemind::getCreateTime);
        Page<CdRemind> page = page(new Page<>(query.getPageNum(), query.getPageSize()), w);
        fillPatientName(page.getRecords());
        return PageResult.of(page.getTotal(), page.getRecords());
    }

    /** 填充患者姓名 */
    private void fillPatientName(List<CdRemind> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        java.util.Set<Long> ids = new java.util.HashSet<>();
        list.forEach(r -> { if (r.getPatientId() != null) ids.add(r.getPatientId()); });
        if (ids.isEmpty()) {
            return;
        }
        java.util.Map<Long, String> nameMap = new java.util.HashMap<>();
        patientMapper.selectBatchIds(ids).forEach(p -> nameMap.put(p.getPatientId(), p.getName()));
        list.forEach(r -> r.setPatientName(nameMap.get(r.getPatientId())));
    }

    @Override
    public void saveRemind(CdRemind remind) {
        if (remind.getPatientId() == null) {
            throw new ServiceException("患者不能为空");
        }
        if (!StringUtils.hasText(remind.getMedicineName())) {
            throw new ServiceException("药品名称不能为空");
        }
        if (!StringUtils.hasText(remind.getStatus())) {
            remind.setStatus("1");
        }
        saveOrUpdate(remind);
    }

    @Override
    public void removeRemind(Long remindId) {
        removeById(remindId);
    }

    @Override
    public void changeStatus(Long remindId, String status) {
        CdRemind remind = getById(remindId);
        if (remind == null) {
            throw new ServiceException("提醒不存在");
        }
        remind.setStatus(status);
        updateById(remind);
    }

    @Override
    public List<CdRemindLog> todayLog(Long patientId) {
        List<CdRemind> reminds = list(new LambdaQueryWrapper<CdRemind>()
                .eq(CdRemind::getPatientId, patientId)
                .eq(CdRemind::getStatus, "1"));
        LocalDate today = LocalDate.now();
        List<CdRemindLog> result = new ArrayList<>();
        for (CdRemind remind : reminds) {
            if (remind.getBeginDate() != null && today.isBefore(remind.getBeginDate())) {
                continue;
            }
            if (remind.getEndDate() != null && today.isAfter(remind.getEndDate())) {
                continue;
            }
            String times = remind.getRemindTimes();
            if (!StringUtils.hasText(times)) {
                times = "08:00";
            }
            for (String time : times.split(",")) {
                String planTime = time.trim();
                if (planTime.isEmpty()) {
                    continue;
                }
                CdRemindLog log = remindLogMapper.selectOne(new LambdaQueryWrapper<CdRemindLog>()
                        .eq(CdRemindLog::getRemindId, remind.getRemindId())
                        .eq(CdRemindLog::getPlanDate, today)
                        .eq(CdRemindLog::getPlanTime, planTime)
                        .last("limit 1"));
                if (log == null) {
                    log = new CdRemindLog();
                    log.setRemindId(remind.getRemindId());
                    log.setPatientId(patientId);
                    log.setPlanDate(today);
                    log.setPlanTime(planTime);
                    log.setCheckStatus("0");
                }
                result.add(log);
            }
        }
        return result;
    }

    @Override
    public List<CdRemindLog> logOfRemind(Long remindId, Integer days) {
        CdRemind remind = getById(remindId);
        if (remind == null) {
            throw new ServiceException("提醒不存在");
        }
        LocalDate today = LocalDate.now();
        LocalDate from = days != null && days > 0 ? today.minusDays(days - 1L) : null;
        if (remind.getBeginDate() != null && (from == null || from.isBefore(remind.getBeginDate()))) {
            from = remind.getBeginDate();
        }
        LocalDate to = remind.getEndDate() != null && remind.getEndDate().isBefore(today) ? remind.getEndDate() : today;

        String times = StringUtils.hasText(remind.getRemindTimes()) ? remind.getRemindTimes() : "08:00";
        List<String> planTimes = new ArrayList<>();
        for (String s : times.split(",")) {
            String time = s.trim();
            if (!time.isEmpty()) {
                planTimes.add(time);
            }
        }
        if (planTimes.isEmpty()) {
            planTimes.add("08:00");
        }

        Map<String, CdRemindLog> exist = new HashMap<>();
        LambdaQueryWrapper<CdRemindLog> w = new LambdaQueryWrapper<CdRemindLog>()
                .eq(CdRemindLog::getRemindId, remindId)
                .ge(from != null, CdRemindLog::getPlanDate, from)
                .le(CdRemindLog::getPlanDate, to);
        for (CdRemindLog log : remindLogMapper.selectList(w)) {
            exist.put(log.getPlanDate() + "|" + log.getPlanTime(), log);
        }

        // 按用药周期与提醒时间点补全：库里没有记录的计划，历史日期记为漏服，当天记为未打卡
        List<CdRemindLog> result = new ArrayList<>();
        if (from == null || to.isBefore(from)) {
            return result;
        }
        for (LocalDate d = to; !d.isBefore(from); d = d.minusDays(1)) {
            for (String planTime : planTimes) {
                CdRemindLog log = exist.get(d + "|" + planTime);
                if (log == null) {
                    log = new CdRemindLog();
                    log.setRemindId(remindId);
                    log.setPatientId(remind.getPatientId());
                    log.setPlanDate(d);
                    log.setPlanTime(planTime);
                    log.setCheckStatus(d.isBefore(today) || isOverdue(d, planTime) ? MISSED : "0");
                }
                result.add(log);
            }
        }
        return result;
    }

    /** 计划服药时点超过 2 小时仍未打卡即判为漏服 */
    private boolean isOverdue(LocalDate planDate, String planTime) {
        LocalDateTime planAt = planAt(planDate, planTime);
        return planAt != null && planAt.plusHours(2).isBefore(LocalDateTime.now());
    }

    /** 计划服药时点 */
    private LocalDateTime planAt(LocalDate planDate, String planTime) {
        if (planDate == null) {
            return null;
        }
        String time = StringUtils.hasText(planTime) ? planTime.trim() : "08:00";
        try {
            String[] parts = time.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : 0;
            return LocalDateTime.of(planDate, java.time.LocalTime.of(hour, minute));
        } catch (Exception e) {
            return null;
        }
    }
    @Override
    public void check(Long remindId, String checkStatus) {
        CdRemind remind = getById(remindId);
        if (remind == null) {
            throw new ServiceException("提醒不存在");
        }
        LocalDate today = LocalDate.now();
        CdRemindLog log = remindLogMapper.selectOne(new LambdaQueryWrapper<CdRemindLog>()
                .eq(CdRemindLog::getRemindId, remindId)
                .eq(CdRemindLog::getPlanDate, today)
                .orderByAsc(CdRemindLog::getPlanTime)
                .last("limit 1"));
        if (log == null) {
            log = new CdRemindLog();
            log.setRemindId(remindId);
            log.setPatientId(remind.getPatientId());
            log.setPlanDate(today);
            log.setPlanTime("00:00");
            log.setCheckStatus(checkStatus);
            log.setCheckTime(LocalDateTime.now());
            remindLogMapper.insert(log);
        } else {
            log.setCheckStatus(checkStatus);
            log.setCheckTime(LocalDateTime.now());
            remindLogMapper.updateById(log);
        }
    }
}
