<template>
  <div class="login-wrap">
    <div class="grid" />
    <div class="glow a" />
    <div class="glow b" />
    <section class="story">
      <div class="eyebrow">PROCUREMENT CONTROL · THESIS DEMO</div>
      <div class="slogan">看得见的审批闭环，算得清的每一分钱</div>
      <h1>基于大模型的<br />智能采购管理系统</h1>
      <p>两级 / 金额三级审批、预算拦截、多轮询价、供应商画像、制度 RAG 与运行中心。没有大模型 Key 时，智能能力按规则降级，主流程仍可演示。</p>
      <div class="metrics">
        <div><b>6</b><span>演示角色</span></div>
        <div><b>8</b><span>流程节点</span></div>
        <div><b>Ctrl K</b><span>命令面板</span></div>
      </div>
    </section>
    <div class="panel">
      <div class="brand">
        <div class="logo-mark">采</div>
        <div>
          <div class="name">进入控制台</div>
          <div class="tag" v-if="showDemoAccounts && !publicMode">点击身份即一键登录 · 本地演示</div>
          <div class="tag" v-else>公网演示已启用门禁。请使用部署者单独提供的账号，页面不再展示本地口令。</div>
        </div>
      </div>
      <el-form class="form" @submit.prevent="onLogin">
        <el-form-item>
          <el-input v-model="form.username" size="large" placeholder="账号" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" size="large" type="password" show-password placeholder="密码" prefix-icon="Lock" @keyup.enter="onLogin" />
        </el-form-item>
        <el-button type="primary" size="large" class="btn" :loading="loading" @click="onLogin">进入系统</el-button>
      </el-form>
      <div class="tips" v-if="showDemoAccounts && !publicMode">
        <div class="tip-title">演示账号 · 一键进入</div>
        <div class="chips">
          <button
            v-for="(a, i) in accounts"
            :key="a.u"
            type="button"
            class="chip"
            :class="{ on: form.username === a.u }"
            :style="{ animationDelay: (i * 70) + 'ms' }"
            @click="enter(a)"
          >
            <b>{{ a.label }}</b>
            <span>{{ a.u }}</span>
            <em>{{ a.desc }}</em>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { burstConfetti } from '@/utils/confetti'
import { authBootstrap } from '@/api/auth'

const showDemoAccounts = import.meta.env.VITE_SHOW_DEMO_ACCOUNTS !== 'false'
const router = useRouter()
const store = useUserStore()
const loading = ref(false)
const publicMode = ref(false)
const form = reactive({ username: showDemoAccounts ? 'admin' : '', password: '' })
const accounts = showDemoAccounts
  ? [
      { u: 'zhangsan', label: '申请人', p: '123456', desc: '建单提交 · 仅本人' },
      { u: 'lisi', label: '主管', p: '123456', desc: '一级审批 · 本部门' },
      { u: 'wangwu', label: '采购', p: '123456', desc: '比价下单 · 全部' },
      { u: 'zhaoliu', label: '仓管', p: '123456', desc: '入库与预警' },
      { u: 'caiwu', label: '财务', p: '123456', desc: '三级终审 / 付款' },
      { u: 'admin', label: '管理员', p: '123456', desc: '运行中心 · 全部' }
    ]
  : []
