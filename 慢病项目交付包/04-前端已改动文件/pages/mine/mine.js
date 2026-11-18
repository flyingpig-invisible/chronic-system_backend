const app = getApp()
const mock = require('../../utils/mock.js')
const util = require('../../utils/util.js')
const api = require('../../utils/api.js')

const TAB_PAGES = ['/pages/index/index', '/pages/monitor/monitor', '/pages/alert/alert', '/pages/guide/guide', '/pages/mine/mine']

Page({
  data: {
    icons: mock.icons,
    user: mock.patient,
    risk: mock.risk,
    score: mock.scoreDetail,
    groups: [
      {
        title: '我的健康',
        items: [
          { id: 'emr', icon: 'doc', name: '电子健康档案', desc: '诊断 · 用药 · 检查', url: '/pages/emr/emr' },
          { id: 'device', icon: 'watch', name: '我的设备', desc: '3 台已绑定', url: '/pages/device/device' },
          { id: 'med', icon: 'pill', name: '用药提醒', desc: '今日 3 次，已打卡 1 次', url: '/pages/medication/medication' },
          { id: 'plan', icon: 'calendar', name: '健康管理计划', desc: '饮食 / 运动 各 1 项', url: '/pages/plan/plan' }
        ]
      },
      {
        title: '服务与协作',
        items: [
          { id: 'family', icon: 'users', name: '家属管理', desc: '已绑定 1 位家属', url: '/pages/bind-relative/bind-relative' },
          { id: 'recipe', icon: 'food', name: '食谱推荐', desc: '低盐低糖 · 每日更新', url: '/pages/recipe/recipe' },
          { id: 'remote', icon: 'hospital', name: '异地就医', desc: '备案 · 长处方 · 结算', url: '/pages/telemedicine/telemedicine' },
          { id: 'chat', icon: 'mind', name: '健康小管家', desc: 'AI 问答 · 症状自查', url: '/pages/chat/chat' }
        ]
      },
      {
        title: '设置',
        items: [
          { id: 'role', icon: 'switch', name: '身份切换', desc: '当前：患者', url: '/pages/role-switch/role-switch' },
          { id: 'alert', icon: 'bell', name: '预警推送设置', desc: '高危实时 · 中危通知', action: 'alert' },
          { id: 'big', icon: 'info', name: '大字模式', desc: '适老化阅读', action: 'big' }
        ]
      }
    ],
    bigFont: false
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 4 })
    }
    this.loadData()
  },

  /* 档案与健康评分：接口优先，失败保留 mock */
  loadData() {
    const self = this
    api.getProfile().then(function (u) {
      if (u) self.setData({ user: u })
    }).catch(function () {})
    api.getScoreDetail().then(function (s) {
      if (s && s.score !== undefined) self.setData({ score: s })
    }).catch(function () {})
  },

  go(e) {
    const url = e.currentTarget.dataset.url
    if (!url) return
    if (TAB_PAGES.indexOf(url) >= 0) wx.switchTab({ url: url })
    else wx.navigateTo({ url: url })
  },

  onItem(e) {
    const id = e.currentTarget.dataset.id
    const url = e.currentTarget.dataset.url
    if (id === 'alert') {
      wx.showModal({
        title: '预警推送设置',
        content: '高危：实时语音 + 电话通知\n中危：微信服务通知\n低危：站内提醒\n\n可在微信「设置 - 订阅消息」中管理。',
        showCancel: false,
        confirmText: '知道了'
      })
      return
    }
    if (id === 'big') {
      const big = !this.data.bigFont
      this.setData({ bigFont: big })
      util.toast(big ? '已开启大字模式' : '已关闭大字模式')
      return
    }
    if (url) {
      if (TAB_PAGES.indexOf(url) >= 0) wx.switchTab({ url: url })
      else wx.navigateTo({ url: url })
    }
  },

  onAvatar() {
    util.toast('资料完善度 85%，补齐过敏史可提升至 100%')
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后需重新登录才能查看健康数据',
      confirmColor: '#F04438',
      success(res) {
        if (!res.confirm) return
        app.logout()
        wx.redirectTo({ url: '/pages/login/login' })
      }
    })
  }
})
