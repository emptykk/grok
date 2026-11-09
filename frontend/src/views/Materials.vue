<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="keyword" placeholder="编码/名称" style="width:200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button type="success" @click="openEdit()">新增</el-button>
    </div>
    <el-table :data="rows">
      <el-table-column prop="code" label="编码" width="100" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="spec" label="规格" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="safeStock" label="安全库存" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button link @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="onDel(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="visible" :title="form.id?'编辑物资':'新增物资'">
      <el-form label-width="90px">
        <el-form-item label="编码"><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="规格"><el-input v-model="form.spec" /></el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="form.safeStock" :min="0" /></el-form-item>
      </el-form>
      <template #footer><el-button type="primary" @click="save">保存</el-button></template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { pageMaterials, saveMaterial, deleteMaterial } from '@/api'
import { ElMessage, ElMessageBox } from 'element-plus'
const rows = ref([]); const keyword = ref(''); const visible = ref(false)
const form = reactive({ id:null, code:'', name:'', spec:'', unit:'', category:'', safeStock:0 })
async function load(){ const d = await pageMaterials({ page:1, size:50, keyword: keyword.value }); rows.value = d.records }
function openEdit(row){ Object.assign(form, row || { id:null, code:'', name:'', spec:'', unit:'台', category:'办公设备', safeStock:0 }); visible.value=true }
async function save(){ await saveMaterial({...form}); ElMessage.success('已保存'); visible.value=false; load() }
async function onDel(row){ await ElMessageBox.confirm('确认删除？'); await deleteMaterial(row.id); load() }
onMounted(load)
</script>
<style scoped>.toolbar{display:flex;gap:8px;margin-bottom:12px}</style>
