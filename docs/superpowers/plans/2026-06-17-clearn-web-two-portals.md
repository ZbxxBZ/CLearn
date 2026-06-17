# CLearn Web 双端正式化实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 将当前单文件 demo 前端改造成登录页、学生端、管理员端分离的 Vue + Element Plus 应用。

**架构：** 使用 `vue-router` 做角色分流和页面导航；使用轻量 `session` 模块集中管理 token/角色；使用 `api/client.js` 统一封装后端请求。学生端与管理员端采用独立布局和页面组件，避免继续把所有功能堆在 `App.vue`。

**技术栈：** Vue 3、Vite、Element Plus、vue-router、Vitest。

---

## 文件结构

- 修改：`clearn-web/package.json`，新增 `vue-router`、`vitest` 和 `test` 脚本。
- 修改：`clearn-web/src/main.js`，注册 router。
- 替换：`clearn-web/src/App.vue`，只渲染 `<router-view />`。
- 创建：`clearn-web/src/api/client.js`，统一处理 JSON、Authorization、错误消息。
- 创建：`clearn-web/src/stores/session.js`，管理登录态、localStorage 和角色跳转。
- 创建：`clearn-web/src/router/index.js`，定义 `/login`、`/student`、`/admin` 及守卫。
- 创建：`clearn-web/src/views/LoginView.vue`，正式登录页。
- 创建：`clearn-web/src/layouts/StudentLayout.vue`，学生端导航框架。
- 创建：`clearn-web/src/layouts/AdminLayout.vue`，管理员端导航框架。
- 创建：`clearn-web/src/views/student/PracticeView.vue`，题库练习与提交。
- 创建：`clearn-web/src/views/student/ExamView.vue`，考试列表、考试提交与成绩。
- 创建：`clearn-web/src/views/student/SubmissionView.vue`，我的提交。
- 创建：`clearn-web/src/views/admin/ProblemManageView.vue`，题目创建与列表。
- 创建：`clearn-web/src/views/admin/ExamManageView.vue`，考试创建、绑定题目。
- 创建：`clearn-web/src/views/admin/ExamResultView.vue`，考试结果查看。
- 修改：`clearn-web/src/styles.css`，从 demo 卡片布局改为后台系统布局。
- 创建：`clearn-web/src/stores/session.test.js`，覆盖角色默认入口和会话持久化。

## 任务 1：依赖与会话测试

**文件：**
- 修改：`clearn-web/package.json`
- 创建：`clearn-web/src/stores/session.test.js`
- 创建：`clearn-web/src/stores/session.js`

- [ ] **步骤 1：编写失败的测试**

```js
import { afterEach, describe, expect, it } from 'vitest';
import { clearSession, defaultRouteForRole, getSession, saveSession } from './session';

afterEach(() => {
  localStorage.clear();
});

describe('session store', () => {
  it('routes admins and students to their own portals', () => {
    expect(defaultRouteForRole('ADMIN')).toBe('/admin/problems');
    expect(defaultRouteForRole('STUDENT')).toBe('/student/practice');
    expect(defaultRouteForRole('')).toBe('/login');
  });

  it('persists and clears login state', () => {
    saveSession({ token: 't', username: 'admin', role: 'ADMIN' });

    expect(getSession()).toEqual({ token: 't', username: 'admin', role: 'ADMIN' });

    clearSession();

    expect(getSession()).toEqual({ token: '', username: '', role: '' });
  });
});
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd clearn-web && npm.cmd test -- --run src/stores/session.test.js`
预期：FAIL，提示找不到 `vitest` 或 `./session`。

- [ ] **步骤 3：新增依赖和最小实现**

