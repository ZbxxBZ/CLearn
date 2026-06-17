<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>新建考试</template>
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <section class="form-grid">
          <el-form-item label="开始时间">
            <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" />
          </el-form-item>
          <el-form-item label="结束时间">
            <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DDTHH:mm:ssZ" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" />
          </el-form-item>
        </section>
        <el-button type="primary" :loading="saving" @click="createExam">创建考试</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>考试列表</span>
          <el-button :icon="Refresh" circle @click="loadExams" />
        </div>
      </template>
      <el-table :data="exams" height="300" empty-text="暂无考试" @row-click="selectExam">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="题数" width="90">
          <template #default="{ row }">{{ row.problems?.length || 0 }}</template>
        </el-table-column>
      </el-table>

      <el-divider />
      <el-form label-position="top">
        <section class="form-grid">
          <el-form-item label="题目 ID">
            <el-input-number v-model="examProblem.problemId" :min="1" />
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="examProblem.score" :min="1" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="examProblem.sortOrder" :min="1" />
          </el-form-item>
        </section>
        <el-button type="primary" @click="addProblemToExam">加入当前考试</el-button>
      </el-form>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { api, toJsonBody } from '../../api/client';

const exams = ref([]);
const selectedExamId = ref(null);
const saving = ref(false);
const form = reactive({
  title: '',
  description: '',
  startTime: '',
  endTime: '',
  enabled: true
});
const examProblem = reactive({
  problemId: 1,
  score: 100,
  sortOrder: 1
});

async function loadExams() {
  exams.value = await api('/api/admin/exams');
}

function selectExam(row) {
  selectedExamId.value = row.id;
}

async function createExam() {
  saving.value = true;
  try {
    await api('/api/admin/exams', {
      method: 'POST',
      body: toJsonBody(form)
    });
    ElMessage.success('考试已创建');
    await loadExams();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    saving.value = false;
  }
}

async function addProblemToExam() {
  if (!selectedExamId.value) {
    ElMessage.warning('请选择考试');
    return;
  }
  try {
    await api(`/api/admin/exams/${selectedExamId.value}/problems`, {
      method: 'POST',
      body: toJsonBody(examProblem)
    });
    ElMessage.success('题目已加入考试');
    await loadExams();
  } catch (error) {
    ElMessage.error(error.message);
  }
}

onMounted(loadExams);
</script>
