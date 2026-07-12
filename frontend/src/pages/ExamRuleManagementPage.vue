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
        <RouterLink to="/exam-rules/manage">考试规则</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
        <RouterLink to="/knowledge-points">知识点管理</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">管理员配置</p>
          <h1>考试规则管理</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/exams">查看考试中心</RouterLink>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <p v-if="statusMessage" class="dashboard-message" role="status">{{ statusMessage }}</p>

      <section class="rule-management-layout">
        <article class="workspace-panel rule-editor">
          <div class="panel-header">
            <div>
              <h2>{{ editingId === null ? '新建规则' : '编辑规则' }}</h2>
              <span class="panel-count">保存后为草稿</span>
            </div>
          </div>

          <form class="question-form" @submit.prevent="saveRule">
            <label>
              考试名称
              <input v-model="draft.name" maxlength="128" required />
            </label>
            <label>
              考试说明
              <textarea v-model="draft.description" maxlength="1000" />
            </label>
            <div class="rule-basic-grid">
              <label>
                总题量
                <input v-model.number="draft.totalQuestions" type="number" min="1" max="200" required />
              </label>
              <label>
                时限（分钟）
                <input v-model.number="draft.durationMinutes" type="number" min="1" max="480" required />
              </label>
            </div>

            <fieldset class="rule-fieldset">
              <legend>知识点范围</legend>
              <p class="field-hint">不勾选时使用全部公共题库。</p>
              <div class="knowledge-checks">
                <label v-for="point in enabledKnowledgePoints" :key="point.id">
                  <input v-model="draft.knowledgePoints" type="checkbox" :value="point.name" />
                  <span>{{ point.name }}</span>
                </label>
                <span v-if="enabledKnowledgePoints.length === 0" class="field-hint">暂无启用的知识点。</span>
              </div>
            </fieldset>

            <fieldset class="rule-fieldset">
              <legend>题型配额</legend>
              <div class="quota-grid">
                <label v-for="item in questionTypes" :key="item.value">
                  {{ item.label }}
                  <input
                    v-model.number="draft.typeQuotas[item.value]"
                    data-quota-type
                    type="number"
                    min="0"
                    :max="draft.totalQuestions"
                  />
                </label>
              </div>
              <p class="quota-total" :class="{ invalid: typeTotal !== draft.totalQuestions }">
                题型合计 {{ typeTotal }} / {{ draft.totalQuestions }}
              </p>
            </fieldset>

            <fieldset class="rule-fieldset">
              <legend>难度配额</legend>
              <div class="quota-grid quota-grid--difficulty">
                <label v-for="item in difficulties" :key="item.value">
                  {{ item.label }}
                  <input
                    v-model.number="draft.difficultyQuotas[item.value]"
                    data-quota-difficulty
                    type="number"
                    min="0"
                    :max="draft.totalQuestions"
                  />
                </label>
              </div>
              <p class="quota-total" :class="{ invalid: difficultyTotal !== draft.totalQuestions }">
                难度合计 {{ difficultyTotal }} / {{ draft.totalQuestions }}
              </p>
            </fieldset>

            <div class="action-row">
              <button type="submit" :disabled="saving || !quotaValid">
                {{ saving ? '正在保存...' : editingId === null ? '保存草稿' : '保存修改' }}
              </button>
              <button v-if="editingId !== null" class="secondary-button" type="button" @click="resetDraft">
                取消编辑
              </button>
            </div>
          </form>
        </article>

        <article class="table-panel rule-list-panel">
          <div class="panel-header">
            <div>
              <h2>规则列表</h2>
              <span class="panel-count">{{ rules.length }} 条</span>
            </div>
          </div>
          <table>
            <thead>
              <tr>
                <th>考试</th>
                <th>配置</th>
                <th>状态</th>
                <th>更新时间</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in rules" :key="rule.id">
                <td>
                  <strong>{{ rule.name }}</strong>
                  <small>{{ rule.description || '暂无说明' }}</small>
                  <small>{{ knowledgeScope(rule) }}</small>
                </td>
                <td>
                  <span>{{ rule.totalQuestions }} 题 · {{ rule.durationMinutes }} 分钟</span>
                  <small>{{ positiveQuotaSummary(rule.typeQuotas, questionTypes) }}</small>
                  <small>{{ positiveQuotaSummary(rule.difficultyQuotas, difficulties) }}</small>
                </td>
                <td>
                  <span class="rule-status" :class="`rule-status--${rule.status.toLowerCase()}`">
                    {{ rule.status === 'PUBLISHED' ? '已发布' : '草稿' }}
                  </span>
                </td>
                <td>{{ formatDateTime(rule.updatedAt) }}</td>
                <td>
                  <div class="rule-actions">
                    <button type="button" @click="editRule(rule)">编辑</button>
                    <button
                      v-if="rule.status === 'DRAFT'"
                      data-action="publish"
                      type="button"
                      @click="publish(rule)"
                    >
                      发布
                    </button>
                    <button v-else type="button" @click="unpublish(rule)">停用</button>
                    <button class="danger-action" type="button" @click="removeRule(rule)">删除</button>
                  </div>
                </td>
              </tr>
              <tr v-if="rules.length === 0">
                <td colspan="5">暂无考试规则，请先创建草稿。</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createExamRule,
  deleteExamRule,
  listAdminExamRules,
  listKnowledgePoints,
  publishExamRule,
  unpublishExamRule,
  updateExamRule,
  type ExamRule,
  type ExamRulePayload,
  type KnowledgePoint
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'

