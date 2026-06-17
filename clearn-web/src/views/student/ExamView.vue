<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>开放考试</span>
          <el-button :icon="Refresh" circle @click="loadExams" />
        </div>
      </template>
      <el-table :data="exams" height="320" empty-text="暂无考试" @row-click="selectExam">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatTime(row.endTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>考试题目</template>
      <el-empty v-if="!examDetail?.problems?.length" description="请选择考试" />
      <div v-else class="button-list">
        <el-button
          v-for="problem in examDetail.problems"
          :key="problem.problemId"
          :loading="loadingProblemId === problem.problemId"
          @click="submitExamProblem(problem.problemId)"
        >
          {{ problem.title }} · {{ problem.score }} 分
        </el-button>
      </div>
    </el-card>

    <el-card class="wide" shadow="never">
      <template #header>考试代码与结果</template>
      <el-input v-model="sourceCode" class="code-editor" type="textarea" :rows="14" resize="vertical" spellcheck="false" />
      <pre class="result-box">{{ statusTextValue }}</pre>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh } from '@element-plus/icons-vue';
import { api, toJsonBody } from '../../api/client';
import { submissionResultText } from '../../utils/statusText';

const TERMINAL_STATUSES = ['AC', 'WA', 'CE', 'TLE', 'MLE', 'RE', 'SE'];
const exams = ref([]);
const selectedExamId = ref(null);
const examDetail = ref({});
const examStatus = ref(null);
const loadingProblemId = ref(null);
const sourceCode = ref(`#include <stdio.h>

int main(void) {
    int a, b;
    scanf("%d%d", &a, &b);
    printf("%d\\n", a + b);
    return 0;
}`);

const statusTextValue = computed(() => {
  if (examStatus.value?.status) {
    return submissionResultText(examStatus.value);
  }
  return JSON.stringify(examStatus.value || examDetail.value || '请选择考试', null, 2);
});

async function loadExams() {
  exams.value = await api('/api/exams');
}

async function selectExam(row) {
  selectedExamId.value = row.id;
  examDetail.value = await api(`/api/exams/${row.id}`);
  examStatus.value = await api(`/api/exams/${row.id}/my-result`);
}

async function submitExamProblem(problemId) {
  if (!selectedExamId.value) {
    ElMessage.warning('请选择考试');
    return;
  }
  loadingProblemId.value = problemId;
  try {
    examStatus.value = await api(`/api/exams/${selectedExamId.value}/problems/${problemId}/submissions`, {
      method: 'POST',
      body: toJsonBody({ sourceCode: sourceCode.value })
    });
    pollSubmission(examStatus.value.submissionId);
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loadingProblemId.value = null;
  }
}

function pollSubmission(submissionId) {
  const timer = window.setInterval(async () => {
    try {
      const data = await api(`/api/submissions/${submissionId}`);
      examStatus.value = data;
      if (TERMINAL_STATUSES.includes(data.status)) {
        window.clearInterval(timer);
      }
    } catch (error) {
      window.clearInterval(timer);
      ElMessage.error(error.message);
    }
  }, 1200);
}

function formatTime(value) {
  return value ? new Date(value).toLocaleString() : '';
}

onMounted(loadExams);
</script>
