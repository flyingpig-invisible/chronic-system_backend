const app = getApp()
const mock = require('../../utils/mock.js')
const util = require('../../utils/util.js')
const api = require('../../utils/api.js')

const TAB_PAGES = [
  '/pages/index/index',
  '/pages/monitor/monitor',
  '/pages/alert/alert',
  '/pages/guide/guide',
  '/pages/mine/mine'
]

Page({
  data: {
    icons: mock.icons,
    statusBarHeight: 20,
    navBarHeight: 44,
    user: mock.patient,
    risk: mock.risk,
    metrics: mock.todayMetrics,
    tasks: mock.todayTasks,
    quick: mock.quickActions,
    score: mock.scoreDetail,
    familyAvatars: [
      { tone: 'blue', icon: 'user' },
      { tone: 'orange', icon: 'users' },
      { tone: 'green', icon: 'doctor' }
    ],
    tips: [
      { id: 't1', title: '低盐饮食建议', desc: '每日盐量不超过 5g，远离腌制食品', icon: 'food', tone: 'green', url: '/pages/recipe/recipe' },
      { id: 't2', title: '餐后运动提醒', desc: '饭后 30 分钟散步 20 分钟，稳糖更有效', icon: 'run', tone: 'blue', url: '/pages/plan/plan' }
    ],
    bigFont: false,
    refreshing: false
  },

  onLoad() {
    this.setData({
      statusBarHeight: app.globalData.statusBarHeight || 20,
      navBarHeight: app.globalData.navBarHeight || 44
    })
    this.loadData()
  },

  /* 首页数据：先渲染 mock，再拉接口覆盖（接口失败不影响页面可用） */
  loadData() {
    const self = this
    return api.getHomeSummary().then(function (d) {
      if (!d) return
      const patch = {}
      if (d.patient) patch.user = d.patient
      if (d.risk) patch.risk = d.risk
      if (d.todayMetrics) patch.metrics = d.todayMetrics
      if (d.todayTasks) patch.tasks = d.todayTasks
      if (d.quickActions) patch.quick = d.quickActions
      self.setData(patch)
    }).catch(function () {
      // 后端未就绪时静默降级，保持 mock 数据
    })
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 0 })
    }
    const u = app.globalData.user
    if (u && u.name) {
      this.setData({
        'user.name': u.name,
        'user.alias': u.age ? u.name.slice(0, 1) + '阿姨' : this.data.user.alias
      })
    }
  },

  onPullDownRefresh() {
    const self = this
    this.setData({ refreshing: true })
    this.loadData().then(function () {
      wx.stopPullDownRefresh()
      self.setData({ refreshing: false })
      util.toast('数据已更新至 ' + util.fmtDate(new Date(), 'hm'))
    })
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    if (TAB_PAGES.indexOf(url) >= 0) wx.switchTab({ url: url })
    else wx.navigateTo({ url: url })
  },

  toggleFont() {
    const big = !this.data.bigFont
    this.setData({ bigFont: big })
    util.toast(big ? '已切换为大字模式' : '已恢复标准字号')
  },

  onCheckin(e) {
    const id = e.currentTarget.dataset.id
    const tasks = this.data.tasks
    for (let i = 0; i < tasks.length; i++) {
      if (tasks[i].id === id && !tasks[i].done) {
        tasks[i].done = true
        tasks[i].desc = '已于 ' + util.fmtDate(new Date(), 'hm') + ' 完成'
      }
    }
    this.setData({ tasks: tasks })
    util.toast('打卡成功，健康分 +1')
    const score = this.data.score
    score.score = Math.min(100, score.score + 1)
    this.setData({ score: score })
    api.updateTaskStatus(id, 'done').catch(function () {})
  },

  goChat() {
    wx.navigateTo({ url: '/pages/chat/chat' })
  },

  goAlert() {
    wx.switchTab({ url: '/pages/alert/alert' })
  },

  onSync() {
    util.loading('同步设备数据')
    const self = this
    setTimeout(function () {
      util.hideLoading()
      util.toast('已同步 3 台设备的最新数据')
    }, 1000)
  }
})
