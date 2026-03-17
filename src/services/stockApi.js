// 股票数据获取服务
// 使用腾讯股票API获取实时行情数据

const STOCK_API_URL = 'https://qt.gtimg.cn/q'

// 使用新浪财经API获取历史数据
const SINA_API_URL = 'https://quotes.sina.cn/cn/api/quotes.php'

/**
 * 获取股票价格
 * @param {string} code - 股票代码（带前缀，如 sh600519, sz000001, hk00700）
 * @returns {Promise<{name: string, price: number}>}
 */
export async function getStockPrice(code) {
  try {
    // 使用cors-anywhere或其他代理服务解决跨域问题
    // 这里使用直接请求，如果遇到跨域问题需要在vite配置中设置代理
    const response = await fetch(`${STOCK_API_URL}=${code}`)
    const text = await response.text()
    
    // 解析返回的数据
    // 腾讯API返回格式: v_sh600519="1~贵州茅台~600519~...~当前价格~...";
    const match = text.match(/v_[^=]+="([^"]+)"/)
    if (!match) {
      throw new Error('无法解析股票数据')
    }
    
    const data = match[1].split('~')
    
    // 数据字段索引:
    // 1: 股票名称
    // 2: 股票代码
    // 3: 当前价格
    // 4: 昨收
    // 5: 今开
    // ...
    
    return {
      name: data[1],
      code: data[2],
      price: parseFloat(data[3]),
      previousClose: parseFloat(data[4]),
      open: parseFloat(data[5]),
      high: parseFloat(data[6]),
      low: parseFloat(data[7]),
      volume: parseInt(data[8]),
      updateTime: new Date().toLocaleString()
    }
  } catch (error) {
    console.error('获取股票数据失败:', error)
    // 如果API请求失败，返回模拟数据用于演示
    return getMockStockData(code)
  }
}

/**
 * 获取多个股票价格
 * @param {string[]} codes - 股票代码数组
 * @returns {Promise<Object[]>}
 */
export async function getMultipleStockPrices(codes) {
  const results = []
  for (const code of codes) {
    const data = await getStockPrice(code)
    if (data) {
      results.push(data)
    }
  }
  return results
}

/**
 * 模拟股票数据（用于演示或API故障时）
 * @param {string} code - 股票代码
 * @returns {Object}
 */
function getMockStockData(code) {
  const mockData = {
    'sh600519': { name: '贵州茅台', basePrice: 1680 },
    'sz000001': { name: '平安银行', basePrice: 12.5 },
    'sz000858': { name: '五粮液', basePrice: 145 },
    'sh000001': { name: '上证指数', basePrice: 3050 },
    'sz399001': { name: '深证成指', basePrice: 9850 },
    'hk00700': { name: '腾讯控股', basePrice: 385 },
    'hk03690': { name: '美团-W', basePrice: 125 },
    'hk09988': { name: '阿里巴巴-SW', basePrice: 85 }
  }
  
  const mock = mockData[code] || { name: code, basePrice: 100 }
  
  // 模拟价格波动 (-5% ~ +5%)
  const fluctuation = (Math.random() - 0.5) * 0.1
  const currentPrice = mock.basePrice * (1 + fluctuation)
  
  return {
    name: mock.name,
    code: code.replace(/^(sh|sz|hk)/, ''),
    price: parseFloat(currentPrice.toFixed(2)),
    previousClose: mock.basePrice,
    open: mock.basePrice * (1 + (Math.random() - 0.5) * 0.02),
    high: currentPrice * 1.02,
    low: currentPrice * 0.98,
    volume: Math.floor(Math.random() * 1000000),
    updateTime: new Date().toLocaleString(),
    isMock: true
  }
}

/**
 * 获取股票历史日线数据（近一年）
 * @param {string} code - 股票代码（带前缀，如 sh600519, sz000001）
 * @returns {Promise<{dates: string[], prices: number[]}>}
 */
