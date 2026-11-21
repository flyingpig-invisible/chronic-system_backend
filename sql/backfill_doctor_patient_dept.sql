-- 存量数据回填：保证每位医生、每个患者都有科室
-- 1) 医生：没有科室的按同科室（无则取首个启用科室）补齐为 101 之外的最近科室；本系统医生均已分配，此处仅兜底
UPDATE sys_user u
SET dept_id = COALESCE(
        (SELECT MIN(d.dept_id) FROM sys_dept d WHERE d.del_flag = '0' AND d.status = '0'),
        u.dept_id)
WHERE u.user_type = '1' AND u.del_flag = '0' AND u.dept_id IS NULL;

-- 2) 患者可见科室（cd_patient_dept）：无任何关联的患者按
--    责任医生科室 > cd_patient.dept_id > 首个启用科室 依次兜底
INSERT INTO cd_patient_dept (patient_id, dept_id, source, create_time)
SELECT p.patient_id, fill.dept_id, '2', NOW()
FROM cd_patient p
JOIN (
    SELECT p2.patient_id,
           COALESCE(u.dept_id, p2.dept_id,
                    (SELECT MIN(d.dept_id) FROM sys_dept d WHERE d.del_flag = '0' AND d.status = '0')) AS dept_id
    FROM cd_patient p2
    LEFT JOIN sys_user u ON u.user_id = p2.doctor_id AND u.del_flag = '0'
    WHERE p2.del_flag = '0'
      AND NOT EXISTS (SELECT 1 FROM cd_patient_dept d WHERE d.patient_id = p2.patient_id)
) fill ON fill.patient_id = p.patient_id
WHERE fill.dept_id IS NOT NULL;