`package.json` scripts 添加 `"test": "vitest"`，dependencies 添加 `"vue-router": "^4.5.1"`，devDependencies 添加 `"vitest": "^3.2.4"`。创建 `session.js` 导出测试中使用的函数。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd clearn-web && npm.cmd install && npm.cmd test -- --run src/stores/session.test.js`
预期：PASS。

## 任务 2：路由与 API 封装

**文件：**
- 创建：`clearn-web/src/api/client.js`
- 创建：`clearn-web/src/router/index.js`
- 修改：`clearn-web/src/main.js`
- 修改：`clearn-web/src/App.vue`

- [ ] **步骤 1：实现 `api/client.js`**

导出 `api(path, options = {})`，自动加 `Content-Type: application/json` 和 `Authorization: Bearer <token>`，对非 2xx 抛出后端 `message`。

- [ ] **步骤 2：实现 `router/index.js`**

定义 `/login`、`/student/practice`、`/student/exams`、`/student/submissions`、`/admin/problems`、`/admin/exams`、`/admin/results`。守卫规则：无 token 进业务页跳 `/login`；非 ADMIN 进 `/admin` 跳 `/student/practice`；根路径按角色跳默认页。

- [ ] **步骤 3：注册 router**

`main.js` 使用 `createApp(App).use(router).use(ElementPlus).mount('#app')`。

- [ ] **步骤 4：精简 App**

`App.vue` 只保留 `<router-view />`。

## 任务 3：登录页

**文件：**
- 创建：`clearn-web/src/views/LoginView.vue`

- [ ] **步骤 1：实现登录页**

使用 Element Plus 表单，字段为账号、密码；提供学生和管理员快捷填充；登录成功后调用 `saveSession` 并 `router.replace(defaultRouteForRole(role))`。

- [ ] **步骤 2：错误处理**

登录失败显示 `ElMessage.error(error.message)`，loading 状态必须在 finally 释放。

## 任务 4：学生端页面

**文件：**
- 创建：`clearn-web/src/layouts/StudentLayout.vue`
- 创建：`clearn-web/src/views/student/PracticeView.vue`
- 创建：`clearn-web/src/views/student/ExamView.vue`
- 创建：`clearn-web/src/views/student/SubmissionView.vue`

- [ ] **步骤 1：实现学生布局**

顶部展示用户和退出；左侧菜单包含题库练习、考试模式、我的提交。

- [ ] **步骤 2：实现题库练习**

加载 `/api/problems`；点击题目加载 `/api/problems/{id}`；提交代码到 `/api/problems/{id}/submissions`；轮询 `/api/submissions/{submissionId}`。

- [ ] **步骤 3：实现考试模式**

加载 `/api/exams`；点击考试加载 `/api/exams/{id}` 和 `/api/exams/{id}/my-result`；提交到 `/api/exams/{examId}/problems/{problemId}/submissions`。

- [ ] **步骤 4：实现我的提交**

加载 `/api/submissions/my`，表格展示 id、problemId、examId、status、score、timeUsedMs、createdAt。

## 任务 5：管理员端页面

**文件：**
- 创建：`clearn-web/src/layouts/AdminLayout.vue`
- 创建：`clearn-web/src/views/admin/ProblemManageView.vue`
- 创建：`clearn-web/src/views/admin/ExamManageView.vue`
- 创建：`clearn-web/src/views/admin/ExamResultView.vue`

- [ ] **步骤 1：实现管理员布局**

顶部展示用户和退出；左侧菜单包含题目管理、考试管理、考试结果。

- [ ] **步骤 2：实现题目管理**

保留创建题目表单；提交 `/api/admin/problems`；保存后刷新 `/api/problems`。

- [ ] **步骤 3：实现考试管理**

保留创建考试表单；加载 `/api/admin/exams`；支持向选中考试添加题目 `/api/admin/exams/{id}/problems`。

- [ ] **步骤 4：实现考试结果**

加载 `/api/admin/exams`；选择考试后请求 `/api/admin/exams/{id}/results`。

## 任务 6：样式和构建验证

**文件：**
- 修改：`clearn-web/src/styles.css`

- [ ] **步骤 1：改为正式后台布局样式**

实现 `.portal-shell`、`.portal-sidebar`、`.portal-main`、`.toolbar`、`.content-grid`、`.code-editor` 等类。保持卡片半径不超过 8px，移动端单列。

- [ ] **步骤 2：运行测试**

运行：`cd clearn-web && npm.cmd test -- --run`
预期：PASS。

- [ ] **步骤 3：运行构建**

运行：`cd clearn-web && npm.cmd run build`
预期：`vite build` 成功。
