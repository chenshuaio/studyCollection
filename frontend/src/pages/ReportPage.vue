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
          <p class="eyebrow">真实作答分析</p>
          <h1>学习报告</h1>
        </div>
        <div class="header-actions">
          <RouterLink
            v-if="report"
            class="button-link"
            data-action="strengthen"
            :to="{ path: '/practice', query: { knowledgePoint: report.weakestKnowledgePoint } }"
          >
            薄弱点强化
          </RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="metric-grid" aria-label="学习报告概览">
        <article>
          <span>已作答</span>
          <strong>{{ report?.answeredQuestionCount ?? 0 }}</strong>
          <small>包含客观题和主观题</small>
        </article>
        <article>
          <span>客观题</span>
          <strong>{{ report?.gradedQuestionCount ?? 0 }}</strong>
          <small>答对 {{ report?.correctQuestionCount ?? 0 }} 题</small>
        </article>
        <article>
          <span>正确率</span>
          <strong>{{ percent(report?.accuracy ?? 0) }}</strong>
          <small>主观题不计入分母</small>
        </article>
      </section>

      <section class="question-layout report-top-layout">
        <article class="workspace-panel">
          <h2>生成新报告</h2>
          <form class="question-form" @submit.prevent="createReport">
            <label>
              分析模式
              <select v-model="mode">
                <option value="OFFLINE_RULES">规则分析</option>
                <option value="ONLINE_MODEL">在线模型</option>
              </select>
            </label>
            <label>
              修订题处理
              <select v-model="revisedQuestionPolicy" aria-label="修订题处理策略">
                <option value="EXCLUDE_REVISED">排除已确认答案错误的旧作答</option>
                <option value="RECALCULATE_REVISED">按当前标准答案重新计算</option>
              </select>
            </label>
            <p v-if="statusMessage" class="form-message" aria-live="polite">{{ statusMessage }}</p>
            <button type="submit" :disabled="generating">
              {{ generating ? '生成中...' : '生成报告' }}
            </button>
          </form>
        </article>

        <article class="workspace-panel report-card">
          <h2>当前结论</h2>
          <template v-if="report">
            <dl>
              <div>
                <dt>最薄弱知识点</dt>
                <dd>{{ report.weakestKnowledgePoint }}</dd>
              </div>
              <div>
                <dt>分析来源</dt>
                <dd>{{ sourceLabel(report.adviceSource) }}</dd>
              </div>
            </dl>
            <p class="report-recommendation">{{ report.recommendation }}</p>
            <p>{{ report.adviceContent }}</p>
            <p v-if="report.revisedAttemptCount" class="revision-policy-note">
              {{ revisionPolicyText(report) }}
            </p>
          </template>
          <p v-else class="empty-state-copy">完成练习或考试后即可生成第一份报告。</p>
        </article>
      </section>

      <section v-if="report" class="report-section-grid">
        <article class="workspace-panel">
          <div class="panel-header">
            <h2>知识点表现</h2>
            <span class="panel-count">{{ report.knowledgePointPerformance.length }} 项</span>
          </div>
          <ul class="performance-list">
            <li v-for="item in report.knowledgePointPerformance" :key="item.label">
              <div class="performance-heading">
                <strong>{{ item.label }}</strong>
                <span>{{ item.correctQuestionCount }}/{{ item.gradedQuestionCount }} · {{ percent(item.accuracy) }}</span>
              </div>
              <div class="progress-track" :aria-label="`${item.label}正确率`">
                <span :style="{ width: percent(item.accuracy) }"></span>
              </div>
              <small>共作答 {{ item.answeredQuestionCount }} 题</small>
            </li>
          </ul>
        </article>

        <article class="workspace-panel">
          <div class="panel-header">
            <h2>题型表现</h2>
            <span class="panel-count">{{ report.questionTypePerformance.length }} 类</span>
          </div>
          <ul class="performance-list">
            <li v-for="item in report.questionTypePerformance" :key="item.label">
              <div class="performance-heading">
                <strong>{{ questionTypeLabel(item.label) }}</strong>
                <span>{{ item.gradedQuestionCount ? percent(item.accuracy) : '主观核对' }}</span>
              </div>
              <div class="progress-track" :aria-label="`${questionTypeLabel(item.label)}正确率`">
                <span :style="{ width: percent(item.accuracy) }"></span>
              </div>
              <small>共作答 {{ item.answeredQuestionCount }} 题</small>
            </li>
          </ul>
        </article>

        <article class="workspace-panel">
          <div class="panel-header">
            <h2>近期趋势</h2>
            <span class="panel-count">近 {{ report.recentTrend.length }} 个学习日</span>
          </div>
          <ul v-if="report.recentTrend.length" class="trend-list">
            <li v-for="point in report.recentTrend" :key="point.date">
              <time :datetime="point.date">{{ point.date }}</time>
              <div class="trend-value">
                <strong>{{ percent(point.accuracy) }}</strong>
                <span>{{ point.correctQuestionCount }}/{{ point.gradedQuestionCount }} 题</span>
              </div>
            </li>
          </ul>
          <p v-else>暂无可展示的学习趋势。</p>
        </article>

        <article class="workspace-panel">
          <div class="panel-header">
            <h2>强化题推荐</h2>
            <span class="panel-count">{{ report.weakestKnowledgePoint }}</span>
          </div>
          <ol v-if="report.strengtheningQuestions.length" class="strengthening-list">
            <li v-for="question in report.strengtheningQuestions" :key="question.id">
              <strong>{{ question.title }}</strong>
              <span>{{ questionTypeLabel(question.type) }} · {{ difficultyLabel(question.difficulty) }}</span>
            </li>
          </ol>
          <p v-else>该知识点暂时没有可用强化题。</p>
        </article>
      </section>

      <section class="workspace-panel report-history-panel">
        <div class="panel-header">
          <h2>历史报告</h2>
          <span class="panel-count">{{ history.length }} 份</span>
        </div>
        <ol v-if="history.length" class="report-history-list">
          <li v-for="item in history" :key="item.id">
            <button type="button" data-report-history @click="report = item">
              <span>
                <strong>{{ item.weakestKnowledgePoint }}</strong>
                <small>{{ formatDateTime(item.createdAt) }}</small>
              </span>
              <span class="history-accuracy">{{ percent(item.accuracy) }}</span>
            </button>
          </li>
        </ol>
        <p v-else>尚未生成学习报告。</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  generateLearningReport,
  listLearningReports,
  type LearningReport,
  type LearningReportPayload
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()
const mode = ref<LearningReportPayload['mode']>('OFFLINE_RULES')
const revisedQuestionPolicy = ref<LearningReportPayload['revisedQuestionPolicy']>('EXCLUDE_REVISED')
const statusMessage = ref('')
const report = ref<LearningReport | null>(null)
const history = ref<LearningReport[]>([])
const generating = ref(false)

