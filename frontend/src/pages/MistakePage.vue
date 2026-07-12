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
        <RouterLink v-if="isAdminUser" to="/exam-rules/manage">考试规则</RouterLink>
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
          题型
          <select v-model="filters.questionType" aria-label="错题题型筛选">
            <option value="">全部</option>
            <option v-for="type in questionTypes" :key="type" :value="type">
              {{ questionTypeText(type) }}
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
        <label>
          最近错误开始日期
          <input v-model="filters.wrongFrom" type="date" aria-label="最近错误开始日期" />
        </label>
        <label>
          最近错误结束日期
          <input v-model="filters.wrongTo" type="date" aria-label="最近错误结束日期" />
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
                <th>题目与分类</th>
                <th>错误记录</th>
                <th>掌握状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="mistake in filteredMistakes" :key="mistake.questionId">
                <td>
                  <div class="mistake-question-summary">
                    <strong>{{ mistake.questionTitle }}</strong>
                    <small>{{ questionTypeText(mistake.questionType) }} · {{ mistake.knowledgePoint }}</small>
                  </div>
                </td>
                <td>
                  <div class="mistake-history">
                    <strong>答错 {{ mistake.wrongCount }} 次</strong>
                    <small>首次 {{ formatDate(mistake.firstWrongAt) }}</small>
                    <small>最近 {{ formatDate(mistake.lastWrongAt) }}</small>
                  </div>
                </td>
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
                    <button
                      type="button"
                      :aria-label="`反馈 ${mistake.questionTitle}`"
                      @click="prepareFeedback(mistake)"
                    >
                      反馈题目
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
          <template v-if="feedbackTarget">
            <h2>反馈题目问题</h2>
            <strong>{{ feedbackTarget.questionTitle }}</strong>
            <p>最近一次答案：{{ feedbackTarget.lastSubmittedAnswer || '未记录' }}</p>
            <label>
              问题类型
              <select v-model="feedbackType" aria-label="错题反馈类型">
                <option value="ANSWER_ERROR">答案错误</option>
                <option value="EXPLANATION_ERROR">解析错误</option>
                <option value="STEM_ERROR">题干错误</option>
                <option value="OPTION_ERROR">选项错误</option>
                <option value="KNOWLEDGE_POINT_ERROR">知识点错误</option>
                <option value="DIFFICULTY_ERROR">难度错误</option>
                <option value="OTHER">其他问题</option>
              </select>
            </label>
            <label>
              具体问题
              <textarea v-model="feedbackContent" aria-label="错题反馈内容"></textarea>
            </label>
            <div class="action-row">
              <button type="button" data-action="submit-mistake-feedback" @click="submitMistakeFeedback">
                提交反馈
              </button>
              <button type="button" @click="feedbackTarget = null">取消</button>
            </div>
          </template>
          <template v-else>
            <h2>强化建议</h2>
            <p>优先重练同一知识点错题，连续答对后再标记为已掌握。</p>
          </template>
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
import { listMistakes, submitQuestionFeedback, updateMistakeStatus, type MistakeRecord } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()

const filters = reactive({
  knowledgePoint: '',
  questionType: '',
  status: '',
  wrongFrom: '',
  wrongTo: ''
})
const mistakes = ref<MistakeRecord[]>([])
const statusMessage = ref('')
const feedbackTarget = ref<MistakeRecord | null>(null)
const feedbackType = ref('ANSWER_ERROR')
const feedbackContent = ref('标准答案或解析可能有误，请管理员复核。')

const knowledgePoints = computed(() => {
  return Array.from(new Set(mistakes.value.map((mistake) => mistake.knowledgePoint))).sort()
})

const questionTypes = computed(() => {
  return Array.from(new Set(mistakes.value.map((mistake) => mistake.questionType))).sort()
})

const filteredMistakes = computed(() => {
  return mistakes.value
    .filter((mistake) => !filters.knowledgePoint || mistake.knowledgePoint === filters.knowledgePoint)
    .filter((mistake) => !filters.questionType || mistake.questionType === filters.questionType)
    .filter((mistake) => !filters.status || mistake.status === filters.status)
    .filter((mistake) => !filters.wrongFrom || isoDate(mistake.lastWrongAt) >= filters.wrongFrom)
    .filter((mistake) => !filters.wrongTo || isoDate(mistake.lastWrongAt) <= filters.wrongTo)
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
    mistakes.value = await listMistakes()
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

function questionTypeText(type: string) {
  const labels: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
    PROGRAMMING: '编程题'
  }
  return labels[type] ?? type
}

function isoDate(value: string) {
  return value.slice(0, 10)
}

function formatDate(value: string) {
  return isoDate(value)
}

function statusActionLabel(status: string) {
  return status === 'MASTERED' ? '重新标记待巩固' : '标记已掌握'
}

async function toggleMistakeStatus(mistake: MistakeRecord) {
  statusMessage.value = ''
  const nextStatus = mistake.status === 'MASTERED' ? 'PENDING' : 'MASTERED'
  try {
    await updateMistakeStatus({
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

function prepareFeedback(mistake: MistakeRecord) {
  feedbackTarget.value = mistake
  feedbackType.value = 'ANSWER_ERROR'
  statusMessage.value = ''
}

async function submitMistakeFeedback() {
  if (!feedbackTarget.value) return
  statusMessage.value = ''
  try {
    await submitQuestionFeedback({
      questionId: feedbackTarget.value.questionId,
      type: feedbackType.value,
      content: feedbackContent.value,
      submittedAnswer: feedbackTarget.value.lastSubmittedAnswer,
      sourceContext: 'MISTAKE_BOOK',
      sourceReference: `mistake-${feedbackTarget.value.questionId}`
    })
    statusMessage.value = '反馈已提交，管理员可在反馈审核页查看。'
    feedbackTarget.value = null
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '反馈提交失败。'
  }
}
</script>

<style scoped>
.mistake-history {
  display: grid;
  gap: 2px;
  min-width: 118px;
}

.mistake-question-summary {
  display: grid;
  gap: 4px;
  min-width: 190px;
}

.mistake-question-summary small {
  color: var(--muted-text, #667085);
}

.mistake-history small {
  color: var(--muted-text, #667085);
}

.table-panel th:nth-child(3),
.table-panel td:nth-child(3) {
  min-width: 76px;
  white-space: nowrap;
}

.table-panel th:last-child,
.table-panel td:last-child {
  min-width: 112px;
}

.workspace-panel label {
  display: grid;
  gap: 6px;
  margin-top: 14px;
}

.workspace-panel textarea {
  min-height: 110px;
  resize: vertical;
}
</style>
