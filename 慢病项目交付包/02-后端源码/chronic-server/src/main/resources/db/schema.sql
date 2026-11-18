-- 慢病管理小程序后端 · 表结构
-- 每次启动重建（spring.sql.init.mode=always），演示环境够用
-- 生产请改成增量迁移（Flyway/Liquibase）

DROP TABLE IF EXISTS t_account;
CREATE TABLE t_account (
  phone       VARCHAR(20) PRIMARY KEY,
  password    VARCHAR(64) NOT NULL,
  patient_id  VARCHAR(64),
  role        VARCHAR(20),
  roles       VARCHAR(500),
  real_name   VARCHAR(50),
  created_at  DATETIME
);

DROP TABLE IF EXISTS t_role;
CREATE TABLE t_role (
  role_key    VARCHAR(20) PRIMARY KEY,
  name        VARCHAR(50),
  icon        VARCHAR(50),
  tone        VARCHAR(20),
  description VARCHAR(200),
  tag         VARCHAR(50)
);

DROP TABLE IF EXISTS t_patient;
CREATE TABLE t_patient (
  id             VARCHAR(64) PRIMARY KEY,
  name           VARCHAR(50),
  alias          VARCHAR(50),
  gender         VARCHAR(10),
  age            INT,
  height         DECIMAL(6,1),
  weight         DECIMAL(6,1),
  bmi            DECIMAL(5,1),
  phone          VARCHAR(20),
  id_card        VARCHAR(40),
  diseases       VARCHAR(500),
  insurance      VARCHAR(100),
  home           VARCHAR(200),
  doctor_name    VARCHAR(50),
  doctor_title   VARCHAR(50),
  doctor_dept    VARCHAR(50),
  doctor_hospital VARCHAR(100)
);

DROP TABLE IF EXISTS t_family;
CREATE TABLE t_family (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  name       VARCHAR(50),
  relation   VARCHAR(20),
  phone      VARCHAR(20)
);

DROP TABLE IF EXISTS t_risk;
CREATE TABLE t_risk (
  patient_id   VARCHAR(64) PRIMARY KEY,
  level        VARCHAR(20),
  tone         VARCHAR(20),
  summary      VARCHAR(200),
  advice       VARCHAR(300),
  score        INT,
  checkin_days INT,
  rank_text    VARCHAR(100)
);

DROP TABLE IF EXISTS t_metric_today;
CREATE TABLE t_metric_today (
  id           VARCHAR(64) PRIMARY KEY,
  patient_id   VARCHAR(64),
  metric_key   VARCHAR(30),
  name         VARCHAR(50),
  metric_value VARCHAR(50),
  unit         VARCHAR(20),
  icon         VARCHAR(50),
  color        VARCHAR(20),
  status       VARCHAR(20),
  status_text  VARCHAR(30),
  sort_no      INT
);

DROP TABLE IF EXISTS t_task;
CREATE TABLE t_task (
  id           VARCHAR(64) PRIMARY KEY,
  patient_id   VARCHAR(64),
  icon         VARCHAR(50),
  title        VARCHAR(100),
  description  VARCHAR(300),
  done         BOOLEAN,
  task_type    VARCHAR(30),
  url          VARCHAR(200),
  sort_no      INT
);

DROP TABLE IF EXISTS t_quick_action;
CREATE TABLE t_quick_action (
  id      VARCHAR(64) PRIMARY KEY,
  icon    VARCHAR(50),
  name    VARCHAR(50),
  url     VARCHAR(200),
  sort_no INT
);

DROP TABLE IF EXISTS t_trend;
CREATE TABLE t_trend (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  trend_type VARCHAR(30),
  payload    VARCHAR(8000)
);

DROP TABLE IF EXISTS t_disease_tab;
CREATE TABLE t_disease_tab (
  tab_key VARCHAR(30) PRIMARY KEY,
  name    VARCHAR(50),
  sort_no INT
);

DROP TABLE IF EXISTS t_record;
CREATE TABLE t_record (
  id          VARCHAR(64) PRIMARY KEY,
  patient_id  VARCHAR(64),
  record_type VARCHAR(30),
  record_value VARCHAR(50),
  unit        VARCHAR(20),
  time_text   VARCHAR(50),
  status      VARCHAR(20),
  label       VARCHAR(30),
  color       VARCHAR(20),
  measured_at DATETIME
);

DROP TABLE IF EXISTS t_alert;
CREATE TABLE t_alert (
  id          VARCHAR(64) PRIMARY KEY,
  patient_id  VARCHAR(64),
  level       VARCHAR(20),
  level_text  VARCHAR(30),
  title       VARCHAR(100),
  alert_value VARCHAR(50),
  unit        VARCHAR(20),
  time_text   VARCHAR(50),
  description VARCHAR(300),
  status      VARCHAR(20),
  is_read     BOOLEAN
);

DROP TABLE IF EXISTS t_alert_summary;
CREATE TABLE t_alert_summary (
  patient_id VARCHAR(64) PRIMARY KEY,
  high_cnt   INT,
  mid_cnt    INT,
  low_cnt    INT,
  tip        VARCHAR(100),
  sub_tip    VARCHAR(200)
);

