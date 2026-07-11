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
          <RouterLink class="button-link" to="/exams">返回考试中心</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section v-if="loading" class="workspace-panel empty-state" aria-live="polite">
        <h2>正在加载考试...</h2>
      </section>

      <section v-else-if="!paper" class="workspace-panel empty-state">
        <h2>无法加载考试记录</h2>
        <p>{{ statusMessage || '请返回考试中心重新选择。' }}</p>
        <RouterLink class="button-link" to="/exams">返回考试中心</RouterLink>
      </section>

      <section v-else class="exam-taking-layout">
        <article class="workspace-panel exam-paper-panel">
          <div class="paper-toolbar">
            <span>{{ paper.durationMinutes }} 分钟</span>
            <span>共 {{ paper.questions.length }} 题</span>
            <span class="exam-countdown" aria-live="polite">剩余 {{ formattedRemainingTime }}</span>
            <span>{{ submitted ? '已提交' : '答题中' }}</span>
          </div>

          <section v-for="(question, index) in paper.questions" :key="question.id" class="exam-question-card">
            <div class="question-meta">
              <span>第 {{ index + 1 }} 题</span>
              <span>{{ questionTypeLabel(question.type) }}</span>
              <span>{{ question.knowledgePoint }}</span>
              <span>{{ difficultyLabel(question.difficulty) }}</span>
            </div>
            <h2>{{ question.title }}</h2>
            <form v-if="hasOptions(question)" class="option-list" :aria-label="`第 ${index + 1} 题选项`">
              <label v-for="option in question.options" :key="option.value">
                <input
                  v-model="answers[question.id]"
                  :type="question.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'"
                  :name="`question-${question.id}`"
                  :value="option.value"
                  :disabled="submitted"
                  @change="persistAnswer(question)"
                />
                <span>{{ option.value }}. {{ option.label }}</span>
              </label>
            </form>
            <label v-else class="answer-field">
              作答
              <textarea
                v-model="answers[question.id]"
                :aria-label="`第 ${index + 1} 题答案`"
                :disabled="submitted"
                :placeholder="question.type === 'PROGRAMMING' ? '请输入 Java 代码或解题思路' : '请输入你的答案'"
                @input="scheduleSave(question)"
                @change="persistAnswer(question)"
              ></textarea>
            </label>
            <small v-if="!submitted && saveStates[question.id]" class="answer-save-state" aria-live="polite">
              {{ saveStateLabel(saveStates[question.id]) }}
            </small>
          </section>

          <p v-if="statusMessage" class="form-message" aria-live="polite">{{ statusMessage }}</p>
          <button
            type="button"
            data-action="submit-exam"
            :disabled="submitted || submitting"
            @click="submitExam(false)"
          >
            {{ submitting ? '正在提交...' : submitted ? '试卷已提交' : '提交试卷' }}
          </button>
        </article>

        <aside class="workspace-panel score-panel">
          <h2>考试结果</h2>
          <strong>{{ submitted ? `${paper.score ?? 0}/${paper.totalScore ?? 0}` : '--/--' }}</strong>
          <p>{{ submitted ? resultSummary : '客观题自动评分，主观题提交后对照参考答案。' }}</p>
          <div class="progress-track" aria-label="得分率">
            <span :style="{ width: submitted ? scorePercent : '0%' }"></span>
          </div>
        </aside>

        <article v-if="submitted" class="workspace-panel analysis-panel">
          <h2>逐题解析</h2>
          <dl>
            <div v-for="question in paper.questions" :key="question.id">
              <dt>{{ question.title }}</dt>
              <dd v-if="question.autoGraded">
                你的答案：{{ displayAnswer(question.submittedAnswer) }}；标准答案：{{ question.correctAnswer }}。
                {{ question.correct ? '回答正确。' : '回答错误，已加入错题整理。' }}
                {{ question.analysis }}
              </dd>
              <dd v-else>
                你的答案：{{ displayAnswer(question.submittedAnswer) }}；参考答案：{{ question.correctAnswer }}。
                本题不自动评分，请结合参考答案自行核对。{{ question.analysis }}
              </dd>
            </div>
          </dl>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { RouterLink, useRoute } from 'vue-router'
