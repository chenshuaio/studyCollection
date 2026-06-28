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
        <RouterLink v-if="isAdminUser" to="/feedback">反馈审核</RouterLink>
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

      <section id="overview" class="metric-grid" aria-label="学习概览">
        <article>
          <span>已做题目</span>
          <strong>{{ dashboardMetrics.answeredQuestionCount }} 题</strong>
          <small>正确率 {{ accuracyText }}</small>
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

      <section class="workspace-grid">
        <article id="import" class="workspace-panel">
          <h2>题库导入</h2>
          <p>支持 JSON、CSV、XLSX、TXT、MD、PDF、DOCX 文件预览后入库。</p>
          <RouterLink class="button-link" to="/import">导入题目</RouterLink>
        </article>
        <article id="exam" class="workspace-panel">
          <h2>自定义组卷</h2>
          <p>按知识点、难度、题型筛选题目，也可以手动组合生成考试卷。</p>
          <RouterLink class="button-link" to="/exams">创建考试卷</RouterLink>
        </article>
        <article id="feedback" class="workspace-panel">
          <h2>错题反馈</h2>
          <p>发现答案或解析错误时提交反馈，管理员审核后修订题库。</p>
          <div class="action-row">
            <RouterLink class="button-link" to="/mistakes">查看错题</RouterLink>
            <RouterLink v-if="isAdminUser" class="button-link" to="/feedback">反馈审核</RouterLink>
          </div>
        </article>
        <article id="report" class="workspace-panel">
          <h2>AI 分析</h2>
          <p>在线模型开启时生成学习建议；关闭时使用规则分析薄弱点。</p>
          <RouterLink class="button-link" to="/reports">生成报告</RouterLink>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { RouterLink } from 'vue-router'
import { getPracticeStats, listMistakes, listUserFeedback } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'
import { getCurrentUser } from '../session'

const isAdminUser = isAdmin()
const currentUser = getCurrentUser()
const dashboardMetrics = reactive({
  answeredQuestionCount: 0,
  correctQuestionCount: 0,
  mistakeCount: 0,
  feedbackCount: 0,
  weakestKnowledgePoint: ''
})

const accuracyText = computed(() => {
  if (dashboardMetrics.answeredQuestionCount === 0) {
    return '0%'
  }
  return `${Math.round((dashboardMetrics.correctQuestionCount / dashboardMetrics.answeredQuestionCount) * 100)}%`
})
const weakestKnowledgeText = computed(() => (
  dashboardMetrics.weakestKnowledgePoint ? `优先复习${dashboardMetrics.weakestKnowledgePoint}` : '暂无待复盘知识点'
))
const feedbackText = computed(() => (
  dashboardMetrics.feedbackCount > 0 ? '等待管理员审核或处理' : '暂无题目反馈'
))

onMounted(loadDashboardMetrics)

async function loadDashboardMetrics() {
  const userId = currentUser?.userId ?? 7
  const [practiceStats, mistakes, feedback] = await Promise.all([
    getPracticeStats(userId),
    listMistakes(userId),
    listUserFeedback(userId)
  ])
  dashboardMetrics.answeredQuestionCount = practiceStats.answeredQuestionCount
  dashboardMetrics.correctQuestionCount = practiceStats.correctQuestionCount
  dashboardMetrics.mistakeCount = mistakes.length
  dashboardMetrics.feedbackCount = feedback.length
  dashboardMetrics.weakestKnowledgePoint = mostFrequentKnowledgePoint(mistakes.map((mistake) => mistake.knowledgePoint))
}

function mostFrequentKnowledgePoint(points: string[]) {
  const counts = new Map<string, number>()
  points.forEach((point) => counts.set(point, (counts.get(point) ?? 0) + 1))
  return [...counts.entries()].sort((left, right) => right[1] - left[1])[0]?.[0] ?? ''
}
</script>
