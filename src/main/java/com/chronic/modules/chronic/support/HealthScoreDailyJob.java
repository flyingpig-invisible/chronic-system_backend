package com.chronic.modules.chronic.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.chronic.modules.chronic.entity.CdPatient;
import com.chronic.modules.chronic.mapper.CdPatientMapper;
import com.chronic.modules.chronic.service.CdScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 健康评分每日任务：每天 0 点为本系统全部患者重算健康评分
 *
 * @author chronic
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthScoreDailyJob {

    private final CdPatientMapper patientMapper;
    private final CdScoreService scoreService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void run() {
        List<CdPatient> patients = patientMapper.selectList(new LambdaQueryWrapper<CdPatient>()
                .select(CdPatient::getPatientId));
        int success = 0;
        int failed = 0;
        for (CdPatient patient : patients) {
            try {
                scoreService.calcScore(patient.getPatientId());
                success++;
            } catch (Exception e) {
                failed++;
                log.warn("[健康评分] 患者 {} 每日重算失败：{}", patient.getPatientId(), e.getMessage());
            }
        }
        log.info("[健康评分] 每日重算完成，成功 {} 人，失败 {} 人", success, failed);
    }
}