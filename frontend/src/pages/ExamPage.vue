<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">{{ isAdminUser ? '题库管理' : '我的题库' }}</RouterLink>
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
          <p class="eyebrow">个人考试卷</p>
          <h1>考试中心</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">练习模式</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="simulation-section" aria-label="管理员模拟考试">
        <div class="simulation-heading">
          <div>
            <p class="eyebrow">固定规则 · 随机组卷</p>
            <h2>管理员模拟考试</h2>
          </div>
          <RouterLink v-if="isAdminUser" class="table-action" to="/exam-rules/manage">管理考试规则</RouterLink>
        </div>
        <div v-if="publishedRules.length > 0" class="simulation-grid">
          <article v-for="rule in publishedRules" :key="rule.id" class="simulation-rule-card">
            <div class="simulation-card-header">
              <div>
                <h3>{{ rule.name }}</h3>
                <p>{{ rule.description || '按管理员配置随机生成模拟试卷。' }}</p>
              </div>
              <span>{{ rule.totalQuestions }} 题</span>
            </div>
            <dl>
              <div>
                <dt>考试时限</dt>
                <dd>{{ rule.durationMinutes }} 分钟</dd>
              </div>
              <div>
                <dt>知识点</dt>
                <dd>{{ rule.knowledgePoints.length ? rule.knowledgePoints.join('、') : '全部知识点' }}</dd>
              </div>
              <div>
                <dt>题型</dt>
                <dd>{{ quotaSummary(rule.typeQuotas, typeLabels) }}</dd>
              </div>
              <div>
                <dt>难度</dt>
                <dd>{{ quotaSummary(rule.difficultyQuotas, difficultyLabels) }}</dd>
              </div>
            </dl>
            <button
              class="simulation-start"
              type="button"
              :data-rule-id="rule.id"
              :disabled="startingRuleId !== null"
              @click="startRule(rule)"
            >
              {{ startingRuleId === rule.id ? '正在生成...' : '开始模拟考试' }}
            </button>
          </article>
        </div>
        <p v-else class="simulation-empty">当前没有已发布的模拟考试，可继续使用下方个人组卷。</p>
      </section>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <div class="candidate-heading">
              <h2>可选题目</h2>
              <label>
                题库范围
                <select v-model="questionScope" aria-label="组卷题库范围" @change="loadQuestions">
                  <option value="ALL">全部可用</option>
                  <option value="PUBLIC">公共题库</option>
                  <option value="PERSONAL">我的题库</option>
                </select>
              </label>
            </div>
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
import { RouterLink, useRouter } from 'vue-router'
import {
  composeCustomExam,
  listExamSessions,
  listPublishedExamRules,
  searchQuestions,
  startSimulationExam,
  type CustomExamPaper,
  type ExamRule,
  type ExamSummary,
  type Question,
  type QuestionBankScope
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()
const router = useRouter()
const availableQuestions = ref<Question[]>([])
const questionScope = ref<QuestionBankScope>('ALL')
const histories = ref<ExamSummary[]>([])
const publishedRules = ref<ExamRule[]>([])
const selectedQuestionIds = ref<number[]>([])
const statusMessage = ref('')
const createdPaper = ref<CustomExamPaper | null>(null)
const creating = ref(false)
const startingRuleId = ref<number | null>(null)
const draft = reactive({
  name: '集合专项测试',
  durationMinutes: 45
})

onMounted(async () => {
  await Promise.all([loadQuestions(), loadHistory(), loadPublishedRules()])
})

const typeLabels: Record<string, string> = {
  SINGLE_CHOICE: '单选题',
  MULTIPLE_CHOICE: '多选题',
  TRUE_FALSE: '判断题',
  FILL_BLANK: '填空题',
  SHORT_ANSWER: '简答题',
  PROGRAMMING: '编程题'
}

const difficultyLabels: Record<string, string> = {
  BEGINNER: '入门',
  INTERMEDIATE: '进阶',
  ADVANCED: '精通'
}

async function loadPublishedRules() {
  try {
    publishedRules.value = await listPublishedExamRules()
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载模拟考试失败，请检查本地后端是否启动。')
  }
}

async function startRule(rule: ExamRule) {
  statusMessage.value = ''
  startingRuleId.value = rule.id
  try {
    const session = await startSimulationExam(rule.id)
    await router.push({ name: 'exam-taking', params: { examId: session.id } })
  } catch (error) {
    statusMessage.value = errorMessage(error, '生成模拟考试失败，请联系管理员检查题库配额。')
  } finally {
    startingRuleId.value = null
  }
}

async function loadQuestions() {
  try {
    availableQuestions.value = await searchQuestions({ scope: questionScope.value })
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

function quotaSummary(quotas: Record<string, number>, labels: Record<string, string>) {
  return Object.entries(quotas)
    .filter(([, count]) => count > 0)
    .map(([key, count]) => `${labels[key] ?? key} ${count}`)
    .join('、') || '未配置'
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

<style scoped>
.simulation-section {
  margin-bottom: 16px;
}

.simulation-heading {
  align-items: flex-end;
  display: flex;
  gap: 16px;
  justify-content: space-between;
  margin-bottom: 12px;
}

.simulation-heading h2 {
  font-size: 22px;
  margin: 0;
}

.simulation-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.simulation-rule-card {
  background: #ffffff;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  display: grid;
  gap: 14px;
  min-width: 0;
  padding: 18px;
}

.simulation-card-header {
  align-items: flex-start;
  display: flex;
  gap: 12px;
  justify-content: space-between;
}

.simulation-card-header h3 {
  font-size: 18px;
  margin: 0;
}

.simulation-card-header p {
  color: #667085;
  line-height: 1.5;
  margin: 6px 0 0;
}

.simulation-card-header > span {
  background: #eef4ff;
  border-radius: 6px;
  color: #1f6feb;
  flex: 0 0 auto;
  font-size: 13px;
  font-weight: 800;
  padding: 6px 8px;
}

.simulation-rule-card dl {
  display: grid;
  gap: 10px 16px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  margin: 0;
}

.simulation-rule-card dt {
  color: #667085;
  font-size: 13px;
  margin-bottom: 3px;
}

.simulation-rule-card dd {
  color: #344054;
  line-height: 1.5;
  margin: 0;
  overflow-wrap: anywhere;
}

.simulation-start {
  background: #1f6feb;
  border: 0;
  border-radius: 7px;
  color: #ffffff;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
  min-height: 42px;
  padding: 0 16px;
  justify-self: start;
}

.simulation-start:disabled {
  cursor: wait;
  opacity: 0.65;
}

.simulation-empty {
  background: #ffffff;
  border: 1px solid #dfe5ee;
  border-radius: 8px;
  color: #667085;
  margin: 0;
  padding: 18px;
}

.candidate-heading {
  align-items: flex-end;
  display: flex;
  flex-wrap: wrap;
  gap: 12px 18px;
}

.candidate-heading h2 {
  margin: 0;
}

.candidate-heading label {
  color: #667085;
  display: grid;
  font-size: 13px;
  gap: 4px;
}

.candidate-heading select {
  background: #ffffff;
  border: 1px solid #cfd7e3;
  border-radius: 6px;
  min-height: 36px;
  padding: 0 34px 0 10px;
}

@media (max-width: 980px) {
  .simulation-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .simulation-heading,
  .simulation-card-header {
    align-items: stretch;
    flex-direction: column;
  }

  .simulation-rule-card dl {
    grid-template-columns: 1fr;
  }

  .simulation-start {
    justify-self: stretch;
    width: 100%;
  }
}
</style>
