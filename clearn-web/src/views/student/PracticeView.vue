<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>题库</span>
          <el-button :icon="Refresh" circle @click="loadProblems" />
        </div>
      </template>
      <el-table :data="problems" height="360" empty-text="暂无题目" @row-click="selectProblem">
        <el-table-column prop="id" label="ID" width="72" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column prop="score" label="分值" width="90" />
      </el-table>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>{{ detail?.title || '题目详情' }}</span>
          <el-tag v-if="detail" effect="plain">{{ detail.difficulty }}</el-tag>
        </div>
      </template>
      <section v-if="detail" class="problem-detail">
        <p>{{ detail.description }}</p>
        <dl>
          <dt>输入</dt>
          <dd>{{ detail.inputDescription }}</dd>
          <dt>输出</dt>
          <dd>{{ detail.outputDescription }}</dd>
          <dt>限制</dt>
          <dd>{{ detail.timeLimitMs }} ms / {{ detail.memoryLimitMb }} MB</dd>
        </dl>
        <section v-if="detail.samples?.length" class="sample-list">
          <h3>样例</h3>
          <div v-for="sample in detail.samples" :key="sample.id || sample.sortOrder" class="sample-item">
            <strong>输入</strong>
            <pre>{{ sample.inputData }}</pre>
            <strong>输出</strong>
            <pre>{{ sample.expectedOutput }}</pre>
          </div>
        </section>
      </section>
      <el-empty v-else description="请选择左侧题目" />
    </el-card>

    <el-card class="wide" shadow="never">
      <template #header>
        <div class="card-header">
          <span>C 代码</span>
          <el-button type="primary" :icon="Upload" :loading="submitting" @click="submitPractice">提交练习</el-button>
        </div>
      </template>
      <el-input v-model="sourceCode" class="code-editor" type="textarea" :rows="15" resize="vertical" spellcheck="false" />
      <pre class="result-box">{{ resultText }}</pre>
    </el-card>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, Upload } from '@element-plus/icons-vue';
import { api, toJsonBody } from '../../api/client';
import { submissionResultText } from '../../utils/statusText';

const TERMINAL_STATUSES = ['AC', 'WA', 'CE', 'TLE', 'MLE', 'RE', 'SE'];
const problems = ref([]);
const detail = ref(null);
const selectedProblemId = ref(null);
const submitting = ref(false);
const result = ref(null);
const sourceCode = ref('');

const resultText = computed(() => {
  return submissionResultText(result.value);
});

async function loadProblems() {
  problems.value = await api('/api/problems');
}

async function selectProblem(row) {
  selectedProblemId.value = row.id;
  detail.value = await api(`/api/problems/${row.id}`);
}

async function submitPractice() {
  if (!selectedProblemId.value) {
    ElMessage.warning('请选择题目');
    return;
  }
  submitting.value = true;
  try {
    result.value = await api(`/api/problems/${selectedProblemId.value}/submissions`, {
      method: 'POST',
      body: toJsonBody({ sourceCode: sourceCode.value })
    });
    pollSubmission(result.value.submissionId);
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    submitting.value = false;
  }
}

function pollSubmission(submissionId) {
  const timer = window.setInterval(async () => {
    try {
      const data = await api(`/api/submissions/${submissionId}`);
      result.value = data;
      if (TERMINAL_STATUSES.includes(data.status)) {
        window.clearInterval(timer);
      }
    } catch (error) {
      window.clearInterval(timer);
      ElMessage.error(error.message);
    }
  }, 1200);
}

onMounted(loadProblems);
</script>
