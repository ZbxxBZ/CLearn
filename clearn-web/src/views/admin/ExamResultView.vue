<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>考试</span>
          <el-button :icon="Refresh" circle @click="loadExams" />
        </div>
      </template>
      <el-table :data="exams" height="520" empty-text="暂无考试" @row-click="loadResults">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>结果</template>
      <pre class="result-box tall">{{ resultText }}</pre>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { api } from '../../api/client';

const exams = ref([]);
const results = ref(null);
const resultText = computed(() => JSON.stringify(results.value || '请选择考试查看结果', null, 2));

async function loadExams() {
  exams.value = await api('/api/admin/exams');
}

async function loadResults(row) {
  try {
    results.value = await api(`/api/admin/exams/${row.id}/results`);
  } catch (error) {
    ElMessage.error(error.message);
  }
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '';
}

onMounted(loadExams);
</script>
