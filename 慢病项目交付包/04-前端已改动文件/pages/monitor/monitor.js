const mock = require('../../utils/mock.js')
const util = require('../../utils/util.js')
const api = require('../../utils/api.js')

/* 把 7 天数据重采样为任意点数（用于日 / 月视图） */
function resample(data, n, intMode) {
  const out = []
  const len = data.length
  for (let i = 0; i < n; i++) {
    const t = (i * (len - 1)) / (n - 1)
    const i0 = Math.floor(t)
    const i1 = Math.min(len - 1, i0 + 1)
    const v = data[i0] + (data[i1] - data[i0]) * (t - i0)
    const noise = Math.sin(i * 2.3) * (intMode ? 1.6 : 0.12)
    const val = v + noise
    out.push(intMode ? Math.round(val) : Math.round(val * 10) / 10)
  }
  return out
}

function buildLabels(range) {
  if (range === 'day') return ['00', '03', '06', '09', '12', '15', '18', '21']
  if (range === 'month') return ['9/5', '9/8', '9/11', '9/14', '9/17', '9/20', '9/23', '9/26', '9/29', '10/2']
  return ['9/28', '9/29', '9/30', '10/1', '10/2', '10/3', '10/4']
}

/* 根据阈值找出异常点 */
function findAbnormal(key, series, target, mode) {
  const out = []
  const first = series[0].data
  for (let i = 0; i < first.length; i++) {
    if (mode === 'low') {
      if (first[i] < target) out.push(i)
    } else if (first[i] > target) {
      out.push(i)
    }
  }
  return out.slice(0, 3)
}

Page({
  data: {
    icons: mock.icons,
    diseaseTabs: mock.diseaseTabs,
    activeKey: 'bp',
    range: 'week',
    ranges: [{ key: 'day', name: '日' }, { key: 'week', name: '周' }, { key: 'month', name: '月' }],
    chart: null,
    stats: [],
    devices: mock.devices,
    records: mock.records,
    showInput: false,
    inputSys: '',
    inputDia: ''
  },

  onLoad() {
    this.trendCache = {}
    this.render()
    this.loadTrend()
  },

  /* 拉趋势接口，写进缓存后重绘；接口失败就继续用 mock */
  loadTrend() {
    const self = this
    const key = this.data.activeKey
    const range = this.data.range
    return api.getTrends(key, range).then(function (d) {
      if (!d || !d.series) return
      self.trendCache[key] = d
      self.render()
    }).catch(function () {})
  },

  onShow() {
    if (typeof this.getTabBar === 'function' && this.getTabBar()) {
      this.getTabBar().setData({ selected: 1 })
    }
  },

  render() {
    const key = this.data.activeKey
    const range = this.data.range
    const base = (this.trendCache && this.trendCache[key]) || mock.trends[key]
    if (!base) return

    const isBp = base.series.length > 1
    const n = range === 'day' ? 8 : (range === 'month' ? 10 : 7)
    const labels = buildLabels(range)

    const series = base.series.map(function (s) {
      return {
        name: s.name,
        color: s.color,
        data: resample(s.data, n, isBp)
      }
    })

    const abnormal = range === 'week'
      ? (base.abnormal || [])
      : findAbnormal(key, series, base.target, base.targetMode)

    // 统计
    const first = series[0].data
    const avg = Math.round(first.reduce(function (a, b) { return a + b }, 0) / first.length)
    const max = Math.max.apply(null, first)
    const min = Math.min.apply(null, first)
    const rate = Math.round(((first.length - abnormal.length) / first.length) * 100)

    let avgText = String(avg)
    let maxText = String(max)
    let minText = String(min)
    if (isBp) {
      const second = series[1].data
      const avg2 = Math.round(second.reduce(function (a, b) { return a + b }, 0) / second.length)
      avgText = avg + '/' + avg2
      maxText = max + '/' + Math.max.apply(null, second)
      minText = min + '/' + Math.min.apply(null, second)
    }

    const rangeName = range === 'day' ? '近24小时' : (range === 'month' ? '近30天' : '近7天')

    const stats = [
      { label: rangeName + '平均', value: avgText, unit: base.unit },
      { label: '最高值', value: maxText, unit: base.unit, danger: true },
      { label: '最低值', value: minText, unit: base.unit },
      { label: '达标率', value: String(rate), unit: '%', ring: true, ringColor: rate >= 80 ? '#12B76A' : '#F79009' },
      { label: '预警次数', value: String(abnormal.length), unit: '次', danger: abnormal.length > 0 }
    ]

    this.setData({
      chart: {
        key: key,
        title: base.title,
        unit: base.unit,
        labels: labels,
        series: series,
        abnormal: abnormal,
        abnormalText: base.abnormalText || '异常',
        target: base.target,
        targetText: base.targetText,
        targetMode: base.targetMode || 'high',
        min: base.min,
        max: base.max,
        rangeName: rangeName
      },
      stats: stats
    })
  },

  onDisease(e) {
    this.setData({ activeKey: e.currentTarget.dataset.key })
    this.render()
    this.loadTrend()
  },

  onRange(e) {
    this.setData({ range: e.currentTarget.dataset.key })
    this.render()
    this.loadTrend()
  },

  onSync() {
    util.loading('同步中')
    setTimeout(function () {
      util.hideLoading()
      util.toast('已同步 3 台设备，数据更新至当前时刻')
    }, 1100)
  },

  /* ---------- 手动录入 ---------- */
  openInput() {
    this.setData({ showInput: true, inputSys: '', inputDia: '' })
  },
  closeInput() {
    this.setData({ showInput: false })
  },
  onSys(e) { this.setData({ inputSys: e.detail.value }) },
  onDia(e) { this.setData({ inputDia: e.detail.value }) },
  noop() {},

  saveInput() {
    const sys = Number(this.data.inputSys)
    const dia = Number(this.data.inputDia)
    if (!sys || !dia || sys < 60 || sys > 260 || dia < 30 || dia > 160) {
      util.toast('请输入有效的血压数值')
      return
    }
    const level = sys >= 180 || dia >= 110 ? 'high' : (sys >= 140 || dia >= 90 ? 'mid' : 'normal')
    const label = level === 'high' ? '极高' : (level === 'mid' ? '偏高' : '正常')
    const color = level === 'high' ? 'red' : (level === 'mid' ? 'orange' : 'green')

    const records = this.data.records.slice()
    records.unshift({
      id: 'r' + Date.now(),
      type: '血压',
      value: sys + '/' + dia,
      unit: 'mmHg',
      time: '刚刚',
      status: level,
      label: label,
      color: color
    })
    this.setData({ records: records, showInput: false })
    util.toast('已记录本次血压 ' + sys + '/' + dia + ' mmHg')
    api.addRecord({ type: '血压', value: sys + '/' + dia, unit: 'mmHg' }).catch(function () {})
  },

  onRecord(e) {
    const id = e.currentTarget.dataset.id
    const item = this.data.records.filter(function (r) { return r.id === id })[0]
    if (!item) return
    wx.showModal({
      title: item.type + ' ' + item.value + ' ' + item.unit,
      content: '测量时间：' + item.time + '\n判定结果：' + item.label + '\n\n本结果由规则引擎自动判定，仅供参考。',
      showCancel: false,
      confirmText: '知道了'
    })
  },

  goDevice() {
    wx.navigateTo({ url: '/pages/device/device' })
  },

  onBluetooth() {
    util.toast('正在搜索附近的蓝牙设备…')
  },
  saveInputNoop() {}
})
