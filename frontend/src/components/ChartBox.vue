<template>
  <div ref="el" class="chart" :style="{ height }" />
</template>
<script setup>
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { useThemeStore } from '@/stores/theme'

const props = defineProps({
  option: { type: Object, required: true },
  height: { type: String, default: '280px' }
})
const el = ref(null)
const theme = useThemeStore()
let chart
function render() {
  if (!el.value) return
  if (!chart) chart = echarts.init(el.value, theme.dark ? 'dark' : undefined)
  chart.setOption({ backgroundColor: 'transparent', ...props.option }, true)
}
function rerender() {
  chart?.dispose()
  chart = null
  render()
}
onMounted(() => {
  render()
  window.addEventListener('resize', resize)
})
onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  chart?.dispose()
})
watch(() => props.option, render, { deep: true })
watch(() => theme.dark, rerender)
function resize() { chart?.resize() }
</script>
<style scoped>
.chart { width: 100%; }
</style>
