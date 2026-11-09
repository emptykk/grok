<template>
  <div class="ph">
    <div>
      <h1 class="ip-page-title">{{ title }}</h1>
      <p v-if="sub" class="ip-page-sub">{{ sub }}</p>
    </div>
    <div class="actions">
      <slot />
    </div>
  </div>
</template>
<script setup>
defineProps({ title: String, sub: String })
</script>
<style scoped>
.ph { display:flex; justify-content:space-between; align-items:flex-start; gap:16px; margin-bottom:16px; flex-wrap: wrap; }
@media (max-width: 720px) {
  .ph { flex-direction: column; }
  .actions { width: 100%; }
}
.actions { display:flex; gap:8px; flex-wrap:wrap; align-items:center; }
</style>
