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
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="filter-bar practice-filter" aria-label="练习生成条件">
        <label>
          知识点
          <select v-model="filters.knowledgePoint" aria-label="知识点筛选">
            <option value="">全部知识点</option>
            <option v-for="point in knowledgePoints" :key="point.id" :value="point.name">{{ point.name }}</option>
          </select>
        </label>
        <label>
          难度
          <select v-model="filters.difficulty" aria-label="难度筛选">
            <option value="">全部难度</option>
            <option value="BEGINNER">入门</option>
            <option value="INTERMEDIATE">进阶</option>
            <option value="ADVANCED">精通</option>
          </select>
        </label>
        <label>
          题型
          <select v-model="filters.type" aria-label="题型筛选">
            <option value="">全部题型</option>
            <option value="SINGLE_CHOICE">单选题</option>
            <option value="MULTIPLE_CHOICE">多选题</option>
            <option value="TRUE_FALSE">判断题</option>
            <option value="FILL_BLANK">填空题</option>
            <option value="SHORT_ANSWER">简答题</option>
            <option value="PROGRAMMING">编程题</option>
          </select>
        </label>
        <label>
          题目数量
          <input v-model.number="filters.count" aria-label="练习题数" type="number" min="1" max="100" />
        </label>
        <button
          type="button"
          data-action="generate-practice"
          :disabled="loading"
          @click="generateFilteredPractice"
        >
          {{ loading ? '生成中...' : '生成练习' }}
        </button>
      </section>

      <p v-if="statusMessage" class="form-message practice-status">{{ statusMessage }}</p>

      <article v-if="finished" class="workspace-panel practice-summary">
        <p class="eyebrow">练习小结</p>
        <h2>本次练习已完成</h2>
        <p>共完成 {{ completedCount }} 题，其中 {{ autoGradedCount }} 题自动评分，答对 {{ correctCount }} 题。</p>
        <button type="button" @click="generateFilteredPractice">按当前条件再练一组</button>
      </article>

      <section v-else class="practice-layout">
        <article class="workspace-panel practice-question">
          <div class="question-progress">
            <p v-if="retryTarget" class="eyebrow">错题重练</p>
            <strong v-if="currentQuestion">第 {{ currentIndex + 1 }} / {{ questions.length }} 题</strong>
          </div>
          <div v-if="currentQuestion" class="question-meta">
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
          <label v-else-if="currentQuestion" class="answer-field">
            作答
            <input
              v-model="selectedAnswer"
              aria-label="练习答案"
              :disabled="submitted"
              placeholder="请输入你的答案"
            />
          </label>
          <div class="action-row practice-actions">
            <button
              v-if="!submitted"
              type="button"
              data-action="submit-answer"
              :disabled="!currentQuestion"
              @click="submitAnswer"
            >
              提交答案
            </button>
            <button
              v-else-if="currentIndex < questions.length - 1"
              type="button"
              data-action="next-question"
              @click="nextQuestion"
            >
              下一题
            </button>
            <button v-else type="button" data-action="finish-practice" @click="finishPractice">完成练习</button>
          </div>
        </article>

        <aside class="workspace-panel score-panel">
          <h2>本题得分</h2>
          <strong>{{ submitted ? scoreText : '--/--' }}</strong>
          <p>{{ submitted ? resultText : '提交后会显示本题结果。' }}</p>
          <div class="progress-track" aria-label="本题正确率">
            <span :style="{ width: submitted ? progressWidth : '0%' }"></span>
          </div>
        </aside>

        <article class="workspace-panel analysis-panel">
          <h2>答案解析</h2>
          <p>{{ submitted ? analysisText : '提交答案后展示解析，并自动整理答错的客观题。' }}</p>
          <dl>
            <div>
              <dt>你的答案</dt>
              <dd>{{ submitted ? displaySelectedAnswer : '未提交' }}</dd>
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
            v-model="feedbackContent"
            class="feedback-editor"
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
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  generatePractice,
  listKnowledgePoints,
  recordMistake,
  searchQuestions,
  submitQuestionFeedback,
  submitUserPractice,
  type GeneratedPracticeQuestion,
  type KnowledgePoint,
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

type PracticeQuestionSource = GeneratedPracticeQuestion | Question
type PracticeQuestion = GeneratedPracticeQuestion & {
  answer?: string
  analysis?: string
  options?: ChoiceOption[]
}

type RetryMistakeTarget = {
  questionId: number
  questionTitle: string
}

const filters = reactive({
  knowledgePoint: '',
  difficulty: '',
  type: '',
  count: 10
})
const knowledgePoints = ref<KnowledgePoint[]>([])
const questions = ref<PracticeQuestion[]>([])
const currentIndex = ref(0)
const retryTarget = ref<RetryMistakeTarget | null>(loadRetryTarget())
const selectedAnswer = ref<string | string[]>('')
const submitted = ref(false)
const finished = ref(false)
const loading = ref(false)
const statusMessage = ref('')
const feedbackStatus = ref('')
const feedbackContent = ref('标准答案或解析可能有误，请管理员复核。')
const backendResult = ref<PracticeResult | null>(null)
const completedCount = ref(0)
const autoGradedCount = ref(0)
const correctCount = ref(0)

