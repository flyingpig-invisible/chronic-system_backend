const app = getApp()
const icons = require('../../utils/icons.js')
const util = require('../../utils/util.js')
const api = require('../../utils/api.js')

Page({
  data: {
    icons: icons,
    phone: '',
    pwd: '',
    showPwd: false,
    agreed: true,
    canSubmit: false
  },

  onPhone(e) {
    const phone = e.detail.value
    this.setData({ phone: phone })
    this.check()
  },

  onPwd(e) {
    this.setData({ pwd: e.detail.value })
    this.check()
  },

  check() {
    const ok = /^1\d{10}$/.test(this.data.phone) && this.data.pwd.length >= 6
    if (ok !== this.data.canSubmit) this.setData({ canSubmit: ok })
  },

  togglePwd() {
    this.setData({ showPwd: !this.data.showPwd })
  },

  toggleAgree() {
    this.setData({ agreed: !this.data.agreed })
  },

  onLogin() {
    if (!this.data.canSubmit) {
      util.toast('请输入 11 位手机号和至少 6 位密码')
      return
    }
    if (!this.data.agreed) {
      util.toast('请先阅读并同意用户协议')
      return
    }
    const self = this
    api.login({ phone: self.data.phone, password: self.data.pwd, role: 'patient' })
      .then(function (d) {
        if (d && d.token) wx.setStorageSync('token', d.token)
        if (d && d.userId) wx.setStorageSync('uid', d.userId)
        app.setUser({ phone: self.data.phone, name: '李秀兰' })
        wx.navigateTo({ url: '/pages/role/role' })
      })
      .catch(function (e) {
        util.toast((e && e.msg) || '登录失败，请检查手机号或密码')
      })
  },

  onWechat() {
    util.loading('微信授权中')
    const self = this
    setTimeout(function () {
      util.hideLoading()
      app.setUser({ phone: '13800136688', name: '李秀兰' })
      wx.navigateTo({ url: '/pages/role/role' })
    }, 600)
  },

  goForgot() {
    wx.navigateTo({ url: '/pages/forgot/forgot' })
  },

  goRegister() {
    wx.navigateTo({ url: '/pages/register/register' })
  },

  onAgreement() {
    wx.showModal({
      title: '用户协议与隐私政策',
      content: '本平台仅提供慢性病辅助管理服务，所有指标与建议不作为诊断依据，如有不适请及时就医。',
      showCancel: false,
      confirmText: '我知道了'
    })
  }
})