const questionTypes = [
  { value: 'SINGLE_CHOICE', label: '单选题' },
  { value: 'MULTIPLE_CHOICE', label: '多选题' },
  { value: 'TRUE_FALSE', label: '判断题' },
  { value: 'FILL_BLANK', label: '填空题' },
  { value: 'SHORT_ANSWER', label: '简答题' },
  { value: 'PROGRAMMING', label: '编程题' }
] as const

const difficulties = [
  { value: 'BEGINNER', label: '入门' },
  { value: 'INTERMEDIATE', label: '进阶' },
  { value: 'ADVANCED', label: '精通' }
] as const

const rules = ref<ExamRule[]>([])
const knowledgePoints = ref<KnowledgePoint[]>([])
const statusMessage = ref('')
const saving = ref(false)
const editingId = ref<number | null>(null)
const draft = reactive(freshDraft())

const enabledKnowledgePoints = computed(() => knowledgePoints.value.filter((point) => point.enabled))
const typeTotal = computed(() => sumQuotas(draft.typeQuotas))
const difficultyTotal = computed(() => sumQuotas(draft.difficultyQuotas))
const quotaValid = computed(() =>
  draft.totalQuestions >= 1
  && typeTotal.value === draft.totalQuestions
  && difficultyTotal.value === draft.totalQuestions
)

onMounted(async () => {
  await Promise.all([loadRules(), loadKnowledgePoints()])
})

async function loadRules() {
  try {
    rules.value = await listAdminExamRules()
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载考试规则失败。')
  }
}

async function loadKnowledgePoints() {
  try {
    knowledgePoints.value = await listKnowledgePoints()
  } catch (error) {
    statusMessage.value = errorMessage(error, '加载知识点失败。')
  }
}

async function saveRule() {
  statusMessage.value = ''
  if (!draft.name.trim()) {
    statusMessage.value = '请输入考试名称。'
    return
  }
  if (!quotaValid.value) {
    statusMessage.value = '题型配额和难度配额必须分别等于总题量。'
    return
  }
  saving.value = true
  try {
    const payload = toPayload()
    const saved = editingId.value === null
      ? await createExamRule(payload)
      : await updateExamRule(editingId.value, payload)
    replaceRule(saved)
    statusMessage.value = editingId.value === null
      ? '考试规则草稿已创建。'
      : '考试规则已更新，请重新发布后供学员使用。'
    resetDraft()
  } catch (error) {
    statusMessage.value = errorMessage(error, '保存考试规则失败。')
  } finally {
    saving.value = false
  }
}

function editRule(rule: ExamRule) {
  editingId.value = rule.id
  Object.assign(draft, {
    name: rule.name,
    description: rule.description,
    durationMinutes: rule.durationMinutes,
    totalQuestions: rule.totalQuestions,
    knowledgePoints: [...rule.knowledgePoints],
    typeQuotas: completeQuotas(questionTypes, rule.typeQuotas),
    difficultyQuotas: completeQuotas(difficulties, rule.difficultyQuotas)
  })
  statusMessage.value = '正在编辑该规则，保存后状态将变为草稿。'
}

async function publish(rule: ExamRule) {
  try {
    replaceRule(await publishExamRule(rule.id))
    statusMessage.value = '考试规则已发布，学员现在可以参加。'
  } catch (error) {
    statusMessage.value = errorMessage(error, '发布失败，请检查题库是否满足配额。')
  }
}

async function unpublish(rule: ExamRule) {
  try {
    replaceRule(await unpublishExamRule(rule.id))
    statusMessage.value = '考试规则已停用，进行中的考试不受影响。'
  } catch (error) {
    statusMessage.value = errorMessage(error, '停用考试规则失败。')
  }
}

