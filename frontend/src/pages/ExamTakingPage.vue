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
          <p class="eyebrow">考试答题</p>
          <h1>{{ paper?.name ?? '考试答题' }}</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/exams">返回组卷</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section v-if="!paper" class="workspace-panel empty-state">
        <h2>还没有可答的考试卷</h2>
        <p>请先在考试中心选择题目并生成考试卷。</p>
        <RouterLink class="button-link" to="/exams">去组卷</RouterLink>
      </section>

      <section v-else class="exam-taking-layout">
        <article class="workspace-panel exam-paper-panel">
          <div class="paper-toolbar">
            <span>{{ paper.durationMinutes }} 分钟</span>
            <span>共 {{ paper.questions.length }} 题</span>
          </div>

          <section v-for="(question, index) in paper.questions" :key="question.id" class="exam-question-card">
            <div class="question-meta">
              <span>第 {{ index + 1 }} 题</span>
              <span>{{ question.knowledgePoint }}</span>
              <span>{{ question.difficulty }}</span>
            </div>
            <h2>{{ question.title }}</h2>
            <form v-if="hasOptions(question)" class="option-list" :aria-label="`第 ${index + 1} 题选项`">
              <label v-for="option in question.options" :key="option.value">
                <input
                  v-model="answers[question.id]"
                  type="radio"
                  :name="`question-${question.id}`"
                  :value="option.value"
                  :disabled="submitted"
                />
                <span>{{ option.value }}. {{ option.label }}</span>
              </label>
            </form>
            <label v-else class="answer-field">
              作答
              <input
                v-model="answers[question.id]"
                :aria-label="`第 ${index + 1} 题答案`"
                :disabled="submitted"
                placeholder="请输入你的答案"
              />
            </label>
          </section>

          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
          <button type="button" :disabled="submitted" @click="submitExam">提交试卷</button>
        </article>

        <aside class="workspace-panel score-panel">
          <h2>考试结果</h2>
          <strong>{{ result ? `${result.score}/${result.totalScore}` : '--/--' }}</strong>
          <p>{{ result ? resultSummary : '提交后会显示本次考试得分和解析。' }}</p>
          <div class="progress-track" aria-label="得分率">
            <span :style="{ width: result ? scorePercent : '0%' }"></span>
          </div>
        </aside>

        <article v-if="result" class="workspace-panel analysis-panel">
          <h2>逐题解析</h2>
          <dl>
            <div v-for="item in result.items" :key="item.questionId">
              <dt>{{ questionTitle(item.questionId) }}</dt>
              <dd>
                你的答案：{{ item.submittedAnswer }}；标准答案：{{ item.correctAnswer }}。
                {{ item.correct ? '回答正确。' : '回答错误，已加入错题整理。' }}
                {{ item.analysis }}
              </dd>
            </div>
          </dl>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { recordMistake, submitUserPractice, type PracticeResult } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'
import { getCurrentUser } from '../session'

const isAdminUser = isAdmin()

type ExamQuestion = {
  id: number
  title: string
  type: string
  difficulty: string
  knowledgePoint: string
  answer: string
  analysis: string
  options?: Array<{
    value: string
    label: string
  }>
}

type StoredExamPaper = {
  name: string
  durationMinutes: number
  questionIds: number[]
  questions: ExamQuestion[]
}

const paper = ref<StoredExamPaper | null>(loadPaper())
const answers = reactive<Record<number, string>>({})
const result = ref<PracticeResult | null>(null)
const submitted = ref(false)
const statusMessage = ref('')
const currentUserId = getCurrentUser()?.userId ?? 7

if (paper.value) {
  paper.value.questions.forEach((question) => {
    answers[question.id] = ''
  })
}

const scorePercent = computed(() => {
  if (!result.value || result.value.totalScore === 0) {
    return '0%'
  }
  return `${Math.round((result.value.score / result.value.totalScore) * 100)}%`
})
const resultSummary = computed(() => {
  if (!result.value) {
    return ''
  }
  const wrongCount = result.value.items.filter((item) => !item.correct).length
  return wrongCount === 0 ? '全部答对，继续保持。' : `${wrongCount} 道题需要复盘，已同步到错题整理。`
})

async function submitExam() {
  if (!paper.value) {
    return
  }
  statusMessage.value = ''
  if (paper.value.questions.some((question) => !answers[question.id])) {
    statusMessage.value = '请先完成所有题目后再提交。'
    return
  }
  try {
    result.value = await submitUserPractice(
      currentUserId,
      paper.value.questions.map((question) => ({
        questionId: question.id,
        answer: answers[question.id],
        correctAnswer: question.answer,
        analysis: question.analysis
      }))
    )
    submitted.value = true
    const wrongItems = result.value.items.filter((item) => !item.correct)
    await Promise.all(
      wrongItems.map((item) => {
        const question = paper.value?.questions.find((candidate) => candidate.id === item.questionId)
        if (!question) {
          return Promise.resolve()
        }
        return recordMistake({
          userId: 7,
          questionId: question.id,
          questionTitle: question.title,
          knowledgePoint: question.knowledgePoint,
          status: 'PENDING'
        })
      })
    )
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '提交试卷失败，请检查本地后端是否启动。'
  }
}

function questionTitle(questionId: number) {
  return paper.value?.questions.find((question) => question.id === questionId)?.title ?? `题目 ${questionId}`
}

function hasOptions(question: ExamQuestion) {
  return Array.isArray(question.options) && question.options.length > 0
}

function loadPaper() {
  const saved = window.sessionStorage.getItem('studyCollectionExamPaper')
  if (!saved) {
    return null
  }
  try {
    const parsed = JSON.parse(saved) as StoredExamPaper
    return Array.isArray(parsed.questions) && parsed.questions.length > 0 ? parsed : null
  } catch {
    return null
  }
}
</script>
