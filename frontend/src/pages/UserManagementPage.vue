<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/knowledge-points">&#30693;&#35782;&#28857;&#31649;&#29702;</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">管理员工作台</p>
          <h1>用户管理</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadUsers">刷新用户</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <article class="table-panel">
        <div class="panel-header">
          <h2>平台账号</h2>
          <span class="panel-count">{{ users.length }} 个</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>ID</th>
              <th>账号</th>
              <th>用户名</th>
              <th>角色</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.id }}</td>
              <td>{{ user.username }}</td>
              <td>{{ user.displayName }}</td>
              <td>{{ roleText(user.role) }}</td>
            </tr>
            <tr v-if="users.length === 0">
              <td colspan="4">暂无用户。</td>
            </tr>
          </tbody>
        </table>
        <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
      </article>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listUsers, type UserSummary } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'

const users = ref<UserSummary[]>([])
const statusMessage = ref('')

onMounted(loadUsers)

async function loadUsers() {
  statusMessage.value = ''
  try {
    users.value = await listUsers()
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载用户失败，请检查本地后端是否启动。'
  }
}

function roleText(role: string) {
  return role === 'ADMIN' ? '管理员' : '普通用户'
}
</script>
