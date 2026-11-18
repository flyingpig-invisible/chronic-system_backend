/* 后端自测脚本：node smoke.js
 * 逐个打一遍契约里的接口，打印 code 与关键字段，红色即失败。
 */
const BASE = process.env.BASE || 'http://127.0.0.1:8080/api'

let token = ''
let pass = 0
let fail = 0

async function call(method, path, body, needAuth = true) {
  const headers = { 'Content-Type': 'application/json' }
  if (needAuth && token) headers.Authorization = 'Bearer ' + token
  const res = await fetch(BASE + path, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  })
  const text = await res.text()
  let json
  try { json = JSON.parse(text) } catch (e) { json = { raw: text.slice(0, 120) } }
  return json
}

function report(label, json, probe) {
  const ok = json && json.code === 0
  let extra = ''
  try {
    if (ok && probe) extra = '  ' + probe(json.data)
  } catch (e) {
    extra = '  (probe error)'
  }
  if (ok) { pass++; console.log(`  PASS  ${label}${extra}`) }
  else { fail++; console.log(`  FAIL  ${label}  ->  ${JSON.stringify(json).slice(0, 160)}`) }
}

const len = d => (Array.isArray(d) ? `list=${d.length}` : Array.isArray(d && d.list) ? `total=${d.total}, list=${d.list.length}` : '')

;(async () => {
  console.log('=== 1. 认证 ===')
  const login = await call('POST', '/auth/login', { phone: '13800136688', password: '123456' }, false)
  report('POST /auth/login', login, d => `token=${(d.token || '').slice(0, 12)}... role=${d.role}`)
  token = login.data && login.data.token
  if (!token) { console.log('\n登录失败，后续跳过'); process.exit(1) }

  report('GET  /auth/roles', await call('GET', '/auth/roles'), len)
  report('POST /auth/role/switch', await call('POST', '/auth/role/switch', { role: 'patient' }), d => 'role=' + d.role)
  report('POST /auth/sms/code', await call('POST', '/auth/sms/code', { phone: '13800136688', scene: 'login' }, false), d => 'sent=' + d.sent)

  console.log('=== 2. 患者 ===')
  report('GET  /patient/profile', await call('GET', '/patient/profile'), d => d.name + ' / ' + (d.diseases || []).join(','))
  report('GET  /patient/family', await call('GET', '/patient/family'), len)
  report('PUT  /patient/profile', await call('PUT', '/patient/profile', { alias: '李阿姨' }), d => 'alias=' + d.alias)
  report('GET  /patient/score-detail', await call('GET', '/patient/score-detail'), d => 'score=' + d.score + ', items=' + (d.items || []).length)

  console.log('=== 3. 首页 ===')
  report('GET  /home/summary', await call('GET', '/home/summary'),
    d => `patient=${d.patient.name}, metrics=${d.todayMetrics.length}, tasks=${d.todayTasks.length}, quick=${d.quickActions.length}`)

  console.log('=== 4. 监测 ===')
  for (const t of ['bp', 'glucose', 'hr', 'copd']) {
    report(`GET  /monitor/trends?type=${t}`, await call('GET', `/monitor/trends?type=${t}&range=week`),
      d => `labels=${(d.labels || []).length}, series=${(d.series || []).length}`)
  }
  report('GET  /monitor/records', await call('GET', '/monitor/records?pageNum=1&pageSize=3'), d => `total=${d.total}, list=${d.list.length}`)
  report('POST /monitor/records', await call('POST', '/monitor/records', { type: '血压', value: '128/80', unit: 'mmHg' }), d => 'id=' + d.id)
  report('GET  /monitor/disease-tabs', await call('GET', '/monitor/disease-tabs'), len)

  console.log('=== 5. 预警 ===')
  report('GET  /alert/summary', await call('GET', '/alert/summary'), d => `high=${d.high}, mid=${d.mid}, low=${d.low}`)
  report('GET  /alert/list', await call('GET', '/alert/list?pageSize=5'), d => `total=${d.total}, list=${d.list.length}`)
  report('PUT  /alert/a1/read', await call('PUT', '/alert/a1/read', {}), d => 'ok=' + d.ok)

  console.log('=== 6. 用药与计划 ===')
  const meds = await call('GET', '/medication/list')
  report('GET  /medication/list', meds, len)
  const pendingMed = (meds.data || []).find(x => x.status !== 'taken')
  if (pendingMed) {
    report('POST /medication/checkin', await call('POST', '/medication/checkin', { medicationId: pendingMed.id, time: '12:30' }), d => 'takenAt=' + d.takenAt)
  } else {
    console.log('  SKIP  POST /medication/checkin（种子数据里已全部打卡）')
  }
  const plan = await call('GET', '/plan/today')
  report('GET  /plan/today', plan, d => `tasks=${d.tasks.length}, plans=${d.doctorPlans.length}`)
  const anyTask = (plan.data.tasks || [])[0]
  report('PUT  /plan/task/' + anyTask.id + '/status', await call('PUT', `/plan/task/${anyTask.id}/status`, { status: 'done' }), d => 'done=' + d.done)

  console.log('=== 7. 内容 ===')
  report('GET  /recipe/list', await call('GET', '/recipe/list'), d => `total=${d.total}`)
  report('GET  /recipe/f1', await call('GET', '/recipe/f1'), d => d.name + ' tags=' + (d.tags || []).join('/'))
  report('GET  /guide/categories', await call('GET', '/guide/categories'), len)
  report('GET  /guide/list', await call('GET', '/guide/list'), len)
  report('GET  /article/list', await call('GET', '/article/list'), d => `total=${d.total}`)
  report('GET  /article/g3', await call('GET', '/article/g3'), d => d.title)

  console.log('=== 8. 设备与病历 ===')
  report('GET  /device/list', await call('GET', '/device/list'), len)
  report('POST /device/bind', await call('POST', '/device/bind', { sn: 'SN-TEST-001', name: '智能手环' }), d => 'id=' + d.id)
  report('GET  /emr/list', await call('GET', '/emr/list'), d => `total=${d.total}`)

  console.log('=== 9. 问答与远程医疗 ===')
  const chat = await call('POST', '/chat/send', { sessionId: 's001', question: '我血压高怎么办' })
  report('POST /chat/send', chat, d => 'answer=' + String(d.answer).slice(0, 24) + '...')
  report('GET  /chat/history', await call('GET', '/chat/history?sessionId=s001'), len)
  report('GET  /chat/quick-questions', await call('GET', '/chat/quick-questions'), len)
  report('GET  /telemedicine/doctors', await call('GET', '/telemedicine/doctors'), d => `doctors=${(d.doctors || []).length}`)
  report('POST /telemedicine/appointment', await call('POST', '/telemedicine/appointment', { doctorId: 'doc1', slot: '2026-11-20 09:00', symptom: '血压控制不佳' }), d => 'orderNo=' + d.orderNo)

  console.log('=== 10. 公共 ===')
  report('GET  /common/dict/disease', await call('GET', '/common/dict/disease'), len)

  console.log('=== 11. 鉴权 ===')
  const noAuth = await fetch(BASE + '/home/summary').then(r => r.json())
  if (noAuth.code === 401) { pass++; console.log('  PASS  无 token 访问被拦截（401）') }
  else { fail++; console.log('  FAIL  无 token 竟然能访问：' + JSON.stringify(noAuth).slice(0, 120)) }

  console.log(`\n结果：PASS ${pass} / FAIL ${fail}`)
  process.exit(fail === 0 ? 0 : 2)
})().catch(e => { console.error('脚本异常：', e); process.exit(1) })
