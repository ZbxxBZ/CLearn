<template>
  <main class="login-page">
    <section class="login-panel">
      <div class="login-brand">
        <div class="brand-mark">C</div>
        <div>
          <h1>CLearn</h1>
          <p>C 语言在线刷题与考试平台</p>
        </div>
      </div>

      <el-form class="login-form" label-position="top" @submit.prevent>
        <el-form-item label="账号">
          <el-input v-model="form.username" autocomplete="username" size="large" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" autocomplete="current-password" show-password size="large" />
        </el-form-item>
        <el-button type="primary" size="large" class="full-width" :loading="loading" @click="login">
          登录
        </el-button>
      </el-form>

      <div class="quick-actions">
        <el-button @click="fillAccount('student')">学生账号</el-button>
        <el-button @click="fillAccount('admin')">管理员账号</el-button>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref } from 'vue';
import { useRouter } from 'vue-router';
import { ElMessage } from 'element-plus';
import { api, toJsonBody } from '../api/client';
import { defaultRouteForRole, saveSession } from '../stores/session';

const router = useRouter();
const loading = ref(false);
const form = reactive({
  username: '',
  password: ''
});

function fillAccount(username) {
  form.username = username;
  form.password = 'password';
}

async function login() {
  loading.value = true;
  try {
    const data = await api('/api/auth/login', {
      method: 'POST',
      body: toJsonBody(form)
    });
    saveSession(data);
    ElMessage.success('登录成功');
    await router.replace(defaultRouteForRole(data.role));
  } catch (error) {
    ElMessage.error(error.message);
  } finally {
    loading.value = false;
  }
}
</script>
