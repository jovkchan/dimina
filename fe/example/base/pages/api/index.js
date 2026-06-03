Page({
  data: {
    // scanCode 扫码结果展示
    scanStatus: '',
    scanResultText: '',
    scanType: '',
    scanCharSet: '',
    scanSource: '',
    scanErrMsg: '',
    scanBatch: false,
    continuousResults: [],
  },

  openSystemBluetoothSetting: function () {
    wx.openSystemBluetoothSetting({
      success(res) {
        console.log(res)
      }
    })
  },
  getMenuButtonBoundingClientRect: function() {
    const res = wx.getMenuButtonBoundingClientRect()
    console.log(res.width)
    console.log(res.height)
    console.log(res.top)
    console.log(res.right)
    console.log(res.bottom)
    console.log(res.left)
  },
  reLaunch: function() {
    wx.reLaunch({
      url: '/pages/scroll-view/index'
    })
  },
  setNavigationBarTitle: function() {
    wx.setNavigationBarTitle({
      title: '当前页面'
    })
  },
  setNavigationBarColor: function() {
    wx.setNavigationBarColor({
      frontColor: '#ffffff',
      backgroundColor: '#ff0000',
      animation: {
        duration: 400,
        timingFunc: 'easeIn'
      }
    })
  },
  getWindowInfo: function () {
    const windowInfo = wx.getWindowInfo()

    console.log(windowInfo.pixelRatio)
    console.log(windowInfo.screenWidth)
    console.log(windowInfo.screenHeight)
    console.log(windowInfo.windowWidth)
    console.log(windowInfo.windowHeight)
    console.log(windowInfo.statusBarHeight)
    console.log(windowInfo.safeArea)
    console.log(windowInfo.screenTop)
  },
  getSystemSetting: function () {
    const systemSetting = wx.getSystemSetting()

    console.log(systemSetting.bluetoothEnabled)
    console.log(systemSetting.deviceOrientation)
    console.log(systemSetting.locationEnabled)
    console.log(systemSetting.wifiEnabled)
  },
  getSystemInfoSync: function () {
    const res = wx.getSystemInfoSync()
    console.log(res.model)
    console.log(res.pixelRatio)
    console.log(res.windowWidth)
    console.log(res.windowHeight)
    console.log(res.language)
    console.log(res.version)
    console.log(res.platform)
  },
  getSystemInfoAsync: function () {
    wx.getSystemInfoAsync({
      success(res) {
        console.log(res.model)
        console.log(res.pixelRatio)
        console.log(res.windowWidth)
        console.log(res.windowHeight)
        console.log(res.language)
        console.log(res.version)
        console.log(res.platform)
      }
    })
  },
  getSystemInfo: async function () {
    const result = await wx.getSystemInfo()
    console.log(result)
    wx.getSystemInfo({
      success(res) {
        console.log(res.model)
        console.log(res.pixelRatio)
        console.log(res.windowWidth)
        console.log(res.windowHeight)
        console.log(res.language)
        console.log(res.version)
        console.log(res.platform)
      }
    })
  },

  getStorage: function () {
    wx.setStorage({
      key: "key",
      data: "value",
      success() {
        wx.getStorage({
          key: "key",
          success(res) {
            console.log('getStorage', res.data)
          }
        })
      }
    })
  },
  setStorageSync: function () {
    wx.setStorageSync('key', 'value')
  },
  getStorageSync: function () {
    var value = wx.getStorageSync('key')
    console.log('getStorageSync', value);
  },
  removeStorageSync: function () {
    wx.removeStorageSync('key')
  },
  clearStorageSync: function () {
    wx.clearStorageSync()
  },
  setStorage: function () {
    wx.setStorage({
      key: "key",
      data: "value"
    })
  },
  getStorage: function () {
    wx.getStorage({
      key: 'key',
      success(res) {
        console.log(res.data)
      }
    })
  },
  removeStorage: function () {
    wx.removeStorage({
      key: 'key',
      success(res) {
        console.log(res)
      }
    })
  },
  clearStorage: function () {
    wx.clearStorage()
  },
  getStorageInfoSync: function () {
    const res = wx.getStorageInfoSync()
    console.log(res.keys)
    console.log(res.currentSize)
    console.log(res.limitSize)
  },
  getStorageInfo: function () {
    wx.getStorageInfo({
      success(res) {
        console.log(res.keys)
        console.log(res.currentSize)
        console.log(res.limitSize)
      }
    })
  },
  getNetworkType: function () {
    wx.getNetworkType({
      success(res) {
        const networkType = res.networkType;
        console.log('networkType', networkType);
      }
    });
  },
  startLocationUpdate: function () {
    wx.startLocationUpdate({
      type: 'gcj02',
      success: function (res) {
        console.log('startLocationUpdate success', res);
      },
      fail: function (res) {
        console.log('startLocationUpdate fail', res);
      },
      complete: function (res) {
        console.log('startLocationUpdate complete', res);
      },
    })
  },
  stopLocationUpdate: function () {
    wx.stopLocationUpdate({
      success: function (res) {
        console.log('stopLocationUpdate success', res);
      },
      fail: function (res) {
        console.log('stopLocationUpdate fail', res);
      },
      complete: function (res) {
        console.log('stopLocationUpdate complete', res);
      },
    })
  },
  showToast: function () {
    wx.showToast({
      title: '成功',
      icon: 'success',
      duration: 2000
    })
  },
  hideToast: function() {
    wx.hideToast()
  },
  showLoading: function () {
    wx.showLoading({
      title: '加载中',
    })
  },
  showModal: function () {
    wx.showModal({
      title: '提示',
      content: '这是一个模态弹窗',
      success(res) {
        console.log(res)
        if (res.confirm) {
          console.log('用户点击确定')
        } else if (res.cancel) {
          console.log('用户点击取消')
        }
      }
    })
  },
  pageScrollTo: function () {
    wx.pageScrollTo({
      scrollTop: 0,
      duration: 300
    })
  },
  request: function () {
    wx.request({
      url: 'https://suggest.taobao.com/sug?code=utf-8&q=%E7%9B%B8%E6%9C%BA',
      success(res) {
        console.log('get success', res);
      },
      fail(res) {
        console.log('get fail', res);
      },
      complete() {
        console.log('get complete')
      }
    });
    wx.request({
      url: 'http://httpbin.org/get',
      data: {name: 'John', age: 30},
      success(res) {
        console.log('get data success', typeof res, res);
      },
      fail(res) {
        console.log('get data fail', res);
      },
      complete() {
        console.log('get data complete')
      }
    });
    
    // POST request to httpbin.org
    wx.request({
      url: 'http://httpbin.org/post',
      method: 'POST',
      data: {
        name: 'John',
        age: 30,
        city: 'New York'
      },
      header: {
        'content-type': 'application/json'
      },
      success(res) {
        console.log('post success', res);
      },
      fail(res) {
        console.log('post fail', res);
      },
      complete() {
        console.log('post complete')
      }
    });
    // array
    wx.request({
      url: 'https://picsum.photos/v2/list?page=1&limit=20',
      success(res) {
        console.log('get array success', res);
      },
      fail(res) {
        console.log('get fail', res);
      },
    })
  },
  downloadFile: function () {
    wx.downloadFile({
      url: 'https://picsum.photos/200/200',
      success(res) {
        console.log(res.tempFilePath)
      }
    })
  },
  uploadFile: function () {
    wx.chooseImage({
      success(res) {
        const tempFilePaths = res.tempFilePaths
        wx.uploadFile({
          url: 'https://example.weixin.qq.com/upload',
          filePath: tempFilePaths[0],
          name: 'file',
          formData: {
            'user': 'test'
          },
          success(res) {
            console.log(res.data)
          }
        })
      }
    })
  },
  setClipboardData: function () {
    wx.setClipboardData({
      data: 'data',
      success(res) {
        console.log(res)
      }
    })
  },
  getClipboardData: function () {
    wx.getClipboardData({
      success(res) {
        console.log(res.data)
      }
    })
  },
  makePhoneCall: function () {
    wx.makePhoneCall({
      phoneNumber: '1340000'
    })
  },
  chooseContact: function() {
    wx.chooseContact({
      success(res) {
        console.log(res)
      },
      fail(res) {
        console.log(res)
      },
      complete() {
        console.log("complete")
      }
    })
  },
  addPhoneContact: function() {
    wx.addPhoneContact({
      firstName: 'firstName',
      mobilePhoneNumber: '12306'
    })
  },
  previewImage: function() {
    wx.previewImage({
      urls: ["https://picsum.photos/200/200","https://picsum.photos/800/600"],
    })
  },
  chooseImage: function() {
    wx.chooseImage({
      count: 2,
      sizeType: ['original', 'compressed'],
      sourceType: ['album', 'camera'],
      success (res) {
        const tempFilePaths = res.tempFilePaths
        wx.saveImageToPhotosAlbum({
          filePath: tempFilePaths[0],
          success(res) {
            console.log('save', res)
           }
        })

        wx.compressImage({
          src: tempFilePaths[0],
          quality: 80,
          success(res) {
            console.log('compress', res)
          }
        })
      }
    })
  },
  chooseMedia: function() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image','video'],
      sourceType: ['album', 'camera'],
      maxDuration: 30,
      camera: 'back',
      success(res) {
        console.log(res.tempFiles[0].tempFilePath)
        console.log(res.tempFiles[0].size)
      }
    })
  },
  vibrateLong: function() {
    wx.vibrateLong({})
  },
  vibrateShort: function() {
    wx.vibrateShort()
  },
  showActionSheet: function() {
    wx.showActionSheet({
      itemList: ['A', 'B', 'C'],
      success (res) {
        console.log(res.tapIndex)
      },
      fail (res) {
        console.log(res.errMsg)
      }
    })
  },
  // ============================================================
  // scanCode 扫码 — 功能全覆盖演示
  // ============================================================
  // 所有结果回填到页面下方「扫码结果演示」区域，方便对照各参数效果

  /**
   * 基础扫码 — 使用默认 UI，演示完整返回数据
   */
  scanCodeBasic: function () {
    const that = this
    wx.scanCode({
      success(res) {
        console.log('[scanCode] result:', res.result)
        console.log('[scanCode] scanType:', res.scanType)
        console.log('[scanCode] charSet:', res.charSet)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: '相机扫码（默认UI）',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
          scanResultText: '',
          scanType: '',
          scanCharSet: '',
        })
      },
    })
  },

  /**
   * 自定义 UI 扫码 — 演示标题、颜色、方形扫描框等参数定制
   */
  scanCodeCustomUI: function () {
    const that = this
    wx.scanCode({
      title: '仓库扫码',
      titleColor: '#FF6B35',
      hint: '请将货物条码对准相机',
      hintColor: '#FFD6C2',
      frameColor: '#FF6B35',
      cornerColor: '#FF6B35',
      frameShape: 'square',
      albumText: '选图识别',
      albumTextColor: '#FF6B35',
      backText: '取消',
      backTextColor: '#FFFFFF',
      success(res) {
        console.log('[scanCode-custom] result:', res.result)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: '方形框（橙色主题）',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
          scanResultText: '',
          scanType: '',
          scanCharSet: '',
        })
      },
    })
  },

  /**
   * 圆形扫描框 — frameShape='circle'，相机≤50%，下半部分展示结果
   */
  scanCodeCircleFrame: function () {
    const that = this
    wx.scanCode({
      title: '圆形扫码',
      hint: '圆形扫描区域',
      frameShape: 'circle',
      frameColor: '#FF6B35',
      cornerColor: '#FF6B35',
      onlyFromCamera: true,
      success(res) {
        console.log('[scanCode-circle] result:', res.result)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: '圆形框 ⭕',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
          scanResultText: '',
          scanType: '',
          scanCharSet: '',
        })
      },
    })
  },

  /**
   * 仅相机扫码 — onlyFromCamera=true，禁止从相册选图
   */
  scanCodeOnlyCamera: function () {
    const that = this
    wx.scanCode({
      title: '仅相机',
      onlyFromCamera: true,
      hint: '无法从相册选图',
      success(res) {
        console.log('[scanCode-onlyCamera] result:', res.result)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: '仅相机（无相册入口）',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
        })
      },
    })
  },

  /**
   * 含相册选图扫码 — onlyFromCamera=false，可从相册选择图片识别条码
   */
  scanCodeWithAlbum: function () {
    const that = this
    wx.scanCode({
      title: '扫码（含相册）',
      onlyFromCamera: false,
      albumText: '从相册选择条码图片',
      success(res) {
        console.log('[scanCode-album] result:', res.result)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: '含相册选图',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
        })
      },
    })
  },

  /**
   * 连续扫码 — continuous=true，同一码只记录一次，点"完成"返回全部结果
   */
  scanCodeContinuous: function () {
    const that = this
    wx.scanCode({
      title: '连续扫码',
      continuous: true,
      hint: '扫码后自动继续，无需手动操作',
      continuousHint: '已识别 %d 个，继续扫码中…',
      finishText: '完成 (%d)',
      onlyFromCamera: true,
      success(res) {
        if (res.batch) {
          // 连续扫码批量结果
          const list = res.result
          that.setData({
            scanStatus: 'ok',
            scanBatch: true,
            scanResultText: '共 ' + list.length + ' 个条码',
            scanType: 'BATCH',
            scanCharSet: res.charSet,
            scanSource: '连续扫码',
            scanErrMsg: '',
            continuousResults: list.map(function (item) {
              return { result: item.result, scanType: item.scanType }
            }),
          })
        } else {
          // 单次结果
          that.setData({
            scanStatus: 'ok',
            scanResultText: res.result,
            scanType: res.scanType,
            scanCharSet: res.charSet,
            scanSource: '连续扫码（单次）',
            scanErrMsg: '',
          })
        }
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
        })
      },
    })
  },

  /**
   * SVG 自定义扫描框
   *
   * SDK 不渲染任何扫描框，前端在 WXML 中自由绘制 SVG 覆盖层。
   * 这里演示狐狸剪影，你可以换成：圆形、星形、品牌 Logo 轮廓……
   */
  scanCodeSvgFrame: function () {
    const that = this
    const foxSVG = `<svg viewBox="0 0 400 400">
  <defs>
    <mask id="hole">
      <rect width="400" height="400" fill="white"/>
      <path d="M200,80 C120,80 40,140 40,220 C40,300 100,340 160,340
               L160,280 C160,260 180,250 200,250 C220,250 240,260 240,280
               L240,340 C300,340 360,300 360,220 C360,140 280,80 200,80 Z
               M130,180 A15,15 0 1,1 130,179
               M270,180 A15,15 0 1,1 270,179"
            fill="black"/>
      <polygon points="120,140 100,60 160,110" fill="black"/>
      <polygon points="280,140 300,60 240,110" fill="black"/>
    </mask>
  </defs>
  <rect width="400" height="400" fill="rgba(0,0,0,0.7)" mask="url(#hole)"/>
  <circle cx="130" cy="180" r="4" fill="#FF6B35"/>
  <circle cx="270" cy="180" r="4" fill="#FF6B35"/>
  <path d="M200,80 C120,80 40,140 40,220 C40,300 100,340 160,340
           L160,280 C160,260 180,250 200,250 C220,250 240,260 240,280
           L240,340 C300,340 360,300 360,220 C360,140 280,80 200,80 Z"
        fill="none" stroke="#FF6B35" stroke-width="3" stroke-dasharray="8,4"/>
</svg>`

    wx.scanCode({
      title: 'SVG 自定义框',
      hint: '狐狸剪影扫描框 🦊',
      frameShape: 'svg',
      frameSvg: foxSVG,
      onlyFromCamera: true,
      success(res) {
        console.log('[scanCode-svg] result:', res.result)
        that.setData({
          scanStatus: 'ok',
          scanResultText: res.result,
          scanType: res.scanType,
          scanCharSet: res.charSet,
          scanSource: 'SVG 狐狸框 🦊',
          scanErrMsg: '',
        })
      },
      fail(err) {
        that.setData({
          scanStatus: 'fail',
          scanErrMsg: err.errMsg,
        })
      },
    })
  },
});