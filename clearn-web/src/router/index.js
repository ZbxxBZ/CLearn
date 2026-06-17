import { createRouter, createWebHistory } from 'vue-router';
import { defaultRouteForRole, getSession, isAuthenticated, isAdmin } from '../stores/session';
import LoginView from '../views/LoginView.vue';
import StudentLayout from '../layouts/StudentLayout.vue';
import AdminLayout from '../layouts/AdminLayout.vue';
import PracticeView from '../views/student/PracticeView.vue';
import ExamView from '../views/student/ExamView.vue';
import SubmissionView from '../views/student/SubmissionView.vue';
import ProblemManageView from '../views/admin/ProblemManageView.vue';
import ExamManageView from '../views/admin/ExamManageView.vue';
import ExamResultView from '../views/admin/ExamResultView.vue';

const routes = [
  {
    path: '/',
    redirect: () => defaultRouteForRole(getSession().role)
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView,
    meta: { public: true }
  },
  {
    path: '/student',
    component: StudentLayout,
    redirect: '/student/practice',
    children: [
      { path: 'practice', name: 'student-practice', component: PracticeView },
      { path: 'exams', name: 'student-exams', component: ExamView },
      { path: 'submissions', name: 'student-submissions', component: SubmissionView }
    ]
  },
  {
    path: '/admin',
    component: AdminLayout,
    redirect: '/admin/problems',
    meta: { adminOnly: true },
    children: [
      { path: 'problems', name: 'admin-problems', component: ProblemManageView },
      { path: 'exams', name: 'admin-exams', component: ExamManageView },
      { path: 'results', name: 'admin-results', component: ExamResultView }
    ]
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to) => {
  if (to.meta.public) {
    return true;
  }
  if (!isAuthenticated()) {
    return '/login';
  }
  if (to.path.startsWith('/admin') && !isAdmin()) {
    return '/student/practice';
  }
  return true;
});

export default router;
