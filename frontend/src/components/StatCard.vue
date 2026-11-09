<template>
  <div class="ip-card lift stat-card" :class="tone">
    <div class="accent-bar" />
    <div class="label">{{ label }}</div>
    <div class="value">{{ display }}</div>
    <div v-if="hint" class="hint">{{ hint }}</div>
  </div>
</template>
<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  label: String,
  value: [String, Number],
  hint: String,
  tone: { type: String, default: '' }
})
const display = ref(props.value ?? 0)
let frame
watch(() => props.value, (v) => {
  const target = Number(v)
  if (!Number.isFinite(target)) { display.value = v ?? 0; return }
  const start = performance.now()
  const from = Number(display.value) || 0
  cancelAnimationFrame(frame)
  const tick = (t) => {
    const p = Math.min(1, (t - start) / 520)
    display.value = Math.round(from + (target - from) * (1 - Math.pow(1 - p, 3)))
    if (p < 1) frame = requestAnimationFrame(tick)
  }
  frame = requestAnimationFrame(tick)
}, { immediate: true })
onBeforeUnmount(() => cancelAnimationFrame(frame))
</script>
