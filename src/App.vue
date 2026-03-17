<template>
  <div class="container">
    <header class="header">
      <h1>📈 股票决策助手</h1>
      <p>智能监控您的股票收益，达到目标自动提醒</p>
    </header>

    <main class="main-content">
      <!-- 添加股票表单 -->
      <section class="add-stock-section">
        <h2>添加持仓股票</h2>
        <div class="form-grid">
          <div class="form-group">
            <label>股票代码</label>
            <input 
              v-model="newStock.code" 
              placeholder="如: 600519 或 00700"
              @blur="formatStockCode"
            />
            <small>支持A股(6位数字)和港股(5位数字)</small>
          </div>
          <div class="form-group">
            <label>买入价格 (¥)</label>
            <input 
              v-model.number="newStock.buyPrice" 
              type="number" 
              step="0.01"
              placeholder="输入买入价格"
            />
          </div>
          <div class="form-group">
            <label>买入数量</label>
            <input 
              v-model.number="newStock.quantity" 
              type="number" 
              placeholder="输入买入数量"
            />
          </div>
          <div class="form-group">
            <label>目标收益率 (%)</label>
            <input 
              v-model.number="newStock.targetReturn" 
              type="number" 
              step="0.1"
              value="15"
            />
          </div>
        </div>
        <button class="btn-primary" @click="addStock" :disabled="!isValidForm">
          添加监控
        </button>
      </section>

      <!-- 股票列表 -->
      <section class="stock-list-section">
        <h2>我的持仓监控</h2>
        <div v-if="stocks.length === 0" class="empty-state">
          <p>暂无监控股票，请添加您的第一只股票</p>
        </div>
        <div v-else class="stock-grid">
          <div 
            v-for="stock in stocks" 
            :key="stock.id"
            class="stock-card"
            :class="{ 'alert': stock.shouldSell }"
          >
            <div class="stock-header">
              <h3>{{ stock.name || stock.code }}</h3>
              <span class="stock-code">{{ stock.code }}</span>
              <button class="btn-delete" @click="removeStock(stock.id)">×</button>
            </div>
            
            <div class="stock-info">
              <div class="info-row">
                <span>买入价格</span>
                <strong>¥{{ stock.buyPrice.toFixed(2) }}</strong>
              </div>
              <div class="info-row">
                <span>当前价格</span>
                <strong :class="getPriceClass(stock)">
                  {{ stock.currentPrice ? '¥' + stock.currentPrice.toFixed(2) : '加载中...' }}
                </strong>
              </div>
              <div class="info-row">
                <span>持仓数量</span>
                <strong>{{ stock.quantity }} 股</strong>
              </div>
              <div class="info-row">
                <span>目标收益率</span>
                <strong class="target">+{{ stock.targetReturn }}%</strong>
              </div>
            </div>

            <div class="profit-section">
              <div class="profit-item">
                <span>当前收益</span>
                <strong :class="getProfitClass(stock)">
                  {{ stock.profit ? (stock.profit >= 0 ? '+' : '') + '¥' + stock.profit.toFixed(2) : '--' }}
                </strong>
              </div>
              <div class="profit-item">
                <span>收益率</span>
                <strong :class="getProfitClass(stock)">
                  {{ stock.returnRate ? (stock.returnRate >= 0 ? '+' : '') + stock.returnRate.toFixed(2) + '%' : '--' }}
                </strong>
              </div>
            </div>

            <div v-if="stock.shouldSell" class="alert-banner">
              🎯 已达到目标收益率，建议卖出！
            </div>

            <div class="stock-footer">
              <button class="btn-chart" @click="showStockChart(stock)">
                📊 查看走势图
              </button>
              <span class="update-time">
                更新: {{ stock.updateTime || '未更新' }}
              </span>
            </div>
          </div>
        </div>
      </section>

      <!-- 提醒设置 -->
      <section class="settings-section">
        <h2>提醒设置</h2>
        <div class="settings-grid">
          <label class="setting-item">
            <input type="checkbox" v-model="settings.browserNotify" />
            <span>浏览器通知提醒</span>
          </label>
          <label class="setting-item">
            <input type="checkbox" v-model="settings.soundAlert" />
            <span>声音提醒</span>
          </label>
          <div class="setting-item">
            <span>刷新频率</span>
            <select v-model="settings.refreshInterval">
              <option :value="10000">10秒</option>
              <option :value="30000">30秒</option>
              <option :value="60000">1分钟</option>
              <option :value="300000">5分钟</option>
            </select>
          </div>
        </div>
      </section>
    </main>

    <!-- 全局提醒 -->
    <div v-if="globalAlert.show" class="global-alert" :class="globalAlert.type">
      {{ globalAlert.message }}
    </div>

    <!-- 股票走势图弹窗 -->
    <div v-if="selectedStockForChart" class="chart-modal" @click="closeStockChart">
      <div class="chart-modal-content" @click.stop>
        <StockChart
          :stock-code="selectedStockForChart.code"
          :stock-name="selectedStockForChart.name"
          @close="closeStockChart"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { getStockPrice } from './services/stockApi.js'
