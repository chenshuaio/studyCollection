<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">薄弱点分析</p>
          <h1>学习报告</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">强化练习</RouterLink>
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="workspace-panel">
          <h2>分析设置</h2>
          <form class="question-form" @submit.prevent="createReport">
            <label>
              分析模式
              <select v-model="mode">
                <option value="OFFLINE_RULES">规则分析</option>
                <option value="ONLINE_MODEL">在线模型</option>
              </select>
            </label>
            <p>系统会根据知识点正确率、错题频率和练习表现生成薄弱点建议。</p>
            <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
            <button type="submit">生成报告</button>
          </form>
        </article>

        <article class="workspace-panel report-card">
          <h2>报告结果</h2>
          <template v-if="report">
            <dl>
              <div>
                <dt>最薄弱知识点</dt>
                <dd>{{ report.weakestKnowledgePoint }}</dd>
              </div>
              <div>
                <dt>分析来源</dt>
                <dd>{{ report.adviceSource }}</dd>
              </div>
            </dl>
            <p>{{ report.recommendation }}</p>
            <p>{{ report.adviceContent }}</p>
          </template>
          <p v-else>生成后展示正确率、薄弱点、学习建议和强化方向。</p>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { generateLearningReport, type LearningReport, type LearningReportPayload } from '../api'
import LogoutButton from '../components/LogoutButton.vue'

const mode = ref<LearningReportPayload['mode']>('OFFLINE_RULES')
const statusMessage = ref('')
const report = ref<LearningReport | null>(null)

const sampleResults = [
  { knowledgePoint: '集合框架', correct: true },
  { knowledgePoint: '集合框架', correct: false },
  { knowledgePoint: 'JVM', correct: false },
  { knowledgePoint: 'JVM', correct: false }
]

async function createReport() {
  statusMessage.value = ''
  try {
    report.value = await generateLearningReport({
      mode: mode.value,
      results: sampleResults
    })
    statusMessage.value = '报告已生成。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '生成报告失败，请检查本地后端是否启动。'
  }
}
</script>