DROP TABLE IF EXISTS t_medication;
CREATE TABLE t_medication (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  name       VARCHAR(100),
  dose       VARCHAR(50),
  way        VARCHAR(50),
  time_text  VARCHAR(50),
  period     VARCHAR(20),
  status     VARCHAR(20),
  taken_at   VARCHAR(50)
);

DROP TABLE IF EXISTS t_doctor_plan;
CREATE TABLE t_doctor_plan (
  id          VARCHAR(64) PRIMARY KEY,
  patient_id  VARCHAR(64),
  doctor      VARCHAR(50),
  title       VARCHAR(100),
  tag         VARCHAR(30),
  description VARCHAR(300),
  period      VARCHAR(100),
  progress    DECIMAL(5,2),
  done        INT,
  total       INT
);

DROP TABLE IF EXISTS t_recipe;
CREATE TABLE t_recipe (
  id          VARCHAR(64) PRIMARY KEY,
  name        VARCHAR(100),
  kcal        INT,
  salt        DECIMAL(5,2),
  gi          VARCHAR(20),
  tags        VARCHAR(300),
  description VARCHAR(300),
  tone        VARCHAR(20)
);

DROP TABLE IF EXISTS t_guide_category;
CREATE TABLE t_guide_category (
  id      INT PRIMARY KEY,
  name    VARCHAR(50),
  sort_no INT
);

DROP TABLE IF EXISTS t_guide;
CREATE TABLE t_guide (
  id          VARCHAR(64) PRIMARY KEY,
  category    VARCHAR(50),
  title       VARCHAR(100),
  description VARCHAR(300),
  tone        VARCHAR(20),
  audio       BOOLEAN,
  tag         VARCHAR(50),
  url         VARCHAR(200)
);

DROP TABLE IF EXISTS t_training;
CREATE TABLE t_training (
  id     VARCHAR(64) PRIMARY KEY,
  title  VARCHAR(100),
  target VARCHAR(100),
  done   INT,
  total  INT,
  tag    VARCHAR(50)
);

DROP TABLE IF EXISTS t_article;
CREATE TABLE t_article (
  id         VARCHAR(64) PRIMARY KEY,
  title      VARCHAR(200),
  author     VARCHAR(100),
  tone       VARCHAR(20),
  audio      BOOLEAN,
  video      BOOLEAN,
  time_text  VARCHAR(50),
  tag        VARCHAR(50),
  content    VARCHAR(8000)
);

DROP TABLE IF EXISTS t_chat_message;
CREATE TABLE t_chat_message (
  id         VARCHAR(64) PRIMARY KEY,
  session_id VARCHAR(64),
  patient_id VARCHAR(64),
  role       VARCHAR(20),
  msg_type   VARCHAR(20),
  content    VARCHAR(2000),
  citations  VARCHAR(4000),
  created_at DATETIME
);

DROP TABLE IF EXISTS t_quick_question;
CREATE TABLE t_quick_question (
  id       INT PRIMARY KEY,
  question VARCHAR(200),
  sort_no  INT
);

DROP TABLE IF EXISTS t_device;
CREATE TABLE t_device (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  name       VARCHAR(100),
  model      VARCHAR(100),
  online     BOOLEAN,
  battery    INT,
  sync_at    VARCHAR(50),
  types      VARCHAR(300)
);

DROP TABLE IF EXISTS t_emr;
CREATE TABLE t_emr (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  archive_no VARCHAR(50),
  payload    VARCHAR(8000)
);

DROP TABLE IF EXISTS t_score_detail;
CREATE TABLE t_score_detail (
  patient_id VARCHAR(64) PRIMARY KEY,
  payload    VARCHAR(8000)
);

DROP TABLE IF EXISTS t_remote_medical;
CREATE TABLE t_remote_medical (
  id      VARCHAR(64) PRIMARY KEY,
  payload VARCHAR(8000)
);

DROP TABLE IF EXISTS t_hospital;
CREATE TABLE t_hospital (
  id      VARCHAR(64) PRIMARY KEY,
  name    VARCHAR(100),
  level   VARCHAR(50),
  address VARCHAR(200),
  city    VARCHAR(50)
);

DROP TABLE IF EXISTS t_doctor;
CREATE TABLE t_doctor (
  id          VARCHAR(64) PRIMARY KEY,
  hospital_id VARCHAR(64),
  name        VARCHAR(50),
  title       VARCHAR(50),
  dept        VARCHAR(50),
  fee         DECIMAL(8,2),
  avatar      VARCHAR(200)
);

DROP TABLE IF EXISTS t_appointment;
CREATE TABLE t_appointment (
  id         VARCHAR(64) PRIMARY KEY,
  patient_id VARCHAR(64),
  doctor_id  VARCHAR(64),
  slot       VARCHAR(100),
  symptom    VARCHAR(500),
  status     VARCHAR(20),
  created_at DATETIME
);
