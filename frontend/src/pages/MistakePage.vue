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

      <section class="metric-grid" aria-label="错题统计">
        <article>
          <span>全部错题</span>
          <strong>{{ summaryCounts.total }}</strong>
        </article>
        <article>
          <span>待巩固</span>
          <strong>{{ summaryCounts.pending }}</strong>
        </article>
        <article>
          <span>已掌握</span>
          <strong>{{ summaryCounts.mastered }}</strong>
        </article>
      </section>

      <section class="filter-bar" aria-label="错题筛选">
        <label>
          知识点
          <select v-model="filters.knowledgePoint" aria-label="错题知识点筛选">
            <option value="">全部</option>
            <option v-for="knowledgePoint in knowledgePoints" :key="knowledgePoint" :value="knowledgePoint">
              {{ knowledgePoint }}
            </option>
          </select>
        </label>
        <label>
          掌握状态
          <select v-model="filters.status" aria-label="错题状态筛选">
            <option value="">全部</option>
            <option value="PENDING">待巩固</option>
            <option value="MASTERED">已掌握</option>
          </select>
        </label>
      </section>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>错题列表</h2>
            <span class="panel-count">{{ filteredMistakes.length }} 题</span>
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
              <tr v-for="mistake in filteredMistakes" :key="mistake.questionId">
                <td>{{ mistake.questionTitle }}</td>
                <td>{{ mistake.knowledgePoint }}</td>
                <td>{{ statusText(mistake.status) }}</td>
                <td>
                  <div class="action-row">
                    <RouterLink
                      class="button-link"
                      to="/practice"
                      :aria-label="`重练 ${mistake.questionTitle}`"
                      @click="prepareRetryPractice(mistake)"
                    >
                      重新练习
                    </RouterLink>
                    <button
                      type="button"
                      :aria-label="statusActionLabel(mistake.status)"
                      @click="toggleMistakeStatus(mistake)"
                    >
                      {{ statusActionLabel(mistake.status) }}
                    </button>
                  </div>
                </td>
              </tr>
              <tr v-if="filteredMistakes.length === 0">
                <td colspan="4">暂无符合条件的错题。</td>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { listMistakes, updateMistakeStatus, type MistakeRecord } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'
import { getCurrentUser } from '../session'

const isAdminUser = isAdmin()
const currentUserId = getCurrentUser()?.userId ?? 7

const filters = reactive({
  knowledgePoint: '',
  status: ''
})
const mistakes = ref<MistakeRecord[]>([])
const statusMessage = ref('')

const knowledgePoints = computed(() => {
  return Array.from(new Set(mistakes.value.map((mistake) => mistake.knowledgePoint))).sort()
})

const filteredMistakes = computed(() => {
  return mistakes.value
    .filter((mistake) => !filters.knowledgePoint || mistake.knowledgePoint === filters.knowledgePoint)
    .filter((mistake) => !filters.status || mistake.status === filters.status)
})

const summaryCounts = computed(() => ({
  total: mistakes.value.length,
  pending: mistakes.value.filter((mistake) => mistake.status === 'PENDING').length,
  mastered: mistakes.value.filter((mistake) => mistake.status === 'MASTERED').length
}))

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
  if (status === 'PENDING') {
    return '待巩固'
  }
  if (status === 'MASTERED') {
    return '已掌握'
  }
  return status
}

function statusActionLabel(status: string) {
  return status === 'MASTERED' ? '重新标记待巩固' : '标记已掌握'
}

async function toggleMistakeStatus(mistake: MistakeRecord) {
  statusMessage.value = ''
  const nextStatus = mistake.status === 'MASTERED' ? 'PENDING' : 'MASTERED'
  try {
    await updateMistakeStatus({
      userId: currentUserId,
      questionId: mistake.questionId,
      status: nextStatus
    })
    await loadMistakes()
    statusMessage.value = nextStatus === 'MASTERED' ? '已标记为已掌握。' : '已重新标记为待巩固。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '更新错题状态失败。'
  }
}

function prepareRetryPractice(mistake: MistakeRecord) {
  window.sessionStorage.setItem('studyCollectionRetryMistake', JSON.stringify({
    questionId: mistake.questionId,
    questionTitle: mistake.questionTitle
  }))
}
</script>
