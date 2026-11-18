/* utils/api.js —— 所有接口按模块集中在这里
 * 放置位置：patient-miniprogram/utils/api.js
 *
 * 每个方法都自带 mock 兜底：request.js 里 useMock=true 时返回 mock 数据，
 * 改成 false 后自动走真实接口，页面代码一个字都不用动。
 *
 * 页面改造方式：
 *   删除  const mock = require('../../utils/mock.js')
 *   新增  const api = require('../../utils/api.js')
 *   把 onLoad 里的同步取数改成 async/await
 */

const req = require('./request.js')
const mock = require('./mock.js')

const list = function (v) { return v || [] }

const api = {

  /* ============ 认证与角色 ============ */
  login: function (data) {
    return req.post('/auth/login', data, {
      loading: true,
      mock: function () {
        return { token: 'mock-token', userId: '1856230144770013', role: 'patient', roles: list(mock.roles) }
      }
    })
  },
  register: function (data) {
    return req.post('/auth/register', data, { mock: function () { return { userId: '1856230144770013' } } })
  },
  sendSmsCode: function (phone, scene) {
    return req.post('/auth/sms/code', { phone: phone, scene: scene }, { mock: function () { return { sent: true } } })
  },
  resetPassword: function (data) {
    return req.post('/auth/password/forgot', data, { mock: function () { return { ok: true } } })
  },
  getRoles: function () {
    return req.get('/auth/roles', {}, { mock: function () { return list(mock.roles) } })
  },
  switchRole: function (role) {
    return req.post('/auth/role/switch', { role: role }, { mock: function () { return { token: 'mock-token', role: role } } })
  },

  /* ============ 患者档案 ============ */
  getProfile: function () {
    return req.get('/patient/profile', {}, { mock: function () { return mock.patient } })
  },
  updateProfile: function (data) {
    return req.put('/patient/profile', data, { mock: function () { return mock.patient } })
  },
  getFamily: function () {
    return req.get('/patient/family', {}, { mock: function () { return list(mock.patient && mock.patient.family) } })
  },
  bindFamily: function (data) {
    return req.post('/patient/family/bind', data, { mock: function () { return { ok: true } } })
  },
  unbindFamily: function (id) {
    return req.del('/patient/family/' + id, {}, { mock: function () { return { ok: true } } })
  },
  getScoreDetail: function () {
    return req.get('/patient/score-detail', {}, {
      mock: function () { return mock.scoreDetail || { score: 0, items: [] } }
    })
  },

  /* ============ 首页聚合 ============ */
  getHomeSummary: function () {
    return req.get('/home/summary', {}, {
      mock: function () {
        return {
          patient: mock.patient,
          risk: mock.risk,
          todayMetrics: list(mock.todayMetrics),
          todayTasks: list(mock.todayTasks),
          quickActions: list(mock.quickActions)
        }
      }
    })
  },

  /* ============ 监测 ============ */
  getTrends: function (type, range) {
    return req.get('/monitor/trends', { type: type, range: range }, {
      mock: function () {
        const t = (mock.trends || {})[type] || {}
        return Object.assign({ type: type, range: range }, t, { labels: list(mock.trendDays) })
      }
    })
  },
  getRecords: function (params) {
    return req.get('/monitor/records', params || {}, {
      mock: function () { return { total: list(mock.records).length, list: list(mock.records) } }
    })
  },
  addRecord: function (data) {
    return req.post('/monitor/records', data, { mock: function () { return { id: 'r' + Date.now() } } })
  },
  getDiseaseTabs: function () {
    return req.get('/monitor/disease-tabs', {}, { mock: function () { return list(mock.diseaseTabs) } })
  },

  /* ============ 预警 ============ */
  getAlertSummary: function () {
    return req.get('/alert/summary', {}, { mock: function () { return mock.alertSummary || { high: 0, mid: 0, low: 0 } } })
  },
  getAlerts: function (params) {
    return req.get('/alert/list', params || {}, {
      mock: function () { return { total: list(mock.alerts).length, list: list(mock.alerts) } }
    })
  },
  readAlert: function (id) {
    return req.put('/alert/' + id + '/read', {}, { mock: function () { return { ok: true } } })
  },

  /* ============ 用药与计划 ============ */
  getMedications: function () {
    return req.get('/medication/list', {}, { mock: function () { return list(mock.medications) } })
  },
  checkinMedication: function (data) {
    return req.post('/medication/checkin', data, { mock: function () { return { ok: true } } })
  },
  getTodayPlan: function () {
    return req.get('/plan/today', {}, {
      mock: function () { return { tasks: list(mock.todayTasks), doctorPlans: list(mock.doctorPlans) } }
    })
  },
  updateTaskStatus: function (id, status) {
    return req.put('/plan/task/' + id + '/status', { status: status }, { mock: function () { return { ok: true } } })
  },

  /* ============ 食谱 / 指导 / 文章 ============ */
  getRecipes: function (params) {
    return req.get('/recipe/list', params || {}, {
      mock: function () { return { total: list(mock.recipes).length, list: list(mock.recipes) } }
    })
  },
  getRecipeDetail: function (id) {
    return req.get('/recipe/' + id, {}, {
      mock: function () { return list(mock.recipes).filter(function (r) { return String(r.id) === String(id) })[0] || null }
    })
  },
  getGuideCategories: function () {
    return req.get('/guide/categories', {}, { mock: function () { return list(mock.guideCategories) } })
  },
  getGuides: function (params) {
    return req.get('/guide/list', params || {}, { mock: function () { return list(mock.recommendGuides) } })
  },
  getArticles: function (params) {
    return req.get('/article/list', params || {}, {
      mock: function () { return { total: list(mock.articles).length, list: list(mock.articles) } }
    })
  },
  getArticleDetail: function (id) {
    return req.get('/article/' + id, {}, {
      mock: function () { return list(mock.articles).filter(function (a) { return String(a.id) === String(id) })[0] || null }
    })
  },

  /* ============ 设备 / 病历 ============ */
  getDevices: function () {
    return req.get('/device/list', {}, { mock: function () { return list(mock.devices) } })
  },
  bindDevice: function (data) {
    return req.post('/device/bind', data, { mock: function () { return { ok: true } } })
  },
  unbindDevice: function (id) {
    return req.del('/device/' + id, {}, { mock: function () { return { ok: true } } })
  },
  getEmrList: function () {
    return req.get('/emr/list', {}, { mock: function () { return list(mock.emr) } })
  },
  uploadEmr: function (data) {
    return req.post('/emr/upload', data, { mock: function () { return { ok: true } } })
  },

  /* ============ 医生问答 / 远程医疗 ============ */
  sendQuestion: function (data) {
    return req.post('/chat/send', data, {
      mock: function () {
        const q = (data && data.question) || ''
        return {
          sessionId: (data && data.sessionId) || 's001',
          answer: '（mock 回答）关于「' + q + '」，建议先测量并记录，如持续异常请及时联系医生。',
          citations: [{ title: '中国高血压防治指南（2023）', source: '指南', url: '' }],
          safetyTip: '本回答仅供参考，不能替代医生诊断。'
        }
      }
    })
  },
  getChatHistory: function (sessionId) {
    return req.get('/chat/history', { sessionId: sessionId }, { mock: function () { return list(mock.chatSeed) } })
  },
  getQuickQuestions: function () {
    return req.get('/chat/quick-questions', {}, { mock: function () { return list(mock.quickQuestions) } })
  },
  getTelemedicine: function () {
    return req.get('/telemedicine/doctors', {}, { mock: function () { return mock.remoteMedical || {} } })
  },
  makeAppointment: function (data) {
    return req.post('/telemedicine/appointment', data, { mock: function () { return { ok: true, orderNo: 'A' + Date.now() } } })
  },

  /* ============ 公共 ============ */
  upload: function (filePath) {
    if (req.config.useMock) {
      return Promise.resolve({ url: 'https://mock.cdn/img.png', name: 'mock.png', size: 1024 })
    }
    return new Promise(function (resolve, reject) {
      wx.uploadFile({
        url: req.config.baseUrl[req.config.env] + '/common/upload',
        filePath: filePath,
        name: 'file',
        header: (function () {
          const t = wx.getStorageSync('token')
          return t ? { Authorization: 'Bearer ' + t } : {}
        })(),
        success: function (res) {
          try {
            const body = JSON.parse(res.data)
            if (body.code === 0) resolve(body.data)
            else reject(body)
          } catch (e) { reject(e) }
        },
        fail: reject
      })
    })
  }
}

module.exports = api
