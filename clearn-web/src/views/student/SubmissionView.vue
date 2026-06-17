<template>
  <el-card shadow="never">
    <template #header>
      <div class="card-header">
        <span>我的提交</span>
        <el-button :icon="Refresh" circle @click="loadSubmissions" />
      </div>
    </template>
    <el-table :data="submissions" height="520" empty-text="暂无提交">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="problemId" label="题目" width="90" />
      <el-table-column prop="examId" label="考试" width="90" />
      <el-table-column prop="status" label="状态" width="100" />
      <el-table-column prop="score" label="分数" width="90" />
      <el-table-column prop="timeUsedMs" label="耗时 ms" width="110" />
      <el-table-column label="提交时间" min-width="180">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
  </el-card>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { api } from '../../api/client';

const submissions = ref([]);

async function loadSubmissions() {
  try {
    submissions.value = await api('/api/submissions/my');
  } catch (error) {
    ElMessage.error(error.message);
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '';
}

onMounted(loadSubmissions);
</script>
