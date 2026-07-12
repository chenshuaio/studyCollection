<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">{{ isAdminUser ? '题库管理' : '我的题库' }}</RouterLink>
        <RouterLink v-if="isAdminUser" to="/knowledge-points">知识点管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink v-if="isAdminUser" to="/exam-rules/manage">考试规则</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
        <a v-if="isAdminUser" href="#new-question">新增题目</a>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">{{ isAdminUser ? '公共题库工作台' : '公共题库与个人题库' }}</p>
          <h1>{{ isAdminUser ? '题库管理' : '我的题库' }}</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadQuestions" aria-label="搜索题库">搜索题库</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <div v-if="!isAdminUser" class="scope-tabs" role="tablist" aria-label="题库范围">
        <button
          v-for="option in scopeOptions"
          :key="option.value"
          type="button"
          role="tab"
          :aria-selected="filters.scope === option.value"
          :class="{ active: filters.scope === option.value }"
          @click="setScope(option.value)"
        >
          {{ option.label }}
        </button>
      </div>

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

      <section :class="['question-layout', { 'user-bank-layout': !isAdminUser }]">
        <article class="table-panel">
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>题库</th>
                <th>知识点</th>
                <th>难度</th>
                <th>题型</th>
                <th v-if="isAdminUser">答案</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in questions" :key="question.id">
                <td>{{ question.title }}</td>
                <td><span class="bank-badge">{{ scopeLabel(question) }}</span></td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.difficulty }}</td>
                <td>{{ question.type }}</td>
                <td v-if="isAdminUser">{{ question.answer }}</td>
                <td>
                  <button
                    v-if="canDeleteQuestion(question)"
                    type="button"
                    aria-label="删除题目"
                    @click="deleteFormalQuestion(question.id)"
                  >
                    删除
                  </button>
                  <span v-else>--</span>
                </td>
              </tr>
              <tr v-if="questions.length === 0">
                <td :colspan="isAdminUser ? 7 : 6">暂无符合条件的题目。</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside v-if="isAdminUser" id="new-question" class="workspace-panel">
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

      <p v-if="!isAdminUser && statusMessage" class="form-message bank-status">{{ statusMessage }}</p>

      <section v-if="isAdminUser" class="table-panel review-panel">
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
              <th>目标题库</th>
              <th>知识点</th>
              <th>难度</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="question in pendingQuestions" :key="question.id">
              <td>{{ question.title }}</td>
              <td>{{ question.submitterUserId }}</td>
              <td>{{ pendingScopeLabel(question) }}</td>
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
              <td colspan="6">暂无待审核导入题。</td>
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
  type Question,
  type QuestionBankScope
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import { isAdmin } from '../permissions'
import { getCurrentUser } from '../session'

const isAdminUser = isAdmin()
const currentUser = getCurrentUser()
const defaultScope: QuestionBankScope = isAdminUser ? 'PUBLIC' : 'ALL'
const scopeOptions: Array<{ value: QuestionBankScope; label: string }> = [
  { value: 'ALL', label: '全部可用' },
  { value: 'PUBLIC', label: '公共题库' },
  { value: 'PERSONAL', label: '我的题库' }
]
const filters = reactive<{
  keyword: string
  knowledgePoint: string
  difficulty: string
  type: string
  scope: QuestionBankScope
}>({
  keyword: '',
  knowledgePoint: '',
  difficulty: '',
  type: '',
  scope: defaultScope
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
const questions = ref<Question[]>([])
const pendingQuestions = ref<PendingQuestion[]>([])

onMounted(async () => {
  await loadQuestions()
  if (isAdminUser) {
    await loadPendingQuestions()
  }
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

async function setScope(scope: QuestionBankScope) {
  filters.scope = scope
  await loadQuestions()
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
    statusMessage.value = '题目已保存到公共题库。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '保存失败，请检查本地后端是否启动。'
  }
}

function canDeleteQuestion(question: Question) {
  return isAdminUser || question.ownerUserId === currentUser?.userId
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

function scopeLabel(question: Question) {
  return question.ownerUserId == null ? '公共题库' : '我的题库'
}

function pendingScopeLabel(question: PendingQuestion) {
  return question.targetScope === 'PERSONAL' ? '个人题库' : '公共题库'
}
</script>

<style scoped>
.scope-tabs {
  background: #eef2f6;
  border-radius: 7px;
  display: inline-grid;
  gap: 3px;
  grid-template-columns: repeat(3, minmax(96px, 1fr));
  margin-bottom: 14px;
  padding: 3px;
}

.scope-tabs button {
  background: transparent;
  border: 0;
  border-radius: 5px;
  color: #475467;
  cursor: pointer;
  font: inherit;
  font-weight: 700;
  min-height: 38px;
  padding: 0 12px;
}

.scope-tabs button.active {
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(16, 24, 40, 0.12);
  color: #175cd3;
}

.user-bank-layout {
  grid-template-columns: minmax(0, 1fr);
}

.bank-badge {
  background: #eef4ff;
  border-radius: 5px;
  color: #175cd3;
  display: inline-block;
  font-size: 12px;
  font-weight: 800;
  padding: 4px 7px;
  white-space: nowrap;
}

.bank-status {
  margin-top: 12px;
}

@media (max-width: 560px) {
  .scope-tabs {
    display: grid;
    grid-template-columns: 1fr;
    width: 100%;
  }
}
</style>
