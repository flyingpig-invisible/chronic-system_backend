package com.chronic.modules.miniapp.mapper;

import com.chronic.modules.chronic.entity.CdPatient;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 患者档案只读查询
 * <p>
 * 显式列出与医生端一致的列，避免实体冗余字段影响 SQL。
 *
 * @author chronic
 */
@Mapper
public interface MpPatientMapper {

    String COLUMNS = "patient_id, patient_no, name, gender, birthday, phone, id_card, address, "
            + "mp_user_id, doctor_id, dept_id, risk_level, risk_score, pending_warning_count, "
            + "last_measure_time, status, create_time, update_time, remark";

    @Select("SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND mp_user_id = #{userId} "
            + "ORDER BY patient_id ASC LIMIT 1")
    CdPatient selectByMpUserId(@Param("userId") Long userId);

    @Select("SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND patient_id = #{patientId}")
    CdPatient selectPatient(@Param("patientId") Long patientId);

    @Select("SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND phone = #{phone} "
            + "ORDER BY patient_id ASC LIMIT 1")
    CdPatient selectByPhone(@Param("phone") String phone);

    @Select("SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND patient_no = #{patientNo} "
            + "ORDER BY patient_id ASC LIMIT 1")
    CdPatient selectByPatientNo(@Param("patientNo") String patientNo);

    @Select("SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND phone = #{phone} "
            + "AND (mp_user_id IS NULL OR mp_user_id = 0) ORDER BY patient_id ASC LIMIT 1")
    CdPatient selectUnlinkedByPhone(@Param("phone") String phone);

    @Select("<script>SELECT " + COLUMNS + " FROM cd_patient WHERE del_flag = '0' AND patient_id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "ORDER BY patient_id ASC</script>")
    List<CdPatient> selectPatients(@Param("ids") List<Long> ids);

    @Update("UPDATE cd_patient SET mp_user_id = #{userId} WHERE patient_id = #{patientId}")
    int linkUser(@Param("patientId") Long patientId, @Param("userId") Long userId);
}