<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>新建题目</span>
          <el-upload
            :show-file-list="false"
            :http-request="importProblems"
            accept=".xlsx"
          >
            <el-button :icon="Upload" :loading="importing">批量导入</el-button>
          </el-upload>
        </div>
      </template>
      <el-form :model="form" label-position="top">
        <el-form-item label="标题">
          <el-input v-model="form.title" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="4" />
        </el-form-item>
        <section class="form-grid">
          <el-form-item label="输入说明">
            <el-input v-model="form.inputDescription" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="输出说明">
            <el-input v-model="form.outputDescription" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="难度">
            <el-select v-model="form.difficulty">
              <el-option label="EASY" value="EASY" />
              <el-option label="MEDIUM" value="MEDIUM" />
              <el-option label="HARD" value="HARD" />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-input v-model="form.tags" />
          </el-form-item>
          <el-form-item label="时间限制 ms">
            <el-input-number v-model="form.timeLimitMs" :min="100" />
          </el-form-item>
          <el-form-item label="内存限制 MB">
            <el-input-number v-model="form.memoryLimitMb" :min="16" />
          </el-form-item>
          <el-form-item label="分值">
            <el-input-number v-model="form.score" :min="1" />
          </el-form-item>
          <el-form-item label="启用">
            <el-switch v-model="form.enabled" />
          </el-form-item>
        </section>

        <el-divider content-position="left">可选样例</el-divider>
        <section class="form-grid">
          <el-form-item label="样例输入">
            <el-input v-model="sample.inputData" type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item label="样例输出">
            <el-input v-model="sample.expectedOutput" type="textarea" :rows="2" />
          </el-form-item>
        </section>

        <el-divider content-position="left">正式判题用例（固定 5 个）</el-divider>
        <div v-for="(testCase, index) in judgeCases" :key="index" class="case-row">
          <span class="case-index">#{{ index + 1 }}</span>
          <el-input v-model="testCase.inputData" type="textarea" :rows="2" placeholder="输入" />
          <el-input v-model="testCase.expectedOutput" type="textarea" :rows="2" placeholder="期望输出" />
        </div>

        <el-button type="primary" :loading="saving" @click="createProblem">保存题目</el-button>
      </el-form>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>题目列表</span>
          <el-button :icon="Refresh" circle @click="loadProblems" />
        </div>
      </template>
      <el-table :data="problems" height="520" empty-text="暂无题目">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="title" label="标题" min-width="180" />
        <el-table-column prop="difficulty" label="难度" width="100" />
        <el-table-column prop="score" label="分值" width="90" />
      </el-table>
    </el-card>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { Refresh, Upload } from '@element-plus/icons-vue';
import { API_BASE_URL, api, toJsonBody } from '../../api/client';
import { getSession } from '../../stores/session';

const saving = ref(false);
const importing = ref(false);
const problems = ref([]);
const form = reactive(defaultForm());
const sample = reactive({ inputData: '', expectedOutput: '' });
const judgeCases = reactive(defaultJudgeCases());

function defaultForm() {
  return {
    title: '',
    description: '',
    inputDescription: '',
    outputDescription: '',
    difficulty: 'EASY',
    tags: 'C',
    timeLimitMs: 1000,
    memoryLimitMb: 128,
    score: 100,
    enabled: true
  };
}

function defaultJudgeCases() {
  return Array.from({ length: 5 }, (_, index) => ({
    inputData: '',
    expectedOutput: '',
    sample: false,
    sortOrder: index + 1
  }));
}

async function loadProblems() {
  problems.value = await api('/api/problems');
}

async function createProblem() {
  saving.value = true;
  try {
    await api('/api/admin/problems', {
      method: 'POST',
      body: toJsonBody({
        ...form,
        judgeCases: judgeCases.map((testCase, index) => ({
          inputData: testCase.inputData,
          expectedOutput: testCase.expectedOutput,
          sample: false,
          sortOrder: index + 1
        })),
        samples: buildSamples()
      })
    });
    ElMessage.success('题目已保存');
    resetForm();
    await loadProblems();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    saving.value = false;
  }
}

async function importProblems({ file }) {
  importing.value = true;
  try {
    const data = new FormData();
    data.append('file', file);
    const { token } = getSession();
    const response = await fetch(`${API_BASE_URL}/api/admin/problems/import`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: data
    });
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
      throw new Error(payload?.message || response.statusText || `HTTP ${response.status}`);
    }
    ElMessage.success(`已导入 ${payload?.data?.importedCount ?? 0} 道题目`);
    await loadProblems();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    importing.value = false;
  }
}

function buildSamples() {
  if (!sample.inputData.trim() && !sample.expectedOutput.trim()) {
    return [];
  }
  return [{
    inputData: sample.inputData,
    expectedOutput: sample.expectedOutput,
    sample: true,
    sortOrder: 1001
  }];
}

function resetForm() {
  Object.assign(form, defaultForm());
  sample.inputData = '';
  sample.expectedOutput = '';
  judgeCases.splice(0, judgeCases.length, ...defaultJudgeCases());
}

onMounted(loadProblems);
</script>