import StockChart from './components/StockChart.vue'

export default {
  name: 'App',
  components: {
    StockChart
  },
  setup() {
    // 响应式数据
    const stocks = ref([])
    const newStock = ref({
      code: '',
      buyPrice: null,
      quantity: null,
      targetReturn: 15
    })
    const settings = ref({
      browserNotify: true,
      soundAlert: true,
      refreshInterval: 30000
    })
    const globalAlert = ref({ show: false, message: '', type: '' })
    const selectedStockForChart = ref(null)
    let refreshTimer = null
    let notifiedStocks = new Set()

    // 计算属性
    const isValidForm = computed(() => {
      return newStock.value.code && 
             newStock.value.buyPrice > 0 && 
             newStock.value.quantity > 0 &&
             newStock.value.targetReturn > 0
    })

    // 格式化股票代码
    const formatStockCode = () => {
      let code = newStock.value.code.trim()
      // 移除空格和特殊字符
      code = code.replace(/[^\d]/g, '')
      newStock.value.code = code
    }

    // 获取完整的股票代码（带前缀）
    const getFullStockCode = (code) => {
      if (code.length === 5) {
        return 'hk' + code // 港股
      } else if (code.startsWith('6')) {
        return 'sh' + code // 上海A股
      } else if (code.startsWith('0') || code.startsWith('3')) {
        return 'sz' + code // 深圳A股
      }
      return code
    }

    // 添加股票
    const addStock = async () => {
      if (!isValidForm.value) return

      const stock = {
        id: Date.now(),
        code: getFullStockCode(newStock.value.code),
        name: '',
        buyPrice: newStock.value.buyPrice,
        quantity: newStock.value.quantity,
        targetReturn: newStock.value.targetReturn,
        currentPrice: null,
        profit: null,
        returnRate: null,
        shouldSell: false,
        updateTime: null
      }

      stocks.value.push(stock)
      
      // 立即获取价格
      await updateStockPrice(stock)
      
      // 保存到本地存储
      saveStocks()
      
      // 重置表单
      newStock.value = {
        code: '',
        buyPrice: null,
        quantity: null,
        targetReturn: 15
      }

      showAlert('股票添加成功！', 'success')
    }

    // 删除股票
    const removeStock = (id) => {
      stocks.value = stocks.value.filter(s => s.id !== id)
      notifiedStocks.delete(id)
      saveStocks()
    }

    // 更新股票价格
    const updateStockPrice = async (stock) => {
      try {
        const data = await getStockPrice(stock.code)
        if (data) {
          stock.name = data.name
          stock.currentPrice = data.price
          stock.updateTime = new Date().toLocaleTimeString()
          
          // 计算收益
          const totalBuy = stock.buyPrice * stock.quantity
          const totalCurrent = stock.currentPrice * stock.quantity
          stock.profit = totalCurrent - totalBuy
          stock.returnRate = ((stock.currentPrice - stock.buyPrice) / stock.buyPrice) * 100
          
          // 检查是否达到目标收益率
          stock.shouldSell = stock.returnRate >= stock.targetReturn
          
          // 触发提醒
          if (stock.shouldSell && !notifiedStocks.has(stock.id)) {
            notifySell(stock)
            notifiedStocks.add(stock.id)
          }
        }
      } catch (error) {
        console.error('获取股票价格失败:', error)
      }
    }

    // 更新所有股票价格
    const updateAllPrices = async () => {
      for (const stock of stocks.value) {
        await updateStockPrice(stock)
      }
    }

    // 通知卖出
    const notifySell = (stock) => {
      const message = `🎯 ${stock.name}(${stock.code}) 已达到目标收益率 ${stock.returnRate.toFixed(2)}%！建议卖出`
      
      // 浏览器通知
      if (settings.value.browserNotify && 'Notification' in window) {
        Notification.requestPermission().then(permission => {
          if (permission === 'granted') {
            new Notification('股票卖出提醒', {
              body: message,
              icon: '📈'
            })
          }
        })
      }
      
      // 声音提醒
      if (settings.value.soundAlert) {
        playAlertSound()
      }
      
      // 页面提醒
      showAlert(message, 'warning')
    }

    // 播放提醒音
    const playAlertSound = () => {
      const audio = new Audio('data:audio/wav;base64,UklGRnoGAABXQVZFZm10IBAAAAABAAEAQB8AAEAfAAABAAgAZGF0YQoGAACBhYqFbF1fdJivrJBhNjVgodDbq2EcBj+a2/LDciUFLIHO8tiJNwgZaLvt559NEAxQp+PwtmMcBjiR1/LMeSwFJHfH8N2QQAoUXrTp66hVFApGn+DyvmwhBTGH0fPTgjMGHm7A7+OZURE')
      audio.play().catch(() => {})
    }

    // 显示全局提醒
    const showAlert = (message, type = 'info') => {
      globalAlert.value = { show: true, message, type }
      setTimeout(() => {
        globalAlert.value.show = false
      }, 5000)
    }

    // 样式类
    const getPriceClass = (stock) => {
      if (!stock.currentPrice) return ''
      return stock.currentPrice > stock.buyPrice ? 'up' : stock.currentPrice < stock.buyPrice ? 'down' : ''
    }

    const getProfitClass = (stock) => {
      if (!stock.profit) return ''
      return stock.profit > 0 ? 'up' : stock.profit < 0 ? 'down' : ''
    }

    // 显示股票走势图
    const showStockChart = (stock) => {
      selectedStockForChart.value = stock
    }

    // 关闭股票走势图
    const closeStockChart = () => {
      selectedStockForChart.value = null
    }

    // 本地存储
    const saveStocks = () => {
      localStorage.setItem('stocks', JSON.stringify(stocks.value))
      localStorage.setItem('settings', JSON.stringify(settings.value))
    }

    const loadStocks = () => {
      const savedStocks = localStorage.getItem('stocks')
      const savedSettings = localStorage.getItem('settings')
      if (savedStocks) {
        stocks.value = JSON.parse(savedStocks)
      }
      if (savedSettings) {
        settings.value = JSON.parse(savedSettings)
      }
    }

    // 监听设置变化
    watch(settings, saveStocks, { deep: true })

    // 监听刷新频率变化
    watch(() => settings.value.refreshInterval, () => {
      clearInterval(refreshTimer)
      refreshTimer = setInterval(updateAllPrices, settings.value.refreshInterval)
    })

    // 生命周期
    onMounted(() => {
      loadStocks()
      updateAllPrices()
      refreshTimer = setInterval(updateAllPrices, settings.value.refreshInterval)
      
      // 请求通知权限
      if ('Notification' in window) {
        Notification.requestPermission()
      }
    })

    onUnmounted(() => {
      clearInterval(refreshTimer)
    })

    return {
      stocks,
      newStock,
      settings,
      globalAlert,
      selectedStockForChart,
      isValidForm,
      formatStockCode,
      addStock,
      removeStock,
      getPriceClass,
      getProfitClass,
      showStockChart,
      closeStockChart
    }
  }
}
</script>

