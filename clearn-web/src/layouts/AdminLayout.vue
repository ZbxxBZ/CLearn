<template>
  <div class="portal-shell">
    <aside class="portal-sidebar admin">
      <div class="portal-brand">
        <strong>CLearn</strong>
        <span>管理员端</span>
      </div>
      <el-menu router :default-active="route.path">
        <el-menu-item index="/admin/problems">
          <el-icon><Collection /></el-icon>
          <span>题目管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/exams">
          <el-icon><Calendar /></el-icon>
          <span>考试管理</span>
        </el-menu-item>
        <el-menu-item index="/admin/results">
          <el-icon><DataAnalysis /></el-icon>
          <span>考试结果</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="portal-main">
      <header class="toolbar">
        <div>
          <h1>管理工作台</h1>
          <p>{{ session.username }} / {{ session.role }}</p>
        </div>
        <div class="toolbar-actions">
          <el-button @click="goStudent">学生端</el-button>
          <el-button :icon="SwitchButton" @click="logout">退出</el-button>
        </div>
      </header>
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { Calendar, Collection, DataAnalysis, SwitchButton } from '@element-plus/icons-vue';
import { clearSession, getSession } from '../stores/session';

const route = useRoute();
const router = useRouter();
const session = getSession();

function goStudent() {
  router.push('/student/practice');
}

function logout() {
  clearSession();
  router.replace('/login');
}
</script>
