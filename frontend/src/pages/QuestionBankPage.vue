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
        <a href="#new-question">新增题目</a>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">题库工作台</p>
          <h1>题库管理</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadQuestions" aria-label="搜索题库">搜索题库</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="filter-bar" aria-label="题目筛选">
        <label>
          题干搜索
          <input v-model="filters.keyword" aria-label="题干搜索" placeholder="输入题干关键词" />
        </label>
        <label>
          知识点
          <select v-model="filters.knowledgePoint">
            <option value="">全部</option>
            <option>集合框架</option>
            <option>Java 基础</option>
            <option>JVM</option>
            <option>并发编程</option>
          </select>
        </label>
        <label>
          难度
          <select v-model="filters.difficulty">
            <option value="">全部</option>
            <option>BEGINNER</option>
            <option>INTERMEDIATE</option>
            <option>ADVANCED</option>
          </select>
        </label>
        <label>
          题型
          <select v-model="filters.type">
            <option value="">全部</option>
            <option>SINGLE_CHOICE</option>
            <option>MULTIPLE_CHOICE</option>
            <option>TRUE_FALSE</option>
            <option>FILL_BLANK</option>
            <option>SHORT_ANSWER</option>
            <option>PROGRAMMING</option>
          </select>
        </label>
      </section>

      <section class="question-layout">
        <article class="table-panel">
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>知识点</th>
                <th>难度</th>
                <th>题型</th>
                <th>答案</th>
                <th v-if="canDeleteQuestions">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in questions" :key="question.id">
                <td>{{ question.title }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.difficulty }}</td>
                <td>{{ question.type }}</td>
                <td>{{ question.answer }}</td>
                <td v-if="canDeleteQuestions">
                  <button type="button" aria-label="删除题目" @click="deleteFormalQuestion(question.id)">删除</button>
                </td>
              </tr>
              <tr v-if="questions.length === 0">
                <td :colspan="canDeleteQuestions ? 6 : 5">暂无题目，请先导入或新增题目。</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside id="new-question" class="workspace-panel">
          <h2>新增题目</h2>
          <form class="question-form" @submit.prevent="saveQuestion">
            <label>
              题干
              <textarea v-model="draft.title"></textarea>
            </label>
            <label>
              标准答案
              <input v-model="draft.answer" />
            </label>
            <label>
              解析
              <textarea v-model="draft.analysis"></textarea>
            </label>
            <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
            <button type="submit">保存题目</button>
          </form>
        </aside>
      </section>

      <section class="table-panel review-panel">
        <div class="panel-header">
          <h2>待审核导入</h2>
          <span class="panel-count">{{ pendingQuestions.length }} 题</span>
        </div>
        <p v-if="reviewStatus" class="form-message review-message">{{ reviewStatus }}</p>
        <table>
          <thead>
            <tr>
              <th>题目</th>
              <th>提交用户</th>
              <th>知识点</th>
              <th>难度</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="question in pendingQuestions" :key="question.id">
              <td>{{ question.title }}</td>
              <td>{{ question.submitterUserId }}</td>
              <td>{{ question.knowledgePoint }}</td>
              <td>{{ question.difficulty }}</td>
              <td>
                <div class="action-row">
                  <button type="button" aria-label="通过待审核题目" @click="approvePending(question.id)">通过</button>
                  <button type="button" aria-label="拒绝待审核题目" @click="rejectPending(question.id)">拒绝</button>
                </div>
              </td>
            </tr>
            <tr v-if="pendingQuestions.length === 0">
              <td colspan="5">暂无待审核导入题。</td>
            </tr>
          </tbody>
        </table>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  approvePendingQuestion,
  createQuestion,
  deleteQuestion,
  listPendingQuestions,
  rejectPendingQuestion,
  searchQuestions,
  type PendingQuestion,
  type Question
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'

const filters = reactive({
  keyword: '',
  knowledgePoint: '',
  difficulty: '',
  type: ''
})
const draft = reactive({
  title: 'HashMap 默认负载因子是多少？',
  type: 'SINGLE_CHOICE',
  difficulty: 'INTERMEDIATE',
  knowledgePoint: '集合框架',
  answer: 'A',
  analysis: 'HashMap 默认负载因子是 0.75。'
})
const statusMessage = ref('')
const reviewStatus = ref('')
const questions = ref<Question[]>([
  {
    id: 1,
    ...draft
  }
])
const pendingQuestions = ref<PendingQuestion[]>([])
const canDeleteQuestions = isAdmin()

onMounted(async () => {
  await loadQuestions()
  await loadPendingQuestions()
})

async function loadQuestions() {
  statusMessage.value = ''
  try {
    const result = await searchQuestions({ ...filters })
    questions.value = Array.isArray(result) ? result : []
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '刷新题库失败。'
  }
}

async function loadPendingQuestions() {
  reviewStatus.value = ''
  try {
    pendingQuestions.value = await listPendingQuestions()
  } catch (error) {
    reviewStatus.value = error instanceof Error ? error.message : '刷新待审核题失败。'
  }
}

async function saveQuestion() {
  statusMessage.value = ''
  try {
    const saved = await createQuestion({ ...draft })
    questions.value = [saved, ...questions.value.filter((question) => question.id !== saved.id)]
    statusMessage.value = '题目已保存到本地后端。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '保存失败，请检查本地后端是否启动。'
  }
}

async function deleteFormalQuestion(id: number) {
  statusMessage.value = ''
  if (!window.confirm('确认删除这道题目吗？删除后题库中将不再展示。')) {
    return
  }
  try {
    await deleteQuestion(id)
    await loadQuestions()
    statusMessage.value = '题目已删除。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '删除题目失败。'
  }
}

async function approvePending(id: number) {
  reviewStatus.value = ''
  try {
    await approvePendingQuestion(id)
    await loadPendingQuestions()
    await loadQuestions()
    reviewStatus.value = '审核通过，题目已入库。'
  } catch (error) {
    reviewStatus.value = error instanceof Error ? error.message : '审核通过失败。'
  }
}

async function rejectPending(id: number) {
  reviewStatus.value = ''
  try {
    await rejectPendingQuestion(id)
    await loadPendingQuestions()
    reviewStatus.value = '已拒绝该导入题。'
  } catch (error) {
    reviewStatus.value = error instanceof Error ? error.message : '拒绝失败。'
  }
}
</script>
