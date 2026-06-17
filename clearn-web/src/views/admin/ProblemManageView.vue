<template>
  <section class="content-grid two-columns">
    <el-card shadow="never">
      <template #header>新建题目</template>
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
import { Refresh } from '@element-plus/icons-vue';
import { api, toJsonBody } from '../../api/client';

const saving = ref(false);
const problems = ref([]);
const form = reactive({
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
});

async function loadProblems() {
  problems.value = await api('/api/problems');
}

async function createProblem() {
  saving.value = true;
  try {
    await api('/api/admin/problems', {
      method: 'POST',
      body: toJsonBody(form)
    });
    ElMessage.success('题目已保存');
    await loadProblems();
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    saving.value = false;
  }
}

onMounted(loadProblems);
</script>
