package com.chronic.modules.chronic.support.huawei;

import java.util.Set;

/**
 * 华为运动健康导出包：数据类型码常量与归类
 * <p>
 * 类型码取自导出包内的《Health detail data-Data Field Description.xlsx》→ Health type No. 表
 *
 * @author chronic
 */
public final class HuaweiTypes {

    private HuaweiTypes() {
    }

    /** 心率（旧格式，value 直接是数字字符串） */
    public static final int HEART_RATE = 7;
    /** 专业睡眠（分期点） */
    public static final int SLEEP_ANALYZED = 9;
    /** 血氧（旧格式，avgSaturation） */
    public static final int BLOOD_OXYGEN = 16;
    /** 血氧（新格式，spo2） */
    public static final int BLOOD_OXYGEN_NEW = 400021;
    /** 睡眠监测（日汇总） */
    public static final int SLEEP_DETECT = 500005;
    /** 动态心率，value = {"bpm":74} */
    public static final int DYNAMIC_HEART_RATE = 500023;
    /** 静息心率，value = {"restBpm":58} */
    public static final int RESTING_HEART_RATE = 500024;

    /** 计入睡眠时长的分期点 */
    public static final Set<String> SLEEP_SEGMENT_KEYS = Set.of(
            "PROFESSIONAL_SLEEP_SHALLOW",
            "PROFESSIONAL_SLEEP_DEEP",
            "PROFESSIONAL_SLEEP_DREAM",
            "PROFESSIONAL_SLEEP_NOON");

    public static boolean isHeartRate(int type) {
        return type == HEART_RATE || type == DYNAMIC_HEART_RATE || type == RESTING_HEART_RATE;
    }

    public static boolean isBloodOxygen(int type) {
        return type == BLOOD_OXYGEN || type == BLOOD_OXYGEN_NEW;
    }

    public static boolean isSleepSegment(int type) {
        return type == SLEEP_ANALYZED;
    }
}
