<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink v-if="isAdminUser" to="/questions">题库管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
        <RouterLink v-if="isAdminUser" to="/feedback">反馈审核</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">薄弱点巩固</p>
          <h1>错题本</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadMistakes">刷新错题</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>待巩固错题</h2>
            <span class="panel-count">{{ mistakes.length }} 题</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>知识点</th>
                <th>掌握状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="mistake in mistakes" :key="mistake.questionId">
                <td>{{ mistake.questionTitle }}</td>
                <td>{{ mistake.knowledgePoint }}</td>
                <td>{{ statusText(mistake.status) }}</td>
                <td><RouterLink class="button-link" to="/practice">重新练习</RouterLink></td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside class="workspace-panel">
          <h2>强化建议</h2>
          <p>优先重练同一知识点错题，连续答对后再标记为已掌握。</p>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
          <RouterLink class="button-link" to="/reports">查看学习报告</RouterLink>
        </aside>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listMistakes, type MistakeRecord } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'
import { getCurrentUser } from '../session'

const isAdminUser = isAdmin()
const currentUserId = getCurrentUser()?.userId ?? 7

const mistakes = ref<MistakeRecord[]>([])
const statusMessage = ref('')

onMounted(loadMistakes)

async function loadMistakes() {
  statusMessage.value = ''
  try {
    mistakes.value = await listMistakes(currentUserId)
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载错题失败，请检查本地后端是否启动。'
  }
}

function statusText(status: string) {
  return status === 'PENDING' ? '待巩固' : status
}
</script>
