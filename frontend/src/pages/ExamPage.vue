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
          <p class="eyebrow">个人考试卷</p>
          <h1>考试中心</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">练习模式</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>可选题目</h2>
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
                <td>{{ question.title }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.difficulty }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside class="workspace-panel">
          <h2>自定义组卷</h2>
          <form class="question-form" @submit.prevent="createPaper">
            <label>
              试卷名称
              <input v-model="draft.name" />
            </label>
            <label>
              时长（分钟）
              <input v-model.number="draft.durationMinutes" type="number" min="1" />
            </label>
            <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
            <button type="submit">生成考试卷</button>
          </form>

          <section v-if="createdPaper" class="paper-summary" aria-label="已生成考试卷">
            <h3>{{ createdPaper.name }}</h3>
            <p>{{ createdPaper.durationMinutes }} 分钟</p>
            <p>共 {{ createdPaper.questionIds.length }} 题</p>
            <RouterLink class="button-link" to="/exams/take">进入答题</RouterLink>
          </section>
        </aside>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { composeCustomExam, searchQuestions, type CustomExamPaper, type Question } from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()

const availableQuestions = ref<Question[]>([])

const draft = reactive({
  name: '集合专项测试',
  durationMinutes: 45
})
const selectedQuestionIds = ref<number[]>([])
const statusMessage = ref('')
const createdPaper = ref<CustomExamPaper | null>(null)

onMounted(loadQuestions)

async function loadQuestions() {
  statusMessage.value = ''
  try {
    availableQuestions.value = await searchQuestions()
    selectedQuestionIds.value = availableQuestions.value.map((question) => question.id)
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载题库失败，请检查本地后端是否启动。'
  }
}

async function createPaper() {
  statusMessage.value = ''
  if (selectedQuestionIds.value.length === 0) {
    createdPaper.value = null
    statusMessage.value = '请至少选择一道题。'
    return
  }

  try {
    createdPaper.value = await composeCustomExam({
      name: draft.name,
      durationMinutes: draft.durationMinutes,
      questionIds: selectedQuestionIds.value
    })
    const selectedQuestions = availableQuestions.value
      .filter((question) => selectedQuestionIds.value.includes(question.id))
      .map(toExamQuestion)
    window.sessionStorage.setItem(
      'studyCollectionExamPaper',
      JSON.stringify({
        ...createdPaper.value,
        questions: selectedQuestions
      })
    )
    statusMessage.value = '考试卷已生成，可以进入答题流程。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '生成考试卷失败，请检查本地后端是否启动。'
  }
}

function toExamQuestion(question: Question) {
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
    const match = line.match(/^([A-D])[\.\u3001\uff0e]\s*(.+)$/i)
    if (match) {
      options.push({ value: match[1].toUpperCase(), label: match[2].trim() })
    } else {
      stemLines.push(line)
    }
  })

  return {
    title: stemLines.join('\n'),
    options
  }
}

function fallbackOptionsFor(question: Question) {
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
</script>