<style scoped>
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  color: white;
  margin-bottom: 30px;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
}

.header p {
  opacity: 0.9;
  font-size: 1.1rem;
}

.main-content {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

section {
  background: white;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

section h2 {
  margin-bottom: 20px;
  color: #333;
  font-size: 1.3rem;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.form-group {
  display: flex;
  flex-direction: column;
}

.form-group label {
  font-weight: 600;
  margin-bottom: 6px;
  color: #555;
  font-size: 0.9rem;
}

.form-group input {
  padding: 12px;
  border: 2px solid #e0e0e0;
  border-radius: 8px;
  font-size: 1rem;
  transition: border-color 0.3s;
}

.form-group input:focus {
  outline: none;
  border-color: #667eea;
}

.form-group small {
  color: #999;
  font-size: 0.75rem;
  margin-top: 4px;
}

.btn-primary {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 14px 32px;
  border-radius: 8px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-primary:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #999;
}

.stock-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.stock-card {
  background: #f8f9fa;
  border-radius: 12px;
  padding: 20px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.stock-card.alert {
  border-color: #ff6b6b;
  background: #fff5f5;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(255, 107, 107, 0.4); }
  50% { box-shadow: 0 0 0 10px rgba(255, 107, 107, 0); }
}

.stock-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
  position: relative;
}

.stock-header h3 {
  font-size: 1.2rem;
  color: #333;
}

.stock-code {
  background: #e0e0e0;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 0.8rem;
  color: #666;
}

.btn-delete {
  position: absolute;
  right: 0;
  top: 0;
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #999;
  cursor: pointer;
  line-height: 1;
}

.btn-delete:hover {
  color: #ff6b6b;
}

.stock-info {
  margin-bottom: 16px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #e0e0e0;
}

.info-row:last-child {
  border-bottom: none;
}

.info-row span {
  color: #666;
}

.info-row strong {
  color: #333;
}

.info-row strong.target {
  color: #51cf66;
}

.profit-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  background: white;
  padding: 16px;
  border-radius: 8px;
  margin-bottom: 12px;
}

.profit-item {
  text-align: center;
}

.profit-item span {
  display: block;
  font-size: 0.8rem;
  color: #999;
  margin-bottom: 4px;
}

.profit-item strong {
  font-size: 1.1rem;
}

.up {
  color: #ff6b6b;
}

.down {
  color: #51cf66;
}

.alert-banner {
  background: linear-gradient(135deg, #ff6b6b 0%, #ff8e8e 100%);
  color: white;
  padding: 12px;
  border-radius: 8px;
  text-align: center;
  font-weight: 600;
  margin-bottom: 12px;
}

.stock-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.btn-chart {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 6px 12px;
  border-radius: 6px;
  font-size: 0.85rem;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.btn-chart:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
}

.update-time {
  font-size: 0.75rem;
  color: #999;
}

.settings-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  align-items: center;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.setting-item input[type="checkbox"] {
  width: 18px;
  height: 18px;
  cursor: pointer;
}

.setting-item select {
  padding: 8px 12px;
  border: 2px solid #e0e0e0;
  border-radius: 6px;
  font-size: 0.9rem;
  cursor: pointer;
}

.global-alert {
  position: fixed;
  top: 20px;
  right: 20px;
  padding: 16px 24px;
  border-radius: 8px;
  color: white;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  z-index: 1000;
  animation: slideIn 0.3s ease;
}

.global-alert.success {
  background: #51cf66;
}

.global-alert.warning {
  background: #ff6b6b;
}

.global-alert.info {
  background: #667eea;
}

@keyframes slideIn {
  from {
    transform: translateX(100%);
    opacity: 0;
  }
  to {
    transform: translateX(0);
    opacity: 1;
  }
}

/* 图表弹窗样式 */
.chart-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.chart-modal-content {
  width: 100%;
  max-width: 900px;
  animation: slideUp 0.3s ease;
}

@keyframes slideUp {
  from {
    transform: translateY(30px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

@media (max-width: 768px) {
  .container {
    padding: 10px;
  }
  
  .header h1 {
    font-size: 1.8rem;
  }
  
  .form-grid {
    grid-template-columns: 1fr;
  }
  
  .stock-grid {
    grid-template-columns: 1fr;
  }
}
</style>
