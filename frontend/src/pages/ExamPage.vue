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
          <p class="eyebrow">个人考试卷</p>
          <h1>考试中心</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">练习模式</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>可选题目</h2>
            <span class="panel-count">{{ selectedQuestionIds.length }} 题已选</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>选择</th>
                <th>题目</th>
                <th>知识点</th>
                <th>难度</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in availableQuestions" :key="question.id">
                <td>
                  <input v-model="selectedQuestionIds" type="checkbox" :value="question.id" />
                </td>
                <td>{{ questionStem(question.title) }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ difficultyLabel(question.difficulty) }}</td>
              </tr>
              <tr v-if="availableQuestions.length === 0">
                <td colspan="4">题库中暂无可用题目。</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside class="workspace-panel">
          <h2>自定义组卷</h2>
          <form class="question-form" @submit.prevent="createPaper">
            <label>
              试卷名称
              <input v-model="draft.name" required maxlength="128" />
            </label>
            <label>
              时长（分钟）
              <input v-model.number="draft.durationMinutes" type="number" min="1" max="480" required />
            </label>
            <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
            <button type="submit" :disabled="creating">
              {{ creating ? '正在生成...' : '生成考试卷' }}
            </button>
          </form>

          <section v-if="createdPaper" class="paper-summary" aria-label="已生成考试卷">
            <h3>{{ createdPaper.name }}</h3>
            <p>{{ createdPaper.durationMinutes }} 分钟</p>
            <p>共 {{ createdPaper.questions.length }} 题</p>
            <RouterLink class="button-link" :to="`/exams/${createdPaper.id}/take`">进入答题</RouterLink>
          </section>
        </aside>
      </section>

      <section class="table-panel exam-history-panel" aria-label="考试记录">
        <div class="panel-header">
          <h2>考试记录</h2>
          <span class="panel-count">{{ histories.length }} 次</span>
        </div>
        <table>
          <thead>
            <tr>
              <th>试卷</th>
              <th>进度</th>
              <th>状态</th>
              <th>成绩</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="history in histories" :key="history.id">
              <td>
                <strong>{{ history.name }}</strong>
                <small>{{ formatDateTime(history.startedAt) }}</small>
              </td>
              <td>{{ history.answeredCount }} / {{ history.questionCount }} 题</td>
              <td>{{ history.status === 'SUBMITTED' ? '已提交' : '进行中' }}</td>
              <td>{{ history.status === 'SUBMITTED' ? `${history.score ?? 0} / ${history.totalScore ?? 0}` : '--' }}</td>
              <td>
                <RouterLink class="table-action" :to="`/exams/${history.id}/take`">
                  {{ history.status === 'SUBMITTED' ? '查看结果' : '继续答题' }}
                </RouterLink>
              </td>
            </tr>
            <tr v-if="histories.length === 0">
              <td colspan="5">还没有考试记录，先从题库组合一套试卷。</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  composeCustomExam,
  listExamSessions,
  searchQuestions,
  type CustomExamPaper,
  type ExamSummary,
  type Question
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()
const availableQuestions = ref<Question[]>([])
const histories = ref<ExamSummary[]>([])
const selectedQuestionIds = ref<number[]>([])
const statusMessage = ref('')
const createdPaper = ref<CustomExamPaper | null>(null)
const creating = ref(false)
const draft = reactive({
  name: '集合专项测试',
  durationMinutes: 45
})

onMounted(async () => {
  await Promise.all([loadQuestions(), loadHistory()])
})

async function loadQuestions() {
  try {
    availableQuestions.value = await searchQuestions()
    selectedQuestionIds.value = availableQuestions.value.map((question) => question.id)
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载题库失败，请检查本地后端是否启动。')
  }
}

async function loadHistory() {
  try {
    histories.value = await listExamSessions()
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载考试记录失败，请检查本地后端是否启动。')
  }
}

async function createPaper() {
  statusMessage.value = ''
  createdPaper.value = null
  if (!draft.name.trim()) {
    statusMessage.value = '请输入试卷名称。'
    return
  }
  if (selectedQuestionIds.value.length === 0) {
    statusMessage.value = '请至少选择一道题。'
    return
  }

  creating.value = true
  try {
    const created = await composeCustomExam({
      name: draft.name.trim(),
      durationMinutes: draft.durationMinutes,
      questionIds: selectedQuestionIds.value
    })
    createdPaper.value = created
    histories.value = [toSummary(created), ...histories.value.filter((item) => item.id !== created.id)]
    statusMessage.value = '考试卷已生成，答题进度会自动保存。'
  } catch (error) {
    statusMessage.value = errorMessage(error, '生成考试卷失败，请检查本地后端是否启动。')
  } finally {
    creating.value = false
  }
}

function toSummary(paper: CustomExamPaper): ExamSummary {
  return {
    id: paper.id,
    name: paper.name,
    durationMinutes: paper.durationMinutes,
    status: paper.status,
    questionCount: paper.questions.length,
    answeredCount: paper.questions.filter((question) => question.submittedAnswer.trim()).length,
    startedAt: paper.startedAt,
    expiresAt: paper.expiresAt,
    submittedAt: paper.submittedAt,
    score: paper.score,
    totalScore: paper.totalScore
  }
}

function questionStem(title: string) {
  return title.split(/\r?\n/).find((line) => line.trim())?.trim() ?? title
}

function difficultyLabel(difficulty: string) {
  return ({ BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '精通' } as Record<string, string>)[difficulty] ?? difficulty
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(new Date(value))
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}
</script>
