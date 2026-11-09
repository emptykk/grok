<template>
  <div class="timeline-rail">
    <div
      v-for="(item, idx) in items"
      :key="idx"
      class="tl-item"
      :class="item.status"
    >
      <div class="tl-dot" />
      <div class="tl-title">{{ item.step }}</div>
      <div class="tl-meta">{{ item.actor }} · {{ item.time }}</div>
      <div v-if="item.comment" class="tl-meta" style="margin-top:4px">{{ item.comment }}</div>
    </div>
  </div>
</template>
<script setup>
defineProps({
  items: { type: Array, default: () => [] }
})
</script>
