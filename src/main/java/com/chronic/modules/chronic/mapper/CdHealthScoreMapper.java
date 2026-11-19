package com.chronic.modules.chronic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.chronic.entity.CdHealthScore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CdHealthScore Mapper
 *
 * @author chronic
 */
@Mapper
public interface CdHealthScoreMapper extends BaseMapper<CdHealthScore> {
    /**
     * 查询每个患者最近一次评分记录（无评分的患者不会出现在结果中）。
     * 排序口径与患者列表/档案一致：calc_time 倒序，同一时间取 score_id 最大者。
     */
    @Select("""
            SELECT h.* FROM cd_health_score h
            INNER JOIN (
                SELECT patient_id, MAX(score_id) AS score_id FROM (
                    SELECT patient_id, score_id,
                           ROW_NUMBER() OVER (PARTITION BY patient_id ORDER BY calc_time DESC, score_id DESC) AS rn
                    FROM cd_health_score
                ) t WHERE t.rn = 1 GROUP BY patient_id
            ) latest ON latest.score_id = h.score_id
            """)
    List<CdHealthScore> selectLatestPerPatient();

    /** 查询指定患者最近一次评分记录（无评分的患者不会出现在结果中） */
    @Select("""
            <script>
            SELECT h.* FROM cd_health_score h
            INNER JOIN (
                SELECT patient_id, MAX(score_id) AS score_id FROM (
                    SELECT patient_id, score_id,
                           ROW_NUMBER() OVER (PARTITION BY patient_id ORDER BY calc_time DESC, score_id DESC) AS rn
                    FROM cd_health_score
                    <where>
                        patient_id IN
                        <foreach collection="patientIds" item="id" open="(" separator="," close=")">#{id}</foreach>
                    </where>
                ) t WHERE t.rn = 1 GROUP BY patient_id
            ) latest ON latest.score_id = h.score_id
            </script>
            """)
    List<CdHealthScore> selectLatestOfPatients(@Param("patientIds") List<Long> patientIds);
}