const currentQuestion = computed(() => questions.value[currentIndex.value] ?? null)
const firstItem = computed(() => backendResult.value?.items[0])
const isAutoGraded = computed(() => firstItem.value?.autoGraded ?? false)
const isCorrect = computed(() => firstItem.value?.correct === true)
const correctAnswer = computed(() => firstItem.value?.correctAnswer ?? currentQuestion.value?.answer ?? '')
const analysisText = computed(() => firstItem.value?.analysis ?? currentQuestion.value?.analysis ?? '暂无解析。')
const displaySelectedAnswer = computed(() => (
  Array.isArray(selectedAnswer.value) ? selectedAnswer.value.join('、') : selectedAnswer.value
))
const scoreText = computed(() => {
  if (!isAutoGraded.value) {
    return '不评分'
  }
  return backendResult.value ? `${backendResult.value.score}/${backendResult.value.totalScore}` : '--/--'
})
const progressWidth = computed(() => (isAutoGraded.value && isCorrect.value ? '100%' : '0%'))
const resultText = computed(() => {
  if (!isAutoGraded.value) {
    return '本题不自动评分，请结合参考答案自行核对。'
  }
  return isCorrect.value ? '回答正确，继续保持。' : '回答错误，已加入错题整理候选。'
})

onMounted(async () => {
  await loadKnowledgePointOptions()
  if (retryTarget.value) {
    await loadRetryPractice()
  } else {
    await generateFilteredPractice()
  }
})

async function loadKnowledgePointOptions() {
  try {
    knowledgePoints.value = (await listKnowledgePoints()).filter((point) => point.enabled)
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载知识点失败。'
  }
}

async function generateFilteredPractice() {
  loading.value = true
  statusMessage.value = ''
  retryTarget.value = null
  window.sessionStorage.removeItem('studyCollectionRetryMistake')
  filters.count = Math.min(100, Math.max(1, Number(filters.count) || 10))
  try {
    const payload: Parameters<typeof generatePractice>[0] = { count: filters.count }
    if (filters.knowledgePoint) payload.knowledgePoint = filters.knowledgePoint
    if (filters.difficulty) payload.difficulty = filters.difficulty
    if (filters.type) payload.type = filters.type
    const generated = await generatePractice(payload)
    questions.value = generated.questions.map(normalizePracticeQuestion)
    resetPracticeProgress()
    if (generated.actualCount < generated.requestedCount) {
      statusMessage.value = `符合条件的题目共 ${generated.actualCount} 道，已全部加入本次练习。`
    }
  } catch (error) {
    questions.value = []
    resetPracticeProgress()
    statusMessage.value = error instanceof Error ? error.message : '生成练习失败，请检查本地后端是否启动。'
  } finally {
    loading.value = false
  }
}

async function loadRetryPractice() {
  loading.value = true
  statusMessage.value = ''
  try {
    const target = (await searchQuestions()).find((question) => question.id === retryTarget.value?.questionId)
    if (!target) {
      retryTarget.value = null
      window.sessionStorage.removeItem('studyCollectionRetryMistake')
      statusMessage.value = '未找到这道错题，已切换为普通练习。'
      await generateFilteredPractice()
      return
    }
    questions.value = [normalizePracticeQuestion(target)]
    resetPracticeProgress()
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载错题失败，请检查本地后端是否启动。'
  } finally {
    loading.value = false
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

function normalizePracticeQuestion(question: PracticeQuestionSource): PracticeQuestion {
  const parsed = parseChoiceOptions(question.title)
  const normalized: PracticeQuestion = { ...question, title: parsed.title }
  if (parsed.options.length >= 2) {
    normalized.options = parsed.options
    return normalized
  }
  if (question.type === 'TRUE_FALSE') {
    normalized.options = [
      { value: 'true', label: '正确' },
      { value: 'false', label: '错误' }
    ]
    return normalized
  }
  if (question.type === 'SINGLE_CHOICE' || question.type === 'MULTIPLE_CHOICE') {
    normalized.options = ['A', 'B', 'C', 'D'].map((value) => ({
      value,
      label: `选项 ${value}（原题未提供选项内容）`
    }))
  }
  return normalized
}

function parseChoiceOptions(title: string) {
  const lines = title.split(/\r?\n/).map((line) => line.trim()).filter(Boolean)
  const options: ChoiceOption[] = []
  const stemLines: string[] = []
  lines.forEach((line) => {
    const match = line.match(/^([A-D])[\.、．]\s*(.+)$/i)
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
    completedCount.value += 1
    const item = backendResult.value.items[0]
    if (item?.autoGraded) {
      autoGradedCount.value += 1
      if (item.correct) {
        correctCount.value += 1
      }
    }
    if (item?.autoGraded && item.correct === false) {
      try {
        await recordMistake({
          questionId: currentQuestion.value.id,
          questionTitle: currentQuestion.value.title,
          knowledgePoint: currentQuestion.value.knowledgePoint,
          status: 'PENDING'
        })
      } catch {
        statusMessage.value = '答案已提交，但同步错题本失败，请稍后重试。'
      }
    }
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '提交失败，请检查本地后端是否启动。'
  }
}

function nextQuestion() {
  if (currentIndex.value >= questions.value.length - 1) {
    return
  }
  currentIndex.value += 1
  resetCurrentAnswer()
}

function finishPractice() {
  if (submitted.value) {
    finished.value = true
    retryTarget.value = null
    window.sessionStorage.removeItem('studyCollectionRetryMistake')
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

function resetPracticeProgress() {
  currentIndex.value = 0
  completedCount.value = 0
  autoGradedCount.value = 0
  correctCount.value = 0
  finished.value = false
  resetCurrentAnswer()
}

function resetCurrentAnswer() {
  selectedAnswer.value = currentQuestion.value?.type === 'MULTIPLE_CHOICE' ? [] : ''
  submitted.value = false
  backendResult.value = null
  feedbackStatus.value = ''
  feedbackContent.value = '标准答案或解析可能有误，请管理员复核。'
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