async function removeRule(rule: ExamRule) {
  if (!window.confirm(`确认删除“${rule.name}”吗？`)) {
    return
  }
  try {
    await deleteExamRule(rule.id)
    rules.value = rules.value.filter((candidate) => candidate.id !== rule.id)
    if (editingId.value === rule.id) {
      resetDraft()
    }
    statusMessage.value = '考试规则已删除。'
  } catch (error) {
    statusMessage.value = errorMessage(error, '删除考试规则失败。')
  }
}

function resetDraft() {
  editingId.value = null
  Object.assign(draft, freshDraft())
}

function freshDraft(): ExamRulePayload {
  return {
    name: '',
    description: '',
    durationMinutes: 60,
    totalQuestions: 10,
    knowledgePoints: [],
    typeQuotas: completeQuotas(questionTypes, { SINGLE_CHOICE: 10 }),
    difficultyQuotas: completeQuotas(difficulties, { BEGINNER: 10 })
  }
}

function toPayload(): ExamRulePayload {
  return {
    name: draft.name.trim(),
    description: draft.description.trim(),
    durationMinutes: draft.durationMinutes,
    totalQuestions: draft.totalQuestions,
    knowledgePoints: [...draft.knowledgePoints],
    typeQuotas: { ...draft.typeQuotas },
    difficultyQuotas: { ...draft.difficultyQuotas }
  }
}

function completeQuotas(
  options: ReadonlyArray<{ value: string }>,
  values: Record<string, number>
) {
  return Object.fromEntries(options.map((option) => [option.value, values[option.value] ?? 0]))
}

function sumQuotas(values: Record<string, number>) {
  return Object.values(values).reduce((total, value) => total + (Number(value) || 0), 0)
}

function replaceRule(rule: ExamRule) {
  rules.value = [rule, ...rules.value.filter((candidate) => candidate.id !== rule.id)]
}

function positiveQuotaSummary(
  quotas: Record<string, number>,
  options: ReadonlyArray<{ value: string; label: string }>
) {
  return options
    .filter((option) => (quotas[option.value] ?? 0) > 0)
    .map((option) => `${option.label} ${quotas[option.value]}`)
    .join('、') || '未配置'
}

function knowledgeScope(rule: ExamRule) {
  return rule.knowledgePoints.length > 0 ? rule.knowledgePoints.join('、') : '全部知识点'
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
.rule-management-layout {
  display: grid;
  gap: 16px;
  grid-template-columns: minmax(340px, 430px) minmax(0, 1fr);
}

.rule-management-layout > * {
  min-width: 0;
}

.rule-editor {
  align-self: start;
}

.rule-editor .panel-header {
  padding: 0 0 16px;
}

.rule-editor input,
.rule-editor textarea {
  border: 1px solid #cfd8e6;
  border-radius: 7px;
  color: #172033;
  font: inherit;
  padding: 10px 12px;
  width: 100%;
}

.rule-basic-grid,
.quota-grid {
  display: grid;
  gap: 12px;
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.rule-fieldset {
  border: 1px solid #dfe5ee;
  border-radius: 7px;
  display: grid;
  gap: 12px;
  margin: 0;
  min-width: 0;
  padding: 14px;
}

.rule-fieldset legend {
  color: #344054;
  font-weight: 800;
  padding: 0 6px;
}

.field-hint {
  color: #667085;
  font-size: 13px;
  margin: 0;
}

.knowledge-checks {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.knowledge-checks label {
  align-items: center;
  border: 1px solid #dfe5ee;
  border-radius: 7px;
  cursor: pointer;
  display: inline-flex;
  gap: 7px;
  padding: 8px 10px;
}

.knowledge-checks input {
  height: 16px;
  padding: 0;
  width: 16px;
}

.quota-total {
  color: #087443;
  font-weight: 800;
  margin: 0;
}

.quota-total.invalid {
  color: #b42318;
}

.secondary-button {
  background: #eef2f7 !important;
  color: #344054 !important;
}

.rule-list-panel strong,
.rule-list-panel small {
  display: block;
}

.rule-list-panel small {
  color: #667085;
  line-height: 1.5;
  margin-top: 4px;
}

.rule-status {
  border-radius: 6px;
  display: inline-block;
  font-size: 13px;
  font-weight: 800;
  padding: 5px 8px;
  white-space: nowrap;
}

.rule-status--draft {
  background: #fff4df;
  color: #8a4b08;
}

.rule-status--published {
  background: #e8f7ef;
  color: #087443;
}

.rule-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 104px;
}

.rule-actions button {
  background: transparent;
  border: 0;
  color: #1f6feb;
  cursor: pointer;
  font: inherit;
  font-weight: 800;
  padding: 3px 0;
}

.rule-actions .danger-action {
  color: #b42318;
}

@media (max-width: 1080px) {
  .rule-management-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .rule-basic-grid,
  .quota-grid {
    grid-template-columns: 1fr;
  }
}
</style>
