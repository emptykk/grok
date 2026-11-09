<template>
  <div class="layer" aria-hidden="true">
    <i v-for="p in pieces" :key="p.id" class="bit" :style="p.style" />
  </div>
</template>
<script setup>
import { onBeforeUnmount, onMounted, ref } from 'vue'

const pieces = ref([])
let seq = 0
const colors = ['#14b8a6', '#5eead4', '#f59e0b', '#60a5fa', '#f472b6', '#facc15']

function burst() {
  const batch = Array.from({ length: 28 }, (_, i) => {
    const id = ++seq
    const x = 40 + Math.random() * 20
    const dx = (Math.random() - 0.5) * 420
    const dy = 180 + Math.random() * 280
    const rot = Math.random() * 360
    const color = colors[i % colors.length]
    return {
      id,
      style: {
        left: x + '%',
        background: color,
        animationDuration: (0.9 + Math.random() * 0.6) + 's',
        '--dx': dx + 'px',
        '--dy': dy + 'px',
        '--rot': rot + 'deg'
      }
    }
  })
  pieces.value = batch
  setTimeout(() => { pieces.value = [] }, 1600)
}
function onEvent() { burst() }
onMounted(() => window.addEventListener('ip-confetti', onEvent))
onBeforeUnmount(() => window.removeEventListener('ip-confetti', onEvent))
</script>
<style scoped>
.layer { pointer-events: none; position: fixed; inset: 0; z-index: 4000; overflow: hidden; }
.bit {
  position: absolute; top: 18%;
  width: 8px; height: 12px; border-radius: 2px;
  animation: fall linear forwards;
}
@keyframes fall {
  to { transform: translate(var(--dx), var(--dy)) rotate(var(--rot)); opacity: 0; }
}
</style>
