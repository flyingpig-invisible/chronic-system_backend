package com.chronic.modules.chronic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.chronic.modules.chronic.entity.CdMeasure;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * CdMeasure Mapper
 *
 * @author chronic
 */
@Mapper
public interface CdMeasureMapper extends BaseMapper<CdMeasure> {

    /**
     * 批量取「每个患者、每个指标」的最近一条测量记录。
     * 用于小程序端高危指标提醒，避免逐个指标查询。
     */
    @Select("<script>"
            + "SELECT * FROM ("
            + "  SELECT m.*, ROW_NUMBER() OVER (PARTITION BY m.patient_id, m.indicator_code "
            + "    ORDER BY m.measure_time DESC, m.measure_id DESC) AS rn"
            + "  FROM cd_measure m WHERE m.patient_id IN "
            + "  <foreach collection='patientIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + ") t WHERE t.rn = 1"
            + "</script>")
    List<CdMeasure> selectLatestPerPatientAndIndicator(@Param("patientIds") List<Long> patientIds);
}
