/* utils/request.js —— 前端唯一网络出口
 * 放置位置：patient-miniprogram/utils/request.js
 *
 * 用法：
 *   const api = require('../../utils/api.js')
 *   const data = await api.getHomeSummary()
 *
 * 联调开关：把 USE_MOCK 改成 false，再填 BASE_URL，页面代码一行都不用动。
 */

const CONFIG = {
  env: 'dev',                     // dev | prod
  baseUrl: {
    dev: 'http://127.0.0.1:8080/api',     // 本机后端；真机联调换成电脑局域网 IP
    prod: 'https://api.example.com/api'
  },
  useMock: false,                 // false = 走真实接口；true = 只用本地假数据
  timeout: 10000
}

const BASE_URL = CONFIG.baseUrl[CONFIG.env]

function getToken() {
  try { return wx.getStorageSync('token') || '' } catch (e) { return '' }
}

function toLogin() {
  try {
    const pages = getCurrentPages()
    const cur = pages[pages.length - 1] || {}
    if (cur.route && cur.route.indexOf('login') > -1) return
  } catch (e) {}
  wx.reLaunch({ url: '/pages/login/login' })
}

/* 后端约定：code === 0 为成功；401 未登录 */
function request(options) {
  const opts = options || {}
  const url = opts.url
  const method = opts.method || 'GET'
  const data = opts.data || {}
  const loading = opts.loading === true

  // ---------- Mock 分支：后端未就绪时用 ----------
  if (CONFIG.useMock) {
    if (typeof opts.mock === 'function') {
      return new Promise(function (resolve) {
        setTimeout(function () { resolve(opts.mock()) }, 120)
      })
    }
    return Promise.reject(new Error('[mock] 未提供 mock 数据: ' + url))
  }

  // ---------- 真实请求 ----------
  if (loading) wx.showLoading({ title: '加载中', mask: true })

  return new Promise(function (resolve, reject) {
    const header = { 'Content-Type': 'application/json' }
    const token = getToken()
    if (token) header.Authorization = 'Bearer ' + token

    wx.request({
      url: BASE_URL + url,
      method: method,
      data: data,
      timeout: CONFIG.timeout,
      header: header,
      success: function (res) {
        const body = res.data || {}

        if (res.statusCode === 401 || body.code === 401) {
          wx.removeStorageSync('token')
          toLogin()
          reject(new Error('未登录'))
          return
        }
        if (res.statusCode !== 200) {
          wx.showToast({ title: 'HTTP ' + res.statusCode, icon: 'none' })
          reject(new Error('HTTP ' + res.statusCode))
          return
        }
        // 兼容 code===0 / code===200 两种约定，按后端最终定的那一种收紧
        if (body.code !== 0 && body.code !== 200) {
          wx.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(body)
          return
        }
        resolve(body.data)
      },
      fail: function (err) {
        wx.showToast({ title: '网络异常，请检查网络', icon: 'none' })
        reject(err)
      },
      complete: function () {
        if (loading) wx.hideLoading()
      }
    })
  })
}

module.exports = {
  request: request,
  get: function (url, data, opts) {
    return request(Object.assign({ url: url, method: 'GET', data: data }, opts || {}))
  },
  post: function (url, data, opts) {
    return request(Object.assign({ url: url, method: 'POST', data: data }, opts || {}))
  },
  put: function (url, data, opts) {
    return request(Object.assign({ url: url, method: 'PUT', data: data }, opts || {}))
  },
  del: function (url, data, opts) {
    return request(Object.assign({ url: url, method: 'DELETE', data: data }, opts || {}))
  },
  config: CONFIG
}
