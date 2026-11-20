-- =====================================================================
-- 设备绑定 & 华为运动健康数据导入 —— 数据库变更脚本
-- 执行顺序：1 → 2 → 3 → 4 → 5，不要打乱（第 5 步依赖第 1 步新增的列）
-- 执行前建议备份：
--   mysqldump -uroot -p --default-character-set=utf8mb4 chronic_db cd_device cd_measure > backup_before_device.sql
-- =====================================================================

-- =====================================================================
-- 0. 修复历史缺失表（与本次功能无关，但会挡住"导入后重算评分"）
--    cd_disease_weight 是 CdDiseaseWeightMapper 依赖的病种权重表，
--    当前库里不存在，导致 /chronic/score/calc 直接报 Table doesn't exist
-- =====================================================================
CREATE TABLE IF NOT EXISTS cd_disease_weight (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  disease_code VARCHAR(64)  DEFAULT NULL COMMENT '病种编码：1高血压 2糖尿病 3冠心病 4慢阻肺',
  disease_name VARCHAR(64)  DEFAULT NULL COMMENT '病种名称',
  weight       DECIMAL(10,4) DEFAULT NULL COMMENT '该病种在综合评分中的占比(0-1)',
  PRIMARY KEY (id),
  UNIQUE KEY uk_disease_weight_code (disease_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='病种综合权重配置';

-- cd_health_score.detail_json 原为 varchar(255)，评分明细 JSON 会被截断，
-- 报 "Data too long for column 'detail_json'"，改成 TEXT
ALTER TABLE cd_health_score MODIFY COLUMN detail_json TEXT NULL COMMENT '各指标评分明细(JSON)';

-- ---------------------------------------------------------------------
-- 1. 设备档案表：补充蓝牙设备识别字段
-- ---------------------------------------------------------------------
ALTER TABLE cd_device
  ADD COLUMN device_mac     VARCHAR(32)  NULL COMMENT 'MAC地址，仅安卓可获取，可空'        AFTER device_name,
  ADD COLUMN model_name     VARCHAR(64)  NULL COMMENT '型号中文名，如 华为手环 10'          AFTER model,
  ADD COLUMN sn_suffix      VARCHAR(16)  NULL COMMENT '蓝牙名后缀（MAC后3位），如 4A7'      AFTER model_name,
  ADD COLUMN alias          VARCHAR(32)  NULL COMMENT '用户别名，如 爸爸的手环'              AFTER sn_suffix,
  ADD COLUMN hw_device_code VARCHAR(32)  NULL COMMENT '华为导出包 deviceCode，导入时关联设备' AFTER alias,
  ADD COLUMN bind_user_id   BIGINT       NULL COMMENT '绑定操作人（小程序用户ID）'           AFTER patient_name,
  ADD COLUMN bind_time      DATETIME     NULL COMMENT '绑定时间'                             AFTER bind_user_id,
  ADD COLUMN bind_source    VARCHAR(8)   NULL COMMENT '绑定来源：1模拟扫描 2手动输入 3医生后台' AFTER bind_time,
  ADD COLUMN last_sync_time DATETIME     NULL COMMENT '最近一次数据导入时间'                 AFTER last_online;

-- 设备码唯一，便于导入时按 deviceCode 关联设备
ALTER TABLE cd_device ADD UNIQUE KEY uk_device_hw_code (hw_device_code);

-- ---------------------------------------------------------------------
-- 2. 修正 cd_measure 唯一索引
--    原索引是 (device_id, indicator_code)，同一设备同一指标只能存一条，
--    导入 7 天心率会直接 Duplicate entry。改成业务唯一键：
--    同一患者 + 同一指标 + 同一时刻 只保留一条（顺带做导入去重）
-- ---------------------------------------------------------------------
ALTER TABLE cd_measure
  DROP INDEX uk_measure_record,
  ADD UNIQUE KEY uk_measure_record (patient_id, indicator_code, measure_time);

-- ---------------------------------------------------------------------
-- 3. 数据导入日志表
-- ---------------------------------------------------------------------
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
  skip_dup_rows    INT          NOT NULL DEFAULT 0 COMMENT '其中因重复跳过条数',
  error_detail     TEXT         NULL COMMENT '失败明细JSON：[{seq,reason}]',
  data_source      VARCHAR(8)   NOT NULL DEFAULT '3' COMMENT '1设备同步 2手动录入 3文件导入',
  status           VARCHAR(8)   NOT NULL DEFAULT '1' COMMENT '1全部成功 2部分成功 3失败',
  operator_user_id BIGINT       NULL COMMENT '操作人ID',
  operator_name    VARCHAR(64)  NULL COMMENT '操作人姓名',
  date_from        DATE         NULL COMMENT '数据起始日',
  date_to          DATE         NULL COMMENT '数据结束日',
  import_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '导入时间',
  PRIMARY KEY (import_id),
  KEY idx_import_patient (patient_id),
  KEY idx_import_hash (file_hash),
  KEY idx_import_time (import_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导入日志';

-- ---------------------------------------------------------------------
-- 4. 字典
-- ---------------------------------------------------------------------
INSERT INTO sys_dict_type (dict_name, dict_type, status, remark) VALUES
('设备类型', 'device_type',    '0', '1手环 2血压计 3血氧仪 4血糖仪'),
('设备型号', 'device_model',   '0', '第三方设备型号，如 HUAWEI Band 10'),
('数据来源', 'measure_source', '0', '1设备同步 2手动录入 3文件导入');

INSERT INTO sys_dict_data (dict_sort, dict_label, dict_value, dict_type, is_default, status) VALUES
(1, '智能手环/手表', '1', 'device_type', 'Y', '0'),
(2, '血压计',       '2', 'device_type', 'N', '0'),
(3, '血氧仪',       '3', 'device_type', 'N', '0'),
(4, '血糖仪',       '4', 'device_type', 'N', '0'),

(1, '华为手环 10',    'HUAWEI Band 10',     'device_model', 'Y', '0'),
(2, '华为手环 9',     'HUAWEI Band 9',      'device_model', 'N', '0'),
(3, '华为手表 GT 5',  'HUAWEI Watch GT 5',  'device_model', 'N', '0'),
(4, '华为手表 Fit 4', 'HUAWEI Watch Fit 4', 'device_model', 'N', '0'),
(9, '其他设备',       'OTHER',              'device_model', 'N', '0'),

(1, '设备同步', '1', 'measure_source', 'N', '0'),
(2, '手动录入', '2', 'measure_source', 'Y', '0'),
(3, '文件导入', '3', 'measure_source', 'N', '0');

-- ---------------------------------------------------------------------
-- 5. 把演示用的 3 台 Ranger 设备改成真实的华为设备
-- ---------------------------------------------------------------------
UPDATE cd_device SET
  device_no = 'HUAWEI Band 10-4A7', device_name = 'HUAWEI Band 10-4A7',
  device_type = '1', vendor = 'HUAWEI', model = 'HUAWEI Band 10',
  model_name = '华为手环 10', sn_suffix = '4A7'
WHERE device_id = 1;

UPDATE cd_device SET
  device_no = 'HUAWEI Band 10-9C2', device_name = 'HUAWEI Band 10-9C2',
  device_type = '1', vendor = 'HUAWEI', model = 'HUAWEI Band 10',
  model_name = '华为手环 10', sn_suffix = '9C2'
WHERE device_id = 2;

UPDATE cd_device SET
  device_no = 'HUAWEI Watch GT 5-1F3', device_name = 'HUAWEI Watch GT 5-1F3',
  device_type = '1', vendor = 'HUAWEI', model = 'HUAWEI Watch GT 5',
  model_name = '华为手表 GT 5', sn_suffix = '1F3'
WHERE device_id = 3;

-- ---------------------------------------------------------------------
-- 6. 自查
-- ---------------------------------------------------------------------
-- DESC cd_device;
-- SHOW INDEX FROM cd_measure;
-- SHOW INDEX FROM cd_device;
-- DESC cd_import_log;
-- SELECT dict_type, COUNT(*) FROM sys_dict_data GROUP BY dict_type;
-- SELECT device_id, device_no, model_name, sn_suffix FROM cd_device;

-- =====================================================================
-- 回滚脚本（仅在需要撤销时执行）
-- =====================================================================
-- ALTER TABLE cd_measure DROP INDEX uk_measure_record,
--   ADD UNIQUE KEY uk_measure_record (device_id, indicator_code);
-- DROP TABLE IF EXISTS cd_import_log;
-- ALTER TABLE cd_device DROP INDEX uk_device_hw_code;
-- ALTER TABLE cd_device
--   DROP COLUMN device_mac, DROP COLUMN model_name, DROP COLUMN sn_suffix,
--   DROP COLUMN alias, DROP COLUMN hw_device_code, DROP COLUMN bind_user_id,
--   DROP COLUMN bind_time, DROP COLUMN bind_source, DROP COLUMN last_sync_time;
-- DELETE FROM sys_dict_data WHERE dict_type IN ('device_type','device_model','measure_source');
-- DELETE FROM sys_dict_type WHERE dict_type IN ('device_type','device_model','measure_source');