const questionTypeLabels: Record<string, string> = {
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

onMounted(loadHistory)

async function loadHistory() {
  statusMessage.value = ''
  try {
    history.value = await listLearningReports()
    report.value = history.value[0] ?? null
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载报告历史失败，请检查本地后端是否启动。'
  }
}

async function createReport() {
  generating.value = true
  statusMessage.value = ''
  try {
    const generated = await generateLearningReport({
      mode: mode.value,
      revisedQuestionPolicy: revisedQuestionPolicy.value
    })
    report.value = generated
    history.value = [generated, ...history.value.filter((item) => item.id !== generated.id)]
    statusMessage.value = '报告已生成。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '生成报告失败，请检查本地后端是否启动。'
  } finally {
    generating.value = false
  }
}

function percent(value: number) {
  return `${Math.round(Math.max(0, Math.min(1, value)) * 100)}%`
}

function sourceLabel(source: string) {
  return source === 'ONLINE_MODEL' ? '在线模型' : '规则分析'
}

function questionTypeLabel(type: string) {
  return questionTypeLabels[type] ?? type
}

function difficultyLabel(difficulty: string) {
  return difficultyLabels[difficulty] ?? difficulty
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function revisionPolicyText(value: LearningReport) {
  if (value.revisionPolicy === 'RECALCULATE_REVISED') {
    return `已按当前标准答案重算 ${value.revisedAttemptCount} 条受题目修订影响的历史作答。`
  }
  if (value.revisionPolicy === 'EXCLUDE_REVISED') {
    return `已排除 ${value.revisedAttemptCount} 条受题目修订影响的历史作答。`
  }
  return `本报告包含 ${value.revisedAttemptCount} 条后来发生题目修订的历史作答。`
}
</script>

<style scoped>
.revision-policy-note {
  padding-left: 10px;
  border-left: 3px solid #f79009;
  color: #7a2e0e;
}
</style>
