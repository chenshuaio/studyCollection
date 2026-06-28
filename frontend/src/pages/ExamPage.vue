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
          <p class="eyebrow">个人考试卷</p>
          <h1>考试中心</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">练习模式</RouterLink>
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
import { reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { composeCustomExam, type CustomExamPaper } from '../api'
import LogoutButton from '../components/LogoutButton.vue'

const availableQuestions = [
  {
    id: 1,
    title: 'HashMap 默认负载因子是多少？',
    type: 'SINGLE_CHOICE',
    knowledgePoint: '集合框架',
    difficulty: 'INTERMEDIATE',
    answer: 'A',
    analysis: 'HashMap 默认负载因子是 0.75，达到阈值后会触发扩容。',
    options: [
      { value: 'A', label: '0.75' },
      { value: 'B', label: '0.5' },
      { value: 'C', label: '1.0' },
      { value: 'D', label: '2.0' }
    ]
  },
  {
    id: 2,
    title: 'Java 局部变量必须先赋值再使用，这句话是否正确？',
    type: 'TRUE_FALSE',
    knowledgePoint: 'Java 基础',
    difficulty: 'BEGINNER',
    answer: 'true',
    analysis: 'Java 局部变量没有默认值，必须先赋值再使用。',
    options: [
      { value: 'true', label: '正确' },
      { value: 'false', label: '错误' }
    ]
  },
  {
    id: 3,
    title: 'ArrayList 扩容通常发生在什么时候？',
    type: 'SINGLE_CHOICE',
    knowledgePoint: '集合框架',
    difficulty: 'INTERMEDIATE',
    answer: 'A',
    analysis: 'ArrayList 在容量不足以容纳新增元素时会触发扩容。',
    options: [
      { value: 'A', label: '容量不足以容纳新增元素时' },
      { value: 'B', label: '每次新增元素时' },
      { value: 'C', label: '调用 get 方法时' },
      { value: 'D', label: '创建对象时立即扩容' }
    ]
  }
]

const draft = reactive({
  name: '集合专项测试',
  durationMinutes: 45
})
const selectedQuestionIds = ref<number[]>([1, 2, 3])
const statusMessage = ref('')
const createdPaper = ref<CustomExamPaper | null>(null)

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
    const selectedQuestions = availableQuestions.filter((question) => selectedQuestionIds.value.includes(question.id))
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
</script>