import {
  getExamSession,
  recordMistake,
  saveExamAnswer,
  submitExamSession,
  type ExamQuestion,
  type ExamSession
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

type AnswerValue = string | string[]
type SaveState = 'saving' | 'saved' | 'error'

const route = useRoute()
const isAdminUser = isAdmin()
const paper = ref<ExamSession | null>(null)
const answers = reactive<Record<number, AnswerValue>>({})
const saveStates = reactive<Record<number, SaveState | undefined>>({})
const loading = ref(true)
const submitting = ref(false)
const statusMessage = ref('')
const remainingSeconds = ref(0)
const mistakeSyncFailedCount = ref(0)
const recordedMistakes = new Set<number>()
const saveTimers = new Map<number, number>()
let countdownTimer: number | null = null
let finalSavePromise: Promise<void> | null = null

const examId = Number(route.params.examId)
const submitted = computed(() => paper.value?.status === 'SUBMITTED')
const formattedRemainingTime = computed(() => formatDuration(remainingSeconds.value))
const scorePercent = computed(() => {
  const totalScore = paper.value?.totalScore ?? 0
  if (!submitted.value || totalScore === 0) {
    return '0%'
  }
  return `${Math.round(((paper.value?.score ?? 0) / totalScore) * 100)}%`
})
const resultSummary = computed(() => {
  if (!paper.value) {
    return ''
  }
  const wrongCount = paper.value.questions.filter((question) => question.autoGraded && question.correct === false).length
  const subjectiveCount = paper.value.questions.filter((question) => !question.autoGraded).length
  if (wrongCount > 0) {
    const syncText = mistakeSyncFailedCount.value > 0
      ? `${mistakeSyncFailedCount.value} 道错题同步失败，可稍后重新打开结果页重试。`
      : '已同步到错题整理。'
    return `${wrongCount} 道客观题需要复盘，${syncText}${subjectiveCount ? `另有 ${subjectiveCount} 道主观题需自行核对。` : ''}`
  }
  return subjectiveCount > 0 ? `客观题已完成，另有 ${subjectiveCount} 道主观题需自行核对。` : '全部答对，继续保持。'
})

onMounted(loadExam)
onBeforeUnmount(stopTimers)

async function loadExam() {
  loading.value = true
  statusMessage.value = ''
  if (!Number.isInteger(examId) || examId <= 0) {
    statusMessage.value = '考试编号不正确。'
    loading.value = false
    return
  }
  try {
    applySession(await getExamSession(examId))
    if (submitted.value) {
      const failedCount = await syncMistakes()
      if (failedCount > 0) {
        statusMessage.value = `考试结果已加载，但有 ${failedCount} 道错题同步失败，可稍后重试。`
      }
    }
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载考试失败，请返回考试中心重试。')
  } finally {
    loading.value = false
  }
}

function applySession(session: ExamSession) {
  paper.value = {
    ...session,
    questions: session.questions.map(normalizeExamQuestion)
  }
  remainingSeconds.value = session.remainingSeconds
  Object.keys(answers).forEach((key) => delete answers[Number(key)])
  paper.value.questions.forEach((question) => {
    answers[question.id] = question.type === 'MULTIPLE_CHOICE'
      ? parseMultipleAnswer(question.submittedAnswer)
      : question.submittedAnswer
  })
  if (session.status === 'IN_PROGRESS') {
    startCountdown()
  } else {
    stopCountdown()
  }
}

function startCountdown() {
  stopCountdown()
  if (remainingSeconds.value <= 0 || submitted.value) {
    return
  }
  countdownTimer = window.setInterval(() => {
    remainingSeconds.value = Math.max(0, remainingSeconds.value - 1)
    if (remainingSeconds.value === 1) {
      finalSavePromise = saveAllAnswers(false)
    }
    if (remainingSeconds.value === 0) {
      stopCountdown()
      void submitExam(true)
    }
  }, 1000)
}

function stopCountdown() {
  if (countdownTimer !== null) {
    window.clearInterval(countdownTimer)
    countdownTimer = null
  }
}

function stopTimers() {
  stopCountdown()
  saveTimers.forEach((timer) => window.clearTimeout(timer))
  saveTimers.clear()
}

function scheduleSave(question: ExamQuestion) {
  const existing = saveTimers.get(question.id)
  if (existing) {
    window.clearTimeout(existing)
  }
  saveTimers.set(question.id, window.setTimeout(() => {
    saveTimers.delete(question.id)
    void persistAnswer(question)
  }, 350))
}

async function persistAnswer(question: ExamQuestion) {
  if (!paper.value || submitted.value) {
    return
  }
  const pendingTimer = saveTimers.get(question.id)
  if (pendingTimer) {
    window.clearTimeout(pendingTimer)
    saveTimers.delete(question.id)
  }
  saveStates[question.id] = 'saving'
  try {
    const saved = await saveExamAnswer(paper.value.id, question.id, serializeAnswer(answers[question.id]))
    saveStates[question.id] = 'saved'
    if (saved.status === 'SUBMITTED') {
      applySession(saved)
      const failedCount = await syncMistakes()
      statusMessage.value = failedCount > 0
        ? `考试时间已到，系统已自动提交，但有 ${failedCount} 道错题同步失败。`
        : '考试时间已到，系统已自动提交。'
    }
  } catch (error) {
    saveStates[question.id] = 'error'
    statusMessage.value = errorMessage(error, '答案保存失败，请检查网络后重试。')
  }
}

async function saveAllAnswers(requireComplete: boolean) {
  if (!paper.value || submitted.value) {
    return
  }
  const questions = requireComplete
    ? paper.value.questions
    : paper.value.questions.filter((question) => hasAnswer(answers[question.id]))
  await Promise.all(questions.map((question) => persistAnswer(question)))
}

async function submitExam(automatic: boolean) {
  if (!paper.value || submitted.value || submitting.value) {
    return
  }
  statusMessage.value = ''
  if (!automatic && paper.value.questions.some((question) => !hasAnswer(answers[question.id]))) {
    statusMessage.value = '请先完成所有题目后再提交。'
    return
  }
  submitting.value = true
  try {
    if (finalSavePromise) {
      await finalSavePromise
      finalSavePromise = null
    }
    if (!automatic) {
      await saveAllAnswers(true)
    }
    applySession(await submitExamSession(paper.value.id))
    const failedCount = await syncMistakes()
    if (failedCount > 0) {
      statusMessage.value = `试卷已提交，但有 ${failedCount} 道错题同步失败，可稍后重新打开结果页重试。`
    } else {
      statusMessage.value = automatic ? '考试时间已到，系统已自动提交。' : '试卷提交成功。'
    }
  } catch (error) {
    statusMessage.value = errorMessage(error, '提交试卷失败，请检查本地后端是否启动。')
  } finally {
    submitting.value = false
  }
}

async function syncMistakes() {
  if (!paper.value) {
    return 0
  }
  const wrongQuestions = paper.value.questions.filter(
    (question) => question.autoGraded && question.correct === false && !recordedMistakes.has(question.id)
  )
  const results = await Promise.all(wrongQuestions.map(async (question) => {
    try {
      await recordMistake({
        questionId: question.id,
        questionTitle: question.title,
        knowledgePoint: question.knowledgePoint,
        status: 'PENDING'
      })
      recordedMistakes.add(question.id)
      return true
    } catch {
      return false
    }
  }))
  const failedCount = results.filter((succeeded) => !succeeded).length
  mistakeSyncFailedCount.value = failedCount
  return failedCount
}

function normalizeExamQuestion(question: ExamQuestion): ExamQuestion {
  const parsed = parseChoiceOptions(question.title)
  if (parsed.options.length >= 2) {
    return { ...question, title: parsed.title, options: parsed.options }
  }
  const fallbackOptions = fallbackOptionsFor(question)
  return fallbackOptions.length > 0 ? { ...question, options: fallbackOptions } : question
}

function parseChoiceOptions(title: string) {
  const lines = title.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const options: Array<{ value: string; label: string }> = []
  const stemLines: string[] = []
  lines.forEach((line) => {
    const match = line.match(/^([A-Z])[\.、．]\s*(.+)$/i)
    if (match) {
      options.push({ value: match[1].toUpperCase(), label: match[2].trim() })
    } else {
      stemLines.push(line)
    }
  })
  return { title: stemLines.join('\n'), options }
}

function fallbackOptionsFor(question: ExamQuestion) {
  if (question.type === 'TRUE_FALSE') {
    return [
      { value: 'true', label: '正确' },
      { value: 'false', label: '错误' }
    ]
  }
  if (question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE') {
    return ['A', 'B', 'C', 'D'].map((value) => ({
      value,
      label: `选项 ${value}（原题未提供选项内容）`
    }))
  }
  return []
}

function parseMultipleAnswer(answer: string) {
  const compact = answer.toUpperCase().replace(/[\s,，、;；|/]+/g, '')
  return compact.match(/^[A-Z]+$/) ? [...new Set(compact.split(''))].sort() : []
}

function hasAnswer(answer: AnswerValue | undefined) {
  return Array.isArray(answer) ? answer.length > 0 : Boolean(answer?.trim())
}

function serializeAnswer(answer: AnswerValue | undefined) {
  if (Array.isArray(answer)) {
    return [...answer].sort().join(',')
  }
  return answer ?? ''
}

function hasOptions(question: ExamQuestion) {
  return Array.isArray(question.options) && question.options.length > 0
}

function formatDuration(seconds: number) {
  const safeSeconds = Math.max(0, Math.floor(seconds))
  const hours = Math.floor(safeSeconds / 3600)
  const minutes = Math.floor((safeSeconds % 3600) / 60)
  const remaining = safeSeconds % 60
  return [hours, minutes, remaining].map((value) => String(value).padStart(2, '0')).join(':')
}

function displayAnswer(answer: string) {
  return answer?.trim() || '未作答'
}

function questionTypeLabel(type: string) {
  return ({
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
    PROGRAMMING: '编程题'
  } as Record<string, string>)[type] ?? type
}

function difficultyLabel(difficulty: string) {
  return ({ BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '精通' } as Record<string, string>)[difficulty] ?? difficulty
}

function saveStateLabel(state: SaveState | undefined) {
  return state ? ({ saving: '正在保存...', saved: '已保存', error: '保存失败' } as Record<SaveState, string>)[state] : ''
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof Error ? error.message : fallback
}
</script>
