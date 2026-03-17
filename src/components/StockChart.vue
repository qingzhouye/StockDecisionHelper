<template>
  <div class="stock-chart-container">
    <div class="chart-header">
      <h3>{{ stockName }} ({{ stockCode }}) - 近一年走势</h3>
      <button class="btn-close" @click="$emit('close')">×</button>
    </div>
    <div ref="chartRef" class="chart"></div>
    <div v-if="loading" class="loading">加载中...</div>
    <div v-if="error" class="error">{{ error }}</div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, watch } from 'vue'
import * as echarts from 'echarts'
import { getStockHistory } from '../services/stockApi.js'

export default {
  name: 'StockChart',
  props: {
    stockCode: {
      type: String,
      required: true
    },
    stockName: {
      type: String,
      default: ''
    }
  },
  emits: ['close'],
  setup(props) {
    const chartRef = ref(null)
    const loading = ref(false)
    const error = ref('')
    let chart = null

    const initChart = async () => {
      if (!chartRef.value) return
      
      loading.value = true
      error.value = ''
      
      try {
        const historyData = await getStockHistory(props.stockCode)
        
        if (!historyData || historyData.dates.length === 0) {
          error.value = '暂无历史数据'
          loading.value = false
          return
        }

        // 初始化 ECharts
        chart = echarts.init(chartRef.value)
        
        const option = {
          backgroundColor: 'transparent',
          grid: {
            left: '3%',
            right: '4%',
            bottom: '15%',
            top: '10%',
            containLabel: true
          },
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            borderColor: '#e0e0e0',
            borderWidth: 1,
            textStyle: {
              color: '#333'
            },
            formatter: function(params) {
              const data = params[0]
              return `<div style="font-weight:600">${data.axisValue}</div>
                      <div>收盘价: <span style="color:#667eea;font-weight:600">¥${data.value}</span></div>`
            }
          },
          xAxis: {
            type: 'category',
            data: historyData.dates,
            boundaryGap: false,
            axisLine: {
              lineStyle: {
                color: '#e0e0e0'
              }
            },
            axisLabel: {
              color: '#666',
              rotate: 45,
              interval: Math.floor(historyData.dates.length / 8)
            },
            axisTick: {
              show: false
            }
          },
          yAxis: {
            type: 'value',
            scale: true,
            axisLine: {
              show: false
            },
            axisTick: {
              show: false
            },
            splitLine: {
              lineStyle: {
                color: '#f0f0f0'
              }
            },
            axisLabel: {
              color: '#666',
              formatter: '¥{value}'
            }
          },
          dataZoom: [
            {
              type: 'inside',
              start: 0,
              end: 100
            },
            {
              type: 'slider',
              start: 0,
              end: 100,
              height: 20,
              bottom: 10,
              borderColor: 'transparent',
              backgroundColor: '#f5f5f5',
              fillerColor: 'rgba(102, 126, 234, 0.2)',
              handleStyle: {
                color: '#667eea'
              }
            }
          ],
          series: [
            {
              name: '收盘价',
              type: 'line',
              data: historyData.prices,
              smooth: true,
              symbol: 'none',
              lineStyle: {
                width: 2,
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
                  { offset: 0, color: '#667eea' },
                  { offset: 1, color: '#764ba2' }
                ])
              },
              areaStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  { offset: 0, color: 'rgba(102, 126, 234, 0.3)' },
                  { offset: 1, color: 'rgba(102, 126, 234, 0.05)' }
                ])
              },
              emphasis: {
                focus: 'series',
                itemStyle: {
                  color: '#667eea'
                }
              }
            }
          ]
        }
        
        chart.setOption(option)
        
        // 响应式
        const handleResize = () => {
          chart && chart.resize()
        }
        window.addEventListener('resize', handleResize)
        
        // 清理函数
        onUnmounted(() => {
          window.removeEventListener('resize', handleResize)
          chart && chart.dispose()
        })
        
      } catch (err) {
        error.value = '加载图表失败: ' + err.message
      } finally {
        loading.value = false
      }
    }

    onMounted(() => {
      initChart()
    })

    // 监听股票代码变化，重新加载图表
    watch(() => props.stockCode, () => {
      if (chart) {
        chart.dispose()
        chart = null
      }
      initChart()
    })

    return {
      chartRef,
      loading,
      error
    }
  }
}
</script>

<style scoped>
.stock-chart-container {
  background: white;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  position: relative;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.chart-header h3 {
  font-size: 1.1rem;
  color: #333;
  margin: 0;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  color: #999;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  width: 30px;
  height: 30px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #f5f5f5;
  color: #333;
}

.chart {
  width: 100%;
  height: 350px;
}

.loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #667eea;
  font-size: 1rem;
}

.error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  color: #ff6b6b;
  text-align: center;
  padding: 20px;
}
</style>
