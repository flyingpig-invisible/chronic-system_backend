-- =====================================================================
-- 设备绑定 & 华为运动健康数据导入 —— 数据库变更脚本（幂等版）
--
-- 可重复执行：表/列/索引已存在时自动跳过，不会报错。
-- 执行：mysql -uroot -p --default-character-set=utf8mb4 chronic_db < ddl_device_import.sql
--
-- 执行前务必备份：
--   mysqldump -uroot -p --default-character-set=utf8mb4 --single-transaction chronic_db ^
--     cd_device cd_measure cd_health_score > backup_before_device.sql
-- =====================================================================

-- 0. 前置：病种权重表（CdDiseaseWeightMapper 依赖）
CREATE TABLE IF NOT EXISTS cd_disease_weight (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  disease_code VARCHAR(64)  DEFAULT NULL COMMENT '病种编码：1高血压 2糖尿病 3冠心病 4慢阻肺',
  disease_name VARCHAR(64)  DEFAULT NULL COMMENT '病种名称',
  weight       DECIMAL(10,4) DEFAULT NULL COMMENT '该病种在综合评分中的占比(0-1)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_disease_weight_code (disease_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病种综合权重配置';

-- 1. cd_device 补充蓝牙/绑定字段（逐列判断，幂等）
DROP PROCEDURE IF EXISTS sp_add_col;
DELIMITER $$
CREATE PROCEDURE sp_add_col(IN t VARCHAR(64), IN c VARCHAR(64), IN d TEXT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns
                  WHERE table_schema=DATABASE() AND table_name=t AND column_name=c) THEN
    SET @s = CONCAT('ALTER TABLE `', t, '` ADD COLUMN `', c, '` ', d);
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;

CALL sp_add_col('cd_device','device_mac','VARCHAR(32) NULL COMMENT ''MAC地址，仅安卓可获取，可空'' AFTER device_name');
CALL sp_add_col('cd_device','model_name','VARCHAR(64) NULL COMMENT ''型号中文名，如 华为手环 10'' AFTER model');
CALL sp_add_col('cd_device','sn_suffix','VARCHAR(16) NULL COMMENT ''蓝牙名后缀（MAC后3位），如 4A7'' AFTER model_name');
CALL sp_add_col('cd_device','alias','VARCHAR(32) NULL COMMENT ''用户别名，如 爸爸的手环'' AFTER sn_suffix');
CALL sp_add_col('cd_device','hw_device_code','VARCHAR(32) NULL COMMENT ''华为导出包 deviceCode，导入时关联设备'' AFTER alias');
CALL sp_add_col('cd_device','bind_user_id','BIGINT NULL COMMENT ''绑定操作人（小程序用户ID）'' AFTER patient_name');
CALL sp_add_col('cd_device','bind_time','DATETIME NULL COMMENT ''绑定时间'' AFTER bind_user_id');
CALL sp_add_col('cd_device','bind_source','VARCHAR(8) NULL COMMENT ''绑定来源：1模拟扫描 2手动输入 3医生后台'' AFTER bind_time');
CALL sp_add_col('cd_device','last_sync_time','DATETIME NULL COMMENT ''最近一次数据导入时间'' AFTER last_online');
DROP PROCEDURE IF EXISTS sp_add_col;

-- 唯一键 uk_device_hw_code（幂等）
DROP PROCEDURE IF EXISTS sp_add_idx;
DELIMITER $$
CREATE PROCEDURE sp_add_idx(IN t VARCHAR(64), IN i VARCHAR(64), IN cols TEXT, IN u TINYINT)
BEGIN
  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                  WHERE table_schema=DATABASE() AND table_name=t AND index_name=i) THEN
    SET @s = CONCAT('ALTER TABLE `', t, '` ADD ', IF(u=1,'UNIQUE ',''), 'KEY `', i, '` (', cols, ')');
    PREPARE st FROM @s; EXECUTE st; DEALLOCATE PREPARE st;
  END IF;
END$$
DELIMITER ;
CALL sp_add_idx('cd_device','uk_device_hw_code','hw_device_code',1);
DROP PROCEDURE IF EXISTS sp_add_idx;

-- 2. cd_measure 业务唯一键 (patient_id, indicator_code, measure_time)
--    原库中不存在 uk_measure_record，这里不做 DROP，只在缺失时 ADD。
--    若已有重复数据则跳过并提示，不擅自删数据。
SELECT CONCAT('cd_measure 重复组数=', COUNT(*)) AS precheck FROM (
    SELECT 1 FROM cd_measure GROUP BY patient_id, indicator_code, measure_time HAVING COUNT(*)>1) x;

