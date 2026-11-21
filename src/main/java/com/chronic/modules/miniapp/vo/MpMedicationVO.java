package com.chronic.modules.miniapp.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 今日用药提醒
 *
 * @author chronic
 */
@Data
public class MpMedicationVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long remindId;

    private String medicineName;

    private String dosage;

    private String frequency;

    private String frequencyName;

    private LocalDate beginDate;

    private LocalDate endDate;

    /** 今日各时间点打卡情况 */
    private List<Dose> doses;

    @Data
    public static class Dose implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long logId;

        private String planTime;

        private String checkStatus;

        private String checkStatusName;

        private LocalDateTime checkTime;
    }
}