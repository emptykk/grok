<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="名称" style="width:200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="category" label="品类" width="100" />
      <el-table-column prop="rating" label="评级" width="70" />
      <el-table-column label="状态" width="80"><template #default="{row}">{{ row.status===1?'合作':'停用' }}</template></el-table-column>
      <el-table-column label="操作" width="220">
        <template #default="{row}">
          <el-button link type="primary" @click="$router.push(`/suppliers/${row.id}/profile`)">画像</el-button>
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" title="供应商">
      <el-form label-width="90px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="品类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="评级"><el-rate v-model="form.rating" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageSuppliers, saveSupplier, deleteSupplier } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
const rows = ref([]); const keyword = ref(''); const visible = ref(false)
const form = reactive({ id:null, name:'', contact:'', phone:'', category:'办公设备', rating:3, status:1 })
async function load(){ const d = await pageSuppliers({ page:1, size:50, keyword: keyword.value }); rows.value = d.records }
function openEdit(row){ Object.assign(form, row || { id:null, name:'', contact:'', phone:'', category:'办公设备', rating:3, status:1 }); visible.value=true }
async function save(){ await saveSupplier({...form}); ElMessage.success('已保存'); visible.value=false; load() }
async function onDel(row){ await ElMessageBox.confirm('确认删除？'); await deleteSupplier(row.id); load() }
onMounted(load)
</script>
<style scoped>.toolbar{display:flex;gap:8px;margin-bottom:12px}</style>
