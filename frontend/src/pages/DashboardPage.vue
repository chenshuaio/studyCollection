<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink v-if="isAdminUser" to="/questions">题库管理</RouterLink>
        <RouterLink v-if="isAdminUser" to="/knowledge-points">知识点管理</RouterLink>
        <RouterLink v-if="isAdminUser" to="/feedback">反馈审核</RouterLink>
        <RouterLink v-if="isAdminUser" to="/users">用户管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">本地学习工作台</p>
          <h1>学习控制台</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">新建练习</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <p v-if="statusMessage" class="form-message dashboard-message" aria-live="polite">
        {{ statusMessage }}
      </p>

      <section id="overview" class="metric-grid" aria-label="学习概览">
        <article>
          <span>已做题目</span>
          <strong>{{ dashboardMetrics.answeredQuestionCount }} 题</strong>
          <small>客观题正确率 {{ accuracyText }}</small>
        </article>
        <article>
          <span>待处理错题</span>
          <strong>{{ dashboardMetrics.mistakeCount }} 题</strong>
          <small>{{ weakestKnowledgeText }}</small>
        </article>
        <article>
          <span>我的反馈</span>
          <strong>{{ dashboardMetrics.feedbackCount }} 条</strong>
          <small>{{ feedbackText }}</small>
        </article>
      </section>

      <section class="dashboard-activity-grid">
        <article class="workspace-panel">
          <div class="panel-header">
            <h2>最近练习</h2>
            <RouterLink class="table-action" to="/practice">开始练习</RouterLink>
          </div>
          <ol v-if="recentPractices.length" class="activity-list">
            <li v-for="practice in recentPractices" :key="practice.referenceId">
              <div class="activity-row">
                <div>
                  <strong>{{ knowledgeLabel(practice.knowledgePoints) }} · {{ practice.answeredQuestionCount }} 题</strong>
                  <time :datetime="practice.attemptedAt">{{ formatDateTime(practice.attemptedAt) }}</time>
                </div>
                <span class="activity-score">
                  {{ practice.gradedQuestionCount ? percent(practice.accuracy) : '主观核对' }}
                </span>
              </div>
            </li>
          </ol>
          <p v-else class="empty-state-copy">暂无练习记录。</p>
        </article>

        <article class="workspace-panel">
          <div class="panel-header">
            <h2>最近考试</h2>
            <RouterLink class="table-action" to="/exams">全部考试</RouterLink>
          </div>
          <ol v-if="recentExams.length" class="activity-list">
            <li v-for="exam in recentExams" :key="exam.id">
              <RouterLink class="activity-row" :to="`/exams/${exam.id}/take`">
                <div>
                  <strong>{{ exam.name }}</strong>
                  <time :datetime="exam.startedAt">{{ formatDateTime(exam.startedAt) }}</time>
                </div>
                <span class="activity-score">{{ examScore(exam) }}</span>
              </RouterLink>
            </li>
          </ol>
          <p v-else class="empty-state-copy">暂无考试记录。</p>
        </article>
      </section>

      <section class="dashboard-insight-grid">
        <article class="workspace-panel">
          <div class="panel-header">
            <h2>学习趋势</h2>
            <RouterLink class="table-action" to="/reports">完整报告</RouterLink>
          </div>
          <ol v-if="latestTrend.length" class="dashboard-trend-list">
            <li v-for="point in latestTrend" :key="point.date">
              <time :datetime="point.date">{{ point.date }}</time>
              <div class="progress-track" :aria-label="`${point.date}正确率`">
                <span :style="{ width: percent(point.accuracy) }"></span>
              </div>
              <strong>{{ percent(point.accuracy) }}</strong>
              <small>{{ point.correctQuestionCount }}/{{ point.gradedQuestionCount }} 题</small>
            </li>
          </ol>
          <p v-else class="empty-state-copy">生成学习报告后展示近期趋势。</p>
        </article>

        <article class="workspace-panel dashboard-focus-panel">
          <p class="eyebrow">当前强化方向</p>
          <template v-if="latestReport">
            <h2>优先强化 {{ latestReport.weakestKnowledgePoint }}</h2>
            <p>{{ latestReport.recommendation }}</p>
            <RouterLink
              class="button-link"
              data-action="dashboard-strengthen"
              :to="{ path: '/practice', query: { knowledgePoint: latestReport.weakestKnowledgePoint } }"
            >
              开始定向练习
            </RouterLink>
          </template>
          <template v-else>
            <h2>尚无薄弱点结论</h2>
            <RouterLink class="button-link" to="/reports">生成学习报告</RouterLink>
          </template>
        </article>
      </section>

      <section class="dashboard-quick-bar" aria-label="快捷入口">
        <h2>快捷入口</h2>
        <nav>
          <RouterLink to="/import">题库导入</RouterLink>
          <RouterLink to="/exams">自定义组卷</RouterLink>
          <RouterLink to="/mistakes">错题整理</RouterLink>
          <RouterLink to="/reports">学习报告</RouterLink>
        </nav>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  getPracticeStats,
  getRecentPractices,
  listExamSessions,
  listLearningReports,
  listMistakes,
  listUserFeedback,
  type ExamSummary,
  type LearningReport,
  type RecentPracticeSummary
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()
const dashboardMetrics = reactive({
  answeredQuestionCount: 0,
  gradedQuestionCount: 0,
  correctQuestionCount: 0,
  mistakeCount: 0,
  feedbackCount: 0,
  fallbackWeakestKnowledgePoint: ''
})
const recentPractices = ref<RecentPracticeSummary[]>([])
const recentExams = ref<ExamSummary[]>([])
const latestReport = ref<LearningReport | null>(null)
const statusMessage = ref('')

