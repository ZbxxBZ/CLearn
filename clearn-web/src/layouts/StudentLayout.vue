<template>
  <div class="portal-shell">
    <aside class="portal-sidebar">
      <div class="portal-brand">
        <strong>CLearn</strong>
        <span>学生端</span>
      </div>
      <el-menu router :default-active="route.path">
        <el-menu-item index="/student/practice">
          <el-icon><EditPen /></el-icon>
          <span>题库练习</span>
        </el-menu-item>
        <el-menu-item index="/student/exams">
          <el-icon><Clock /></el-icon>
          <span>考试模式</span>
        </el-menu-item>
        <el-menu-item index="/student/submissions">
          <el-icon><Tickets /></el-icon>
          <span>我的提交</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <section class="portal-main">
      <header class="toolbar">
        <div>
          <h1>学生工作台</h1>
          <p>{{ session.username }} / {{ session.role }}</p>
        </div>
        <el-button :icon="SwitchButton" @click="logout">退出</el-button>
      </header>
      <router-view />
    </section>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router';
import { Clock, EditPen, SwitchButton, Tickets } from '@element-plus/icons-vue';
import { clearSession, getSession } from '../stores/session';

const route = useRoute();
const router = useRouter();
const session = getSession();

function logout() {
  clearSession();
  router.replace('/login');
}
</script>
