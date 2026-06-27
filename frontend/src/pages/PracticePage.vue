<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">自适应刷题</p>
          <h1>练习中心</h1>
        </div>
        <button type="button" @click="resetPractice">生成练习</button>
      </header>

      <section class="practice-layout">
        <article class="workspace-panel practice-question">
          <div class="question-meta">
            <span>单选题</span>
            <span>集合框架</span>
            <span>中等</span>
          </div>
          <h2>HashMap 默认负载因子是多少？</h2>
          <form class="option-list" aria-label="练习选项">
            <label v-for="option in currentQuestion.options" :key="option.value">
              <input v-model="selectedAnswer" type="radio" name="practice-answer" :value="option.value" />
              <span>{{ option.value }}. {{ option.label }}</span>
            </label>
          </form>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
          <button type="button" @click="submitAnswer">提交答案</button>
        </article>

        <aside class="workspace-panel score-panel">
          <h2>得分</h2>
          <strong>{{ submitted ? scoreText : '--/10' }}</strong>
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
              <dt>标准答案</dt>
              <dd>{{ submitted ? correctAnswer : '提交后可见' }}</dd>
            </div>
            <div>
              <dt>错题反馈</dt>
              <dd>{{ submitted && !isCorrect ? '可提交给管理员复核题目或解析。' : '暂无反馈' }}</dd>
            </div>
          </dl>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { submitPractice, type PracticeResult } from '../api'

const currentQuestion = {
  id: 1,
  answer: 'A',
  analysis: 'HashMap 默认负载因子是 0.75，达到阈值后会触发扩容。',
  options: [
    { value: 'A', label: '0.75' },
    { value: 'B', label: '0.5' },
    { value: 'C', label: '1.0' },
    { value: 'D', label: '2.0' }
  ]
}

const selectedAnswer = ref('A')
const submitted = ref(false)
const statusMessage = ref('')
const backendResult = ref<PracticeResult | null>(null)

const firstItem = computed(() => backendResult.value?.items[0])
const isCorrect = computed(() => firstItem.value?.correct ?? selectedAnswer.value === currentQuestion.answer)
const correctAnswer = computed(() => firstItem.value?.correctAnswer ?? currentQuestion.answer)
const analysisText = computed(() => firstItem.value?.analysis ?? currentQuestion.analysis)
const scoreText = computed(() => {
  if (!backendResult.value) {
    return isCorrect.value ? '10/10' : '0/10'
  }
  return `${backendResult.value.score}/${backendResult.value.totalScore}`
})
const progressWidth = computed(() => (isCorrect.value ? '100%' : '0%'))
const resultText = computed(() => (isCorrect.value ? '回答正确，继续保持。' : '回答错误，已加入错题整理候选。'))

async function submitAnswer() {
  statusMessage.value = ''
  try {
    backendResult.value = await submitPractice([{ questionId: currentQuestion.id, answer: selectedAnswer.value }])
    submitted.value = true
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '提交失败，请检查本地后端是否启动。'
  }
}

function resetPractice() {
  selectedAnswer.value = 'A'
  submitted.value = false
  backendResult.value = null
  statusMessage.value = ''
}
</script>
