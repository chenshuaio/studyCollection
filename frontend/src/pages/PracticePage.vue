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
          <p class="eyebrow">自适应刷题</p>
          <h1>练习中心</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="resetPractice">生成练习</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="practice-layout">
        <article class="workspace-panel practice-question">
          <p v-if="retryTarget" class="eyebrow">错题重练</p>
          <div class="question-meta" v-if="currentQuestion">
            <span>{{ currentQuestion.type }}</span>
            <span>{{ currentQuestion.knowledgePoint }}</span>
            <span>{{ currentQuestion.difficulty }}</span>
          </div>
          <h2>{{ currentQuestion?.title ?? '暂无可练习题目' }}</h2>
          <form v-if="currentQuestion && hasOptions(currentQuestion)" class="option-list" aria-label="练习题选项">
            <label v-for="option in currentQuestion.options" :key="option.value">
              <input
                v-model="selectedAnswer"
                :type="currentQuestion.type === 'MULTIPLE_CHOICE' ? 'checkbox' : 'radio'"
                name="practice-question"
                :value="option.value"
                :disabled="submitted"
              />
              <span>{{ option.value }}. {{ option.label }}</span>
            </label>
          </form>
          <label v-else class="answer-field">
            作答
            <input
              v-model="selectedAnswer"
              aria-label="练习答案"
              :disabled="submitted"
              placeholder="请输入你的答案"
            />
          </label>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
          <button type="button" @click="submitAnswer">提交答案</button>
        </article>

        <aside class="workspace-panel score-panel">
          <h2>得分</h2>
          <strong>{{ submitted ? scoreText : '--/--' }}</strong>
          <p>{{ submitted ? resultText : '提交后会显示本次练习结果。' }}</p>
          <div class="progress-track" aria-label="正确率">
            <span :style="{ width: submitted ? progressWidth : '0%' }"></span>
          </div>
        </aside>

        <article class="workspace-panel analysis-panel">
          <h2>答案解析</h2>
          <p>{{ submitted ? analysisText : '提交答案后展示解析，并自动进入错题整理候选。' }}</p>
          <dl>
            <div>
              <dt>你的答案</dt>
              <dd>{{ submitted ? selectedAnswer : '未提交' }}</dd>
            </div>
            <div>
              <dt>{{ isAutoGraded ? '标准答案' : '参考答案' }}</dt>
              <dd>{{ submitted ? correctAnswer : '提交后可见' }}</dd>
            </div>
            <div>
              <dt>错题反馈</dt>
              <dd>{{ submitted && isAutoGraded && !isCorrect ? '可提交给管理员复核题目或解析。' : '暂无反馈' }}</dd>
            </div>
          </dl>
        </article>

        <article class="workspace-panel analysis-panel">
          <h2>反馈题目问题</h2>
          <p>如果你认为答案、解析或题干有误，可以提交给管理员审核。</p>
          <textarea
            class="feedback-editor"
            v-model="feedbackContent"
            aria-label="题目反馈内容"
            placeholder="例如：标准答案应为 B，当前解析遗漏了扩容阈值。"
          ></textarea>
          <p v-if="feedbackStatus" class="form-message">{{ feedbackStatus }}</p>
          <button type="button" @click="sendFeedback">提交反馈</button>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  recordMistake,
  searchQuestions,
  submitQuestionFeedback,
  submitUserPractice,
  type PracticeResult,
  type Question
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()

type ChoiceOption = {
  value: string
  label: string
}

type PracticeQuestion = Question & {
  options?: ChoiceOption[]
}

const questions = ref<PracticeQuestion[]>([])
const retryTarget = ref<RetryMistakeTarget | null>(loadRetryTarget())
const currentQuestion = computed(() => questions.value[0] ?? null)
const selectedAnswer = ref<string | string[]>('')
const submitted = ref(false)
const statusMessage = ref('')
const feedbackStatus = ref('')
const feedbackContent = ref('标准答案或解析可能有误，请管理员复核。')
const backendResult = ref<PracticeResult | null>(null)

type RetryMistakeTarget = {
  questionId: number
  questionTitle: string
}

const firstItem = computed(() => backendResult.value?.items[0])
const isAutoGraded = computed(() => firstItem.value?.autoGraded ?? false)
const isCorrect = computed(() => firstItem.value?.correct === true)
const correctAnswer = computed(() => firstItem.value?.correctAnswer ?? currentQuestion.value?.answer ?? '')
const analysisText = computed(() => firstItem.value?.analysis ?? currentQuestion.value?.analysis ?? '')
const scoreText = computed(() => {
  if (!isAutoGraded.value) {
    return '不评分'
  }
  if (!backendResult.value) {
    return isCorrect.value ? '10/10' : '0/10'
  }
  return `${backendResult.value.score}/${backendResult.value.totalScore}`
})
const progressWidth = computed(() => (isAutoGraded.value && isCorrect.value ? '100%' : '0%'))
const resultText = computed(() => {
  if (!isAutoGraded.value) {
    return '本题不自动评分，请结合参考答案自行核对。'
  }
  return isCorrect.value ? '回答正确，继续保持。' : '回答错误，已加入错题整理候选。'
})

