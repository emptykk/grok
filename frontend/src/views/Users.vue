<template>
  <div class="ip-page">
    <PageHeader title="用户与数据范围" sub="角色上的 SELF / DEPT / ALL 会显示在顶栏。管理员可在此查看，改范围仍走角色接口。" />
    <el-row :gutter="14">
      <el-col :xs="24" :lg="14">
        <div class="ip-card" style="padding:8px">
          <el-table :data="rows">
            <el-table-column prop="username" label="账号" />
            <el-table-column prop="realName" label="姓名" />
            <el-table-column prop="deptName" label="部门" />
            <el-table-column prop="phone" label="电话" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '停用' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </el-col>
      <el-col :xs="24" :lg="10">
        <div class="ip-card pad">
          <div class="sec">角色数据范围</div>
          <div v-for="r in roles" :key="r.id" class="role">
            <div>
              <b>{{ r.name }}</b>
              <div class="muted">{{ r.code }}</div>
            </div>
            <el-tag :type="r.dataScope === 'ALL' ? 'success' : (r.dataScope === 'DEPT' ? 'warning' : 'info')">{{ r.dataScope }}</el-tag>
          </div>
          <p class="muted">申请人 SELF 只看自己的申请；部门主管 DEPT 看本部门；采购、仓管、财务、管理员 ALL。</p>
        </div>
      </el-col>
    </el-row>
  </div>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { pageUsers, listRoles } from '@/api'
import PageHeader from '@/components/PageHeader.vue'

const rows = ref([])
const roles = ref([])
onMounted(async () => {
  const d = await pageUsers({ page: 1, size: 50 })
  rows.value = d.records
  roles.value = await listRoles()
})
</script>
<style scoped>
.pad { padding: 16px; }
.sec { font-weight: 700; margin-bottom: 10px; }
.role { display: flex; justify-content: space-between; align-items: center; padding: 10px 0; border-bottom: 1px solid var(--ip-border); }
.muted { color: var(--ip-text-secondary); font-size: 12px; margin-top: 2px; }
</style>
