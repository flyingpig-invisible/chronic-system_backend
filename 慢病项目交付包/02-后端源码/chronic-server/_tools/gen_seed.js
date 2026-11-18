/* 由小程序 utils/mock.js 生成后端种子数据 data.sql
 * 运行：node gen_seed.js <mini program 目录> <输出 data.sql>
 */
const path = require('path')
const fs = require('fs')
const crypto = require('crypto')

const src = process.argv[2]
const out = process.argv[3]
const m = require(path.join(src, 'utils/mock.js'))

const esc = s => String(s === undefined || s === null ? '' : s)
  .replace(/\\/g, '\\\\')
  .replace(/'/g, "''")
const q = s => "'" + esc(s) + "'"
const num = v => (v === undefined || v === null || v === '' || isNaN(v)) ? 'NULL' : String(v)
const bool = v => (v ? 'TRUE' : 'FALSE')
const now = "NOW()"

const L = []
const push = s => L.push(s)

push('-- 由 patient-miniprogram/utils/mock.js 自动生成，请勿手改')
push('-- 账号：13800136688 / 123456')
push('')

/* ---------- 账号 ---------- */
const pwd = crypto.createHash('sha256').update('123456').digest('hex')
push(`INSERT INTO t_account (phone,password,patient_id,role,roles,real_name,created_at) VALUES (${q('13800136688')},${q(pwd)},${q('1')},${q('patient')},${q(JSON.stringify(['patient', 'family', 'doctor']))},${q(m.patient.name)},${now});`)

/* ---------- 角色 ---------- */
m.roles.forEach(r => push(`INSERT INTO t_role (role_key,name,icon,tone,description,tag) VALUES (${q(r.key)},${q(r.name)},${q(r.icon)},${q(r.tone)},${q(r.desc)},${q(r.tag)});`))

/* ---------- 患者 ---------- */
const p = m.patient
const d = p.doctor || {}
push(`INSERT INTO t_patient (id,name,alias,gender,age,height,weight,bmi,phone,id_card,diseases,insurance,home,doctor_name,doctor_title,doctor_dept,doctor_hospital) VALUES (${q('1')},${q(p.name)},${q(p.alias)},${q(p.gender)},${num(p.age)},${num(p.height)},${num(p.weight)},${num(p.bmi)},${q(p.phone)},${q(p.idcard)},${q(JSON.stringify(p.diseases || []))},${q(p.insurance)},${q(p.home)},${q(d.name)},${q(d.title)},${q(d.dept)},${q(d.hospital)});`)

;(p.family || []).forEach((f, i) => push(`INSERT INTO t_family (id,patient_id,name,relation,phone) VALUES (${q('fm' + (i + 1))},${q('1')},${q(f.name)},${q(f.relation)},${q(f.phone)});`))

/* ---------- 风险评分 ---------- */
const r = m.risk || {}
push(`INSERT INTO t_risk (patient_id,level,tone,summary,advice,score,checkin_days,rank_text) VALUES (${q('1')},${q(r.level)},${q(r.type)},${q(r.summary)},${q(r.advice)},${num(r.score)},${num(r.checkinDays)},${q(r.rank)});`)

/* ---------- 今日指标 ---------- */
;(m.todayMetrics || []).forEach((x, i) => push(`INSERT INTO t_metric_today (id,patient_id,metric_key,name,metric_value,unit,icon,color,status,status_text,sort_no) VALUES (${q('mt' + (i + 1))},${q('1')},${q(x.key)},${q(x.name)},${q(x.value)},${q(x.unit)},${q(x.icon)},${q(x.color)},${q(x.status)},${q(x.statusText)},${i + 1});`))

/* ---------- 今日任务 ---------- */
;(m.todayTasks || []).forEach((t, i) => push(`INSERT INTO t_task (id,patient_id,icon,title,description,done,task_type,url,sort_no) VALUES (${q(t.id || 't' + (i + 1))},${q('1')},${q(t.icon)},${q(t.title)},${q(t.desc)},${bool(t.done)},${q(t.type)},${q(t.url)},${i + 1});`))

/* ---------- 快捷入口 ---------- */
;(m.quickActions || []).forEach((a, i) => push(`INSERT INTO t_quick_action (id,icon,name,url,sort_no) VALUES (${q(a.id || 'q' + (i + 1))},${q(a.icon)},${q(a.name)},${q(a.url)},${i + 1});`))

/* ---------- 趋势（整段 JSON 存） ---------- */
Object.keys(m.trends || {}).forEach(k => {
  push(`INSERT INTO t_trend (id,patient_id,trend_type,payload) VALUES (${q('tr_' + k)},${q('1')},${q(k)},${q(JSON.stringify(m.trends[k]))});`)
})

/* ---------- 病种 Tab ---------- */
;(m.diseaseTabs || []).forEach((t, i) => push(`INSERT INTO t_disease_tab (tab_key,name,sort_no) VALUES (${q(t.key)},${q(t.name)},${i + 1});`))

/* ---------- 测量记录 ---------- */
;(m.records || []).forEach((x, i) => push(`INSERT INTO t_record (id,patient_id,record_type,record_value,unit,time_text,status,label,color,measured_at) VALUES (${q(x.id || 'r' + (i + 1))},${q('1')},${q(x.type)},${q(x.value)},${q(x.unit)},${q(x.time)},${q(x.status)},${q(x.label)},${q(x.color)},${now});`))

/* ---------- 预警 ---------- */
;(m.alerts || []).forEach((a, i) => push(`INSERT INTO t_alert (id,patient_id,level,level_text,title,alert_value,unit,time_text,description,status,is_read) VALUES (${q(a.id || 'a' + (i + 1))},${q('1')},${q(a.level)},${q(a.levelText)},${q(a.title)},${q(a.value)},${q(a.unit)},${q(a.time)},${q(a.desc)},${q(a.status)},${bool(a.read)});`))

const s = m.alertSummary || {}
push(`INSERT INTO t_alert_summary (patient_id,high_cnt,mid_cnt,low_cnt,tip,sub_tip) VALUES (${q('1')},${num(s.high)},${num(s.mid)},${num(s.low)},${q(s.tip)},${q(s.subTip)});`)

/* ---------- 用药 ---------- */
;(m.medications || []).forEach((x, i) => push(`INSERT INTO t_medication (id,patient_id,name,dose,way,time_text,period,status,taken_at) VALUES (${q(x.id || 'm' + (i + 1))},${q('1')},${q(x.name)},${q(x.dose)},${q(x.way)},${q(x.time)},${q(x.period)},${q(x.status)},${q(x.takenAt)});`))

/* ---------- 医生计划 ---------- */
;(m.doctorPlans || []).forEach((x, i) => push(`INSERT INTO t_doctor_plan (id,patient_id,doctor,title,tag,description,period,progress,done,total) VALUES (${q(x.id || 'p' + (i + 1))},${q('1')},${q(x.doctor)},${q(x.title)},${q(x.tag)},${q(x.desc)},${q(x.period)},${num(x.progress)},${num(x.done)},${num(x.total)});`))

/* ---------- 食谱 ---------- */
;(m.recipes || []).forEach((x, i) => push(`INSERT INTO t_recipe (id,name,kcal,salt,gi,tags,description,tone) VALUES (${q(x.id || 'f' + (i + 1))},${q(x.name)},${num(x.kcal)},${num(x.salt)},${q(x.gi)},${q(JSON.stringify(x.tags || []))},${q(x.desc)},${q(x.tone)});`))

/* ---------- 指导分类 ---------- */
;(m.guideCategories || []).forEach((name, i) => push(`INSERT INTO t_guide_category (id,name,sort_no) VALUES (${i + 1},${q(name)},${i + 1});`))

/* ---------- 指导内容 ---------- */
;(m.recommendGuides || []).forEach((x, i) => push(`INSERT INTO t_guide (id,category,title,description,tone,audio,tag,url) VALUES (${q(x.id || 'g' + (i + 1))},${q((m.guideCategories || [])[0])},${q(x.title)},${q(x.desc)},${q(x.tone)},${bool(x.audio)},${q(x.tag)},${q(x.url)});`))

/* ---------- 康复训练 ---------- */
;(m.trainings || []).forEach((x, i) => push(`INSERT INTO t_training (id,title,target,done,total,tag) VALUES (${q(x.id || 'tt' + (i + 1))},${q(x.title)},${q(x.target)},${num(x.done)},${num(x.total)},${q(x.tag)});`))

/* ---------- 文章 ---------- */
;(m.articles || []).forEach((x, i) => {
  const body = `${x.title}。本文由${x.author || '健康管理团队'}提供，围绕${x.tag || '慢病管理'}给出可执行的日常建议：规律监测、按时用药、控制饮食中的盐与糖、保持每周 150 分钟中等强度运动。出现明显不适时请及时就医。`
  push(`INSERT INTO t_article (id,title,author,tone,audio,video,time_text,tag,content) VALUES (${q(x.id || 'ar' + (i + 1))},${q(x.title)},${q(x.author)},${q(x.tone)},${bool(x.audio)},${bool(x.video)},${q(x.time)},${q(x.tag)},${q(body)});`)
})

/* ---------- 聊天记录 ---------- */
;(m.chatSeed || []).forEach((x, i) => push(`INSERT INTO t_chat_message (id,session_id,patient_id,role,msg_type,content,citations,created_at) VALUES (${q(x.id || 'c' + (i + 1))},${q('s001')},${q('1')},${q(x.role)},${q(x.type)},${q(x.text)},${q('[]')},${now});`))

/* ---------- 快捷提问 ---------- */
;(m.quickQuestions || []).forEach((x, i) => push(`INSERT INTO t_quick_question (id,question,sort_no) VALUES (${i + 1},${q(x)},${i + 1});`))

/* ---------- 设备 ---------- */
;(m.devices || []).forEach((x, i) => push(`INSERT INTO t_device (id,patient_id,name,model,online,battery,sync_at,types) VALUES (${q(x.id || 'd' + (i + 1))},${q('1')},${q(x.name)},${q(x.model)},${bool(x.online)},${num(x.battery)},${q(x.syncAt)},${q(JSON.stringify(x.types || []))});`))

/* ---------- 电子病历 ---------- */
if (m.emr) {
  push(`INSERT INTO t_emr (id,patient_id,archive_no,payload) VALUES (${q('e1')},${q('1')},${q(m.emr.archiveNo)},${q(JSON.stringify(m.emr))});`)
}

/* ---------- 健康评分明细 ---------- */
if (m.scoreDetail) {
  push(`INSERT INTO t_score_detail (patient_id,payload) VALUES (${q('1')},${q(JSON.stringify(m.scoreDetail))});`)
}

/* ---------- 远程医疗 ---------- */
push(`INSERT INTO t_remote_medical (id,payload) VALUES (${q('rm1')},${q(JSON.stringify(m.remoteMedical || {}))});`)

push(`INSERT INTO t_hospital (id,name,level,address,city) VALUES (${q('h1')},${q('贵州省人民医院')},${q('三级甲等')},${q('贵阳市南明区中山东路83号')},${q('贵阳')});`)
push(`INSERT INTO t_hospital (id,name,level,address,city) VALUES (${q('h2')},${q('贵阳市第一人民医院')},${q('三级甲等')},${q('贵阳市南明区博爱路97号')},${q('贵阳')});`)
push(`INSERT INTO t_doctor (id,hospital_id,name,title,dept,fee,avatar) VALUES (${q('doc1')},${q('h1')},${q('王志强')},${q('主任医师')},${q('心内科')},${num(30)},${q('')});`)
push(`INSERT INTO t_doctor (id,hospital_id,name,title,dept,fee,avatar) VALUES (${q('doc2')},${q('h1')},${q('周慧敏')},${q('副主任医师')},${q('内分泌科')},${num(25)},${q('')});`)
push(`INSERT INTO t_doctor (id,hospital_id,name,title,dept,fee,avatar) VALUES (${q('doc3')},${q('h2')},${q('陈立')},${q('主任医师')},${q('呼吸与危重症医学科')},${num(30)},${q('')});`)

fs.writeFileSync(out, L.join('\n') + '\n', 'utf8')
console.log('written: ' + out + '  lines=' + L.length)
