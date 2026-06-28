<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">管理员工作台</p>
          <h1>反馈审核</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadFeedback">刷新反馈</button>
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>待处理反馈</h2>
            <span class="panel-count">{{ feedbackItems.length }} 条</span>
          </div>

          <table>
            <thead>
              <tr>
                <th>题目 ID</th>
                <th>类型</th>
                <th>反馈内容</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="item in feedbackItems"
                :key="item.id"
                :class="{ selected: selectedFeedbackId === item.id }"
                @click="selectedFeedbackId = item.id"
              >
                <td>{{ item.questionId }}</td>
                <td>{{ typeText(item.type) }}</td>
                <td>{{ item.content }}</td>
                <td>{{ statusText(item.status) }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="workspace-panel question-form">
          <h2>采纳反馈</h2>
          <label>
            修订说明
            <textarea v-model="changeSummary" aria-label="修订说明"></textarea>
          </label>
          <label>
            审核备注
            <textarea v-model="reviewNote" aria-label="审核备注"></textarea>
          </label>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
          <button type="button" @click="acceptSelected">采纳并记录修订</button>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { acceptQuestionFeedback, listPendingFeedback, type QuestionFeedback } from '../api'
import LogoutButton from '../components/LogoutButton.vue'

const feedbackItems = ref<QuestionFeedback[]>([])
const selectedFeedbackId = ref<number | null>(null)
const changeSummary = ref('答案从 A 修改为 B')
const reviewNote = ref('用户反馈属实')
const statusMessage = ref('')

onMounted(loadFeedback)

async function loadFeedback() {
  statusMessage.value = ''
  try {
    feedbackItems.value = await listPendingFeedback()
    selectedFeedbackId.value = feedbackItems.value[0]?.id ?? null
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载反馈失败，请检查本地后端是否启动。'
  }
}

async function acceptSelected() {
  if (!selectedFeedbackId.value) {
    statusMessage.value = '请选择一条待处理反馈。'
    return
  }

  try {
    await acceptQuestionFeedback(selectedFeedbackId.value, {
      adminUserId: 1,
      changeSummary: changeSummary.value,
      reviewNote: reviewNote.value
    })
    statusMessage.value = '反馈已采纳并记录修订。'
    await loadFeedback()
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '采纳反馈失败，请检查本地后端是否启动。'
  }
}

function typeText(type: string) {
  return type === 'ANSWER_ERROR' ? '答案错误' : type
}

function statusText(status: string) {
  return status === 'PENDING' ? '待处理' : status
}
</script>
