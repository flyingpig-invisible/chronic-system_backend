-- =====================================================================
-- 演示用种子数据（可选）
-- 目的：让"导入数据 → 触发预警 → 重算评分"这条链路能完整演示
-- 现在库里 cd_warning_rule、cd_patient_disease、cd_score_weight 都是空的，
-- 不补这些数据，导入成功但预警不会触发、评分会报"该患者未登记病种"
-- =====================================================================

-- 1. 给患者 1（张三）登记病种：病种码 1 = 高血压（见 CdScoreServiceImpl.DISEASE_NAME）
INSERT INTO cd_patient_disease (patient_id, disease_code, disease_name, diagnose_date, severity, create_time)
SELECT 1, '1', '高血压', DATE '2024-03-01', '2', NOW()
WHERE NOT EXISTS (SELECT 1 FROM cd_patient_disease WHERE patient_id = 1 AND disease_code = '1');

-- 2. 预警规则（心率 / 血氧 / 睡眠）
--    说明：operator 支持 >= <= > < =，threshold_value 为阈值，warning_level 1低危 2中危 3高危
INSERT INTO cd_warning_rule
(rule_name, disease_code, indicator_code, indicator_name, operator, threshold_value,
 threshold_desc, warning_level, duration_minutes, action_desc, status, create_time)
VALUES
('静息心率偏高', '1', 'hr',   '心率',     '>',  100, '心率 > 100 次/分',   2, 0, '建议静坐休息 5 分钟后复测，持续偏高请联系医生', '0', NOW()),
('心动过缓',     '1', 'hr',   '心率',     '<',   50, '心率 < 50 次/分',    3, 0, '立即联系医生，必要时急诊',                       '0', NOW()),
('血氧偏低',     '1', 'spo2', '血氧',     '<',   95, '血氧 < 95%',         3, 0, '保持通风，重复测量，仍低请立即就医',             '0', NOW()),
('睡眠不足',     '1', 'sleep','睡眠时长', '<',    6, '睡眠 < 6 小时',      1, 0, '建议规律作息，保证 7 小时睡眠',                  '0', NOW()),
('严重睡眠不足', '1', 'sleep','睡眠时长', '<',    4, '睡眠 < 4 小时',      2, 0, '排查睡眠呼吸暂停等问题，建议门诊评估',           '0', NOW());

-- 3. 评分权重（可选）
--    不配置时 CdScoreServiceImpl 会把患者最新指标权重平分，也能算出分数；
--    配置后评分更贴近业务预期。权重之和需为 1。
-- INSERT INTO cd_score_weight (disease_code, indicator_code, indicator_name, weight, create_time) VALUES
-- ('1', 'sbp',  '收缩压',   0.30, NOW()),
-- ('1', 'dbp',  '舒张压',   0.25, NOW()),
-- ('1', 'hr',   '心率',     0.20, NOW()),
-- ('1', 'spo2', '血氧',     0.15, NOW()),
-- ('1', 'sleep','睡眠时长', 0.10, NOW());

-- 4. 自查
-- SELECT * FROM cd_patient_disease WHERE patient_id = 1;
-- SELECT rule_name, indicator_code, operator, threshold_value, warning_level FROM cd_warning_rule;