export async function getStockHistory(code) {
  try {
    // 计算近一年的日期范围
    const endDate = new Date()
    const startDate = new Date()
    startDate.setFullYear(startDate.getFullYear() - 1)
    
    const formatDate = (date) => {
      return date.toISOString().split('T')[0].replace(/-/g, '')
    }
    
    // 使用腾讯财经API获取历史数据
    // 转换为腾讯格式
    let tencentCode = code
    if (code.startsWith('sh')) {
      tencentCode = 'sh' + code.slice(2)
    } else if (code.startsWith('sz')) {
      tencentCode = 'sz' + code.slice(2)
    }
    
    // 尝试从腾讯API获取历史数据
    const url = `https://web.ifzq.gtimg.cn/appstock/app/fqkline/get?param=${tencentCode},day,,,250,qfq`
    
    const response = await fetch(url)
    const data = await response.json()
    
    if (data && data.data && data.data[tencentCode] && data.data[tencentCode].qfq) {
      const klines = data.data[tencentCode].qfq.day
      const dates = []
      const prices = []
      
      klines.forEach(item => {
        dates.push(item[0]) // 日期
        prices.push(parseFloat(item[2])) // 收盘价
      })
      
      return { dates, prices }
    }
    
    // 如果API获取失败，返回模拟数据
    return getMockHistoryData(code)
  } catch (error) {
    console.error('获取历史数据失败:', error)
    return getMockHistoryData(code)
  }
}

/**
 * 生成模拟历史数据（用于演示）
 * @param {string} code - 股票代码
 * @returns {{dates: string[], prices: number[]}}
 */
function getMockHistoryData(code) {
  const dates = []
  const prices = []
  
  // 获取基础价格
  const basePrices = {
    'sh600519': 1680,
    'sz000001': 12.5,
    'sz000858': 145,
    'sh000001': 3050,
    'sz399001': 9850,
    'hk00700': 385,
    'hk03690': 125,
    'hk09988': 85
  }
  
  const basePrice = basePrices[code] || 100
  
  // 生成近一年的交易日数据（约250个交易日）
  const today = new Date()
  let currentPrice = basePrice * 0.85 // 从较低点开始
  
  for (let i = 250; i >= 0; i--) {
    const date = new Date(today)
    date.setDate(date.getDate() - i)
    
    // 跳过周末
    if (date.getDay() === 0 || date.getDay() === 6) {
      continue
    }
    
    const dateStr = date.toISOString().split('T')[0]
    dates.push(dateStr)
    
    // 模拟价格波动 (-3% ~ +3%)
    const change = (Math.random() - 0.48) * 0.06
    currentPrice = currentPrice * (1 + change)
    prices.push(parseFloat(currentPrice.toFixed(2)))
  }
  
  return { dates, prices }
}

/**
 * 搜索股票（根据名称或代码）
 * @param {string} keyword - 搜索关键词
 * @returns {Promise<Object[]>}
 */
export async function searchStocks(keyword) {
  // 这里可以实现股票搜索功能
  // 由于腾讯API没有直接的搜索接口，这里返回一些常见的股票
  const commonStocks = [
    { code: 'sh600519', name: '贵州茅台', pinyin: 'maotai' },
    { code: 'sz000001', name: '平安银行', pinyin: 'pinganyinhang' },
    { code: 'sz000858', name: '五粮液', pinyin: 'wuliangye' },
    { code: 'sh601318', name: '中国平安', pinyin: 'pingan' },
    { code: 'sh600036', name: '招商银行', pinyin: 'zhaoshang' },
    { code: 'sz000333', name: '美的集团', pinyin: 'meidi' },
    { code: 'sz000002', name: '万科A', pinyin: 'wanke' },
    { code: 'sh600276', name: '恒瑞医药', pinyin: 'hengrui' },
    { code: 'sh600030', name: '中信证券', pinyin: 'zhongxin' },
    { code: 'hk00700', name: '腾讯控股', pinyin: 'tengxun' },
    { code: 'hk03690', name: '美团-W', pinyin: 'meituan' },
    { code: 'hk09988', name: '阿里巴巴-SW', pinyin: 'alibaba' }
  ]
  
  const lowerKeyword = keyword.toLowerCase()
  return commonStocks.filter(stock => 
    stock.name.includes(keyword) ||
    stock.code.includes(keyword) ||
    stock.pinyin.includes(lowerKeyword)
  )
}