DROP PROCEDURE IF EXISTS sp_fix_measure_unique;
DELIMITER $$
CREATE PROCEDURE sp_fix_measure_unique()
BEGIN
  DECLARE v_dup INT;
  SELECT COUNT(*) INTO v_dup FROM (
      SELECT 1 FROM cd_measure GROUP BY patient_id, indicator_code, measure_time HAVING COUNT(*)>1) x;
  IF v_dup > 0 THEN
    SELECT 'cd_measure 存在重复数据，已跳过唯一索引；请先清理后重跑' AS warn;
  ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.statistics
                      WHERE table_schema=DATABASE() AND table_name='cd_measure' AND index_name='uk_measure_record') THEN
    ALTER TABLE cd_measure ADD UNIQUE KEY uk_measure_record (patient_id, indicator_code, measure_time);
    SELECT 'uk_measure_record 已创建' AS result;
  ELSE
    SELECT 'uk_measure_record 已存在，跳过' AS result;
  END IF;
END$$
DELIMITER ;
CALL sp_fix_measure_unique();
DROP PROCEDURE IF EXISTS sp_fix_measure_unique;

-- 3. 导入日志表（幂等）
CREATE TABLE IF NOT EXISTS cd_import_log (
  import_id        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '导入ID',
  patient_id       BIGINT       NOT NULL COMMENT '患者ID',
  patient_name     VARCHAR(64)  NULL COMMENT '患者姓名（冗余，便于列表展示）',
  device_id        BIGINT       NULL COMMENT '指定来源设备ID，可为空',
  file_name        VARCHAR(255) NULL COMMENT '文件名',
  file_type        VARCHAR(16)  NULL COMMENT 'ZIP / JSON',
  file_hash        CHAR(32)     NULL COMMENT '文件MD5，用于幂等去重',
  file_count       INT          NULL COMMENT '解析的 json 文件数',
  raw_records      INT          NULL COMMENT '原始记录数',
  parsed_points    INT          NULL COMMENT '展开后的采样点数（聚合前）',
  total_rows       INT          NULL COMMENT '兼容字段，等于 raw_records',
  success_rows     INT          NOT NULL DEFAULT 0 COMMENT '成功入库条数',
  failed_rows      INT          NOT NULL DEFAULT 0 COMMENT '失败/跳过条数',
  skip_dup_rows    INT          NOT NULL DEFAULT 0 COMMENT '其中因重复跳过的条数',
  error_detail     MEDIUMTEXT   NULL COMMENT '失败明细 JSON',
  data_source      CHAR(1)      NULL COMMENT '数据来源：3=华为运动健康导入',
  status           CHAR(1)      NULL COMMENT '1成功 2部分成功 3失败',
  operator_user_id BIGINT       NULL COMMENT '操作人用户ID',
  operator_name    VARCHAR(64)  NULL COMMENT '操作人姓名',
  date_from        DATE         NULL COMMENT '数据起始日期',
  date_to          DATE         NULL COMMENT '数据结束日期',
  import_time      DATETIME     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  PRIMARY KEY (import_id),
  KEY idx_import_patient (patient_id, import_time),
  KEY idx_import_hash (patient_id, file_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备数据导入日志';

-- 4.（已移除）原脚本此处会把设备 1-3 改名成华为设备，
--    但设备 1-3 是已绑定的历史设备，改名会和 demo_huawei_data.sql 里
--    新导入的可扫描手环（101-103）撞 uk_device_no 唯一索引。
--    演示用的未绑定手环统一由 demo_huawei_data.sql 提供，这里不做任何 UPDATE。

-- 5. 自查
SELECT column_name FROM information_schema.columns
 WHERE table_schema=DATABASE() AND table_name='cd_device'
   AND column_name IN ('device_mac','model_name','sn_suffix','alias','hw_device_code','bind_user_id','bind_time','bind_source','last_sync_time')
 ORDER BY column_name;
SELECT index_name, GROUP_CONCAT(column_name ORDER BY seq_in_index) AS cols
  FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name IN ('cd_measure','cd_device')
   AND index_name IN ('uk_measure_record','uk_device_hw_code') GROUP BY index_name;

-- =====================================================================
-- 回滚（手动执行，先备份）
-- =====================================================================
-- ALTER TABLE cd_measure DROP INDEX uk_measure_record;
-- DROP TABLE IF EXISTS cd_import_log;
-- ALTER TABLE cd_device DROP INDEX uk_device_hw_code,
--   DROP COLUMN device_mac, DROP COLUMN model_name, DROP COLUMN sn_suffix,
--   DROP COLUMN alias, DROP COLUMN hw_device_code, DROP COLUMN bind_user_id,
--   DROP COLUMN bind_time, DROP COLUMN bind_source, DROP COLUMN last_sync_time;