onMounted(loadPracticeQuestion)

async function loadPracticeQuestion() {
  statusMessage.value = ''
  try {
    questions.value = prioritizeRetryQuestion(await searchQuestions()).map(normalizePracticeQuestion)
    selectedAnswer.value = currentQuestion.value?.type === 'MULTIPLE_CHOICE' ? [] : ''
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载练习题失败，请检查本地后端是否启动。'
  }
}

function loadRetryTarget() {
  const raw = window.sessionStorage.getItem('studyCollectionRetryMistake')
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as RetryMistakeTarget
  } catch {
    window.sessionStorage.removeItem('studyCollectionRetryMistake')
    return null
  }
}

function prioritizeRetryQuestion(loadedQuestions: Question[]) {
  if (!retryTarget.value) {
    return loadedQuestions
  }
  const targetIndex = loadedQuestions.findIndex((question) => question.id === retryTarget.value?.questionId)
  if (targetIndex === -1) {
    statusMessage.value = '未找到这道错题，已切换为普通练习。'
    retryTarget.value = null
    window.sessionStorage.removeItem('studyCollectionRetryMistake')
    return loadedQuestions
  }
  const target = loadedQuestions[targetIndex]
  return [target, ...loadedQuestions.filter((question) => question.id !== target.id)]
}

function normalizePracticeQuestion(question: Question): PracticeQuestion {
  const parsed = parseChoiceOptions(question.title)
  if (parsed.options.length >= 2) {
    return { ...question, title: parsed.title, options: parsed.options }
  }
  if (question.type === 'TRUE_FALSE') {
    return {
      ...question,
      options: [
        { value: 'true', label: '正确' },
        { value: 'false', label: '错误' }
      ]
    }
  }
  if (question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE') {
    return {
      ...question,
      options: ['A', 'B', 'C', 'D'].map((value) => ({
        value,
        label: `选项 ${value}（原题未提供选项内容）`
      }))
    }
  }
  return question
}

function parseChoiceOptions(title: string) {
  const lines = title.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const options: ChoiceOption[] = []
  const stemLines: string[] = []

  lines.forEach((line) => {
    const match = line.match(/^([A-D])[\.\u3001\uff0e]\s*(.+)$/i)
    if (match) {
      options.push({ value: match[1].toUpperCase(), label: match[2].trim() })
    } else {
      stemLines.push(line)
    }
  })

  return { title: stemLines.join('\n'), options }
}

function hasOptions(question: PracticeQuestion) {
  return Array.isArray(question.options) && question.options.length > 0
}

async function submitAnswer() {
  statusMessage.value = ''
  if (!currentQuestion.value) {
    statusMessage.value = '暂无可提交的练习题。'
    return
  }
  if (!hasSelectedAnswer()) {
    statusMessage.value = '请先填写答案。'
    return
  }
  try {
    backendResult.value = await submitUserPractice([{
      questionId: currentQuestion.value.id,
      answer: serializedAnswer()
    }])
    submitted.value = true
    const item = backendResult.value.items[0]
    if (item?.autoGraded && item.correct === false) {
      await recordMistake({
        questionId: currentQuestion.value.id,
        questionTitle: currentQuestion.value.title,
        knowledgePoint: currentQuestion.value.knowledgePoint,
        status: 'PENDING'
      })
    }
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '提交失败，请检查本地后端是否启动。'
  }
}

async function sendFeedback() {
  feedbackStatus.value = ''
  if (!currentQuestion.value) {
    feedbackStatus.value = '暂无可反馈的题目。'
    return
  }
  try {
    await submitQuestionFeedback({
      questionId: currentQuestion.value.id,
      type: 'ANSWER_ERROR',
      content: feedbackContent.value
    })
    feedbackStatus.value = '反馈已提交，管理员可在反馈审核页处理。'
  } catch (error) {
    feedbackStatus.value = error instanceof Error ? error.message : '反馈提交失败，请检查本地后端是否启动。'
  }
}

function resetPractice() {
  selectedAnswer.value = ''
  submitted.value = false
  backendResult.value = null
  statusMessage.value = ''
  feedbackStatus.value = ''
  loadPracticeQuestion()
}

function hasSelectedAnswer() {
  return Array.isArray(selectedAnswer.value)
    ? selectedAnswer.value.length > 0
    : selectedAnswer.value.trim().length > 0
}

function serializedAnswer() {
  return Array.isArray(selectedAnswer.value)
    ? [...selectedAnswer.value].sort().join(',')
    : selectedAnswer.value
}
</script>