if (showDemoAccounts) form.password = '123456'
onMounted(async () => {
  try {
    const data = await authBootstrap()
    publicMode.value = !!data.publicMode
    if (publicMode.value) {
      form.username = ''
      form.password = ''
    }
  } catch { /* local API may be down; keep the local form */ }
})
async function enter(a) {
  form.username = a.u
  form.password = a.p
  await onLogin()
}
async function onLogin() {
  loading.value = true
  try {
    await store.login(form.username, form.password)
    ElMessage.success('登录成功')
    burstConfetti()
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>
<style scoped>
.login-wrap {
  min-height: 100%;
  display: grid;
  grid-template-columns: 1.15fr .85fr;
  position: relative;
  overflow: hidden;
  background: #070b14;
  color: #e5e7eb;
}
.grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(45,212,191,.08) 1px, transparent 1px),
    linear-gradient(90deg, rgba(45,212,191,.08) 1px, transparent 1px);
  background-size: 42px 42px;
  mask-image: radial-gradient(circle at 30% 40%, #000 20%, transparent 72%);
  animation: drift 18s linear infinite;
}
@keyframes drift { to { background-position: 42px 42px; } }
.glow { position: absolute; width: 320px; height: 320px; border-radius: 50%; filter: blur(50px); opacity: .35; }
.glow.a { background: #0d9488; left: -40px; top: 10%; animation: floaty 7s ease-in-out infinite; }
.glow.b { background: #1d4ed8; right: 20%; bottom: -80px; animation: floaty 9s ease-in-out infinite reverse; }
.story, .panel { position: relative; z-index: 1; }
.story { padding: 10vh 8vw 8vh; display: flex; flex-direction: column; justify-content: center; }
.eyebrow { letter-spacing: .22em; font-size: 12px; color: #5eead4; margin-bottom: 10px; }
.slogan {
  display: inline-block; margin-bottom: 14px; padding: 4px 10px; border-radius: 999px;
  border: 1px solid rgba(94, 234, 212, .35); color: #99f6e4; font-size: 13px;
  background: rgba(13, 148, 136, .15);
  animation: pulse 3.2s ease-in-out infinite;
}
@keyframes pulse { 50% { box-shadow: 0 0 24px rgba(45, 212, 191, .35); } }
h1 { margin: 0; font-size: clamp(36px, 4vw, 56px); line-height: 1.15; letter-spacing: -0.04em; }
.story p { max-width: 520px; color: #94a3b8; line-height: 1.7; margin-top: 18px; }
.metrics { display: flex; gap: 28px; margin-top: 36px; }
.metrics b { display: block; font-size: 28px; color: #fff; }
.metrics span { color: #64748b; font-size: 12px; }
.panel {
  margin: auto 7vw auto 0;
  width: min(440px, 92%);
  padding: 28px;
  border-radius: 20px;
  background: rgba(15, 23, 42, .78);
  border: 1px solid rgba(148, 163, 184, .2);
  box-shadow: 0 30px 80px rgba(0,0,0,.35);
  backdrop-filter: blur(16px);
}
.brand { display:flex; gap:12px; align-items:center; margin-bottom: 18px; }
.logo-mark {
  width: 46px; height: 46px; border-radius: 14px;
  display:flex; align-items:center; justify-content:center;
  font-weight: 800; color: #042f2e;
  background: linear-gradient(135deg, #5eead4, #14b8a6);
}
.name { font-size: 18px; font-weight: 750; }
.tag { font-size: 12px; color: #94a3b8; margin-top: 2px; }
.btn { width: 100%; font-weight: 700; }
.tip-title { font-size: 12px; color: #94a3b8; margin: 16px 0 8px; }
.chips { display:grid; grid-template-columns: 1fr 1fr; gap: 8px; }
.chip {
  border: 1px solid rgba(148,163,184,.25);
  background: rgba(15,23,42,.5);
  color: #e5e7eb;
  border-radius: 12px;
  padding: 8px 10px 7px;
  cursor: pointer;
  text-align: left;
  animation: rise .45s ease both;
  transition: transform .18s ease, border-color .18s ease;
}
@keyframes rise { from { opacity: 0; transform: translateY(8px); } to { opacity: 1; transform: none; } }
.chip b { display: block; font-size: 13px; }
.chip span, .chip em { display: block; font-size: 11px; color: #94a3b8; font-style: normal; }
.chip.on, .chip:hover { border-color: #2dd4bf; transform: translateY(-2px); }
@media (max-width: 900px) {
  .login-wrap { grid-template-columns: 1fr; min-height: 100%; overflow: auto; }
  .story { padding: 36px 20px 8px; }
  .story h1 { font-size: 32px; }
  .panel { margin: 8px auto 28px; width: min(440px, calc(100% - 24px)); }
  .metrics { display: none; }
}
@media (max-width: 520px) {
  .chips { grid-template-columns: 1fr; }
}
</style>