const latestTrend = computed(() => latestReport.value?.recentTrend ?? [])
const accuracyText = computed(() => {
  if (dashboardMetrics.gradedQuestionCount === 0) {
    return '0%'
  }
  return percent(dashboardMetrics.correctQuestionCount / dashboardMetrics.gradedQuestionCount)
})
const weakestKnowledgePoint = computed(() => (
  latestReport.value?.weakestKnowledgePoint ?? dashboardMetrics.fallbackWeakestKnowledgePoint
))
const weakestKnowledgeText = computed(() => (
  weakestKnowledgePoint.value ? `优先复习 ${weakestKnowledgePoint.value}` : '暂无待复盘知识点'
))
const feedbackText = computed(() => (
  dashboardMetrics.feedbackCount > 0 ? '等待管理员审核或处理' : '暂无题目反馈'
))

onMounted(loadDashboard)

async function loadDashboard() {
  const results = await Promise.allSettled([
    getPracticeStats(),
    listMistakes(),
    listUserFeedback(),
    getRecentPractices(3),
    listExamSessions(),
    listLearningReports()
  ] as const)

  const [statsResult, mistakesResult, feedbackResult, practicesResult, examsResult, reportsResult] = results
  if (statsResult.status === 'fulfilled') {
    dashboardMetrics.answeredQuestionCount = statsResult.value.answeredQuestionCount
    dashboardMetrics.gradedQuestionCount = statsResult.value.gradedQuestionCount
    dashboardMetrics.correctQuestionCount = statsResult.value.correctQuestionCount
  }
  if (mistakesResult.status === 'fulfilled') {
    dashboardMetrics.mistakeCount = mistakesResult.value.length
    dashboardMetrics.fallbackWeakestKnowledgePoint = mostFrequentKnowledgePoint(
      mistakesResult.value.map((mistake) => mistake.knowledgePoint)
    )
  }
  if (feedbackResult.status === 'fulfilled') {
    dashboardMetrics.feedbackCount = feedbackResult.value.length
  }
  if (practicesResult.status === 'fulfilled') {
    recentPractices.value = practicesResult.value
  }
  if (examsResult.status === 'fulfilled') {
    recentExams.value = examsResult.value.slice(0, 3)
  }
  if (reportsResult.status === 'fulfilled') {
    latestReport.value = reportsResult.value[0] ?? null
  }

  const failureCount = results.filter((result) => result.status === 'rejected').length
  statusMessage.value = failureCount ? `有 ${failureCount} 项学习数据暂时加载失败，请稍后刷新。` : ''
}

function mostFrequentKnowledgePoint(points: string[]) {
  const counts = new Map<string, number>()
  points.forEach((point) => counts.set(point, (counts.get(point) ?? 0) + 1))
  return [...counts.entries()].sort((left, right) => right[1] - left[1])[0]?.[0] ?? ''
}

function knowledgeLabel(points: string[]) {
  return points.length ? points.join('、') : '综合练习'
}

function percent(value: number) {
  return `${Math.round(Math.max(0, Math.min(1, value)) * 100)}%`
}

function examScore(exam: ExamSummary) {
  if (exam.score !== null && exam.totalScore !== null) {
    return `${exam.score} / ${exam.totalScore}`
  }
  return exam.status === 'IN_PROGRESS' ? '进行中' : '--'
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>
