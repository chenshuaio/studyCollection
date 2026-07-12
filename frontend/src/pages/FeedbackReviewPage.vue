<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/knowledge-points">知识点管理</RouterLink>
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
          <p class="eyebrow">管理员工作台</p>
          <h1>反馈审核</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadFeedback">刷新反馈</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout feedback-review-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>待处理反馈</h2>
            <span class="panel-count">{{ totalFeedbackCount }} 条</span>
          </div>

          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>问题类型</th>
                <th>反馈数</th>
                <th>最近提交</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="group in feedbackGroups"
                :key="groupKey(group)"
                :class="{ selected: selectedGroupKey === groupKey(group) }"
                @click="selectGroup(group)"
              >
                <td>
                  <strong>{{ group.questionTitle }}</strong>
                  <small>#{{ group.questionId }} · {{ group.knowledgePoint }}</small>
                </td>
                <td>{{ typeText(group.type) }}</td>
                <td>{{ group.feedbackCount }} 条重复反馈</td>
                <td>{{ formatDateTime(group.latestAt) }}</td>
              </tr>
              <tr v-if="feedbackGroups.length === 0">
                <td colspan="4">当前没有待处理或待复核反馈。</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="workspace-panel question-form">
          <template v-if="selectedGroup">
            <div class="panel-header">
              <h2>处理反馈组</h2>
              <span class="panel-count">{{ selectedGroup.feedbackCount }} 条</span>
            </div>

            <dl class="question-context">
              <div>
                <dt>原题</dt>
                <dd>{{ selectedGroup.questionTitle }}</dd>
              </div>
              <div>
                <dt>题型与难度</dt>
                <dd>{{ questionTypeText(selectedGroup.questionType) }} · {{ difficultyText(selectedGroup.difficulty) }}</dd>
              </div>
              <div>
                <dt>题目来源</dt>
                <dd>{{ questionSourceText(selectedGroup.questionSource) }}</dd>
              </div>
              <div>
                <dt>当前答案</dt>
                <dd>{{ selectedGroup.currentAnswer }}</dd>
              </div>
              <div class="full-row">
                <dt>当前解析</dt>
                <dd>{{ selectedGroup.currentAnalysis || '暂无解析' }}</dd>
              </div>
            </dl>

            <div class="feedback-detail-section">
              <h3>用户反馈明细</h3>
              <ol class="feedback-detail-list">
                <li v-for="item in selectedGroup.items" :key="item.id">
                  <div class="feedback-detail-heading">
                    <strong>用户 #{{ item.userId }}</strong>
                    <span>{{ sourceText(item.sourceContext) }} · {{ statusText(item.status) }}</span>
                  </div>
                  <p>{{ item.content }}</p>
                  <p><span>用户答案</span>{{ item.submittedAnswer || '未记录' }}</p>
                  <small v-if="item.sourceReference">来源编号：{{ item.sourceReference }}</small>
                  <small>{{ formatDateTime(item.createdAt) }}</small>
                </li>
              </ol>
            </div>

            <label>
              修订说明
              <textarea v-model="changeSummary" aria-label="修订说明"></textarea>
            </label>
            <label>
              审核备注
              <textarea v-model="reviewNote" aria-label="审核备注"></textarea>
            </label>
            <label>
              新题干与选项（留空不改）
              <textarea v-model="correctedTitle" aria-label="新题干与选项"></textarea>
            </label>
            <label>
              新题型（留空不改）
              <select v-model="correctedType" aria-label="新题型">
                <option value="">保持原题型</option>
                <option value="SINGLE_CHOICE">单选题</option>
                <option value="MULTIPLE_CHOICE">多选题</option>
                <option value="TRUE_FALSE">判断题</option>
                <option value="FILL_BLANK">填空题</option>
                <option value="SHORT_ANSWER">简答题</option>
                <option value="PROGRAMMING">编程题</option>
              </select>
            </label>
            <label>
              新难度（留空不改）
              <select v-model="correctedDifficulty" aria-label="新难度">
                <option value="">保持原难度</option>
                <option value="BEGINNER">入门</option>
                <option value="INTERMEDIATE">进阶</option>
                <option value="ADVANCED">精通</option>
              </select>
            </label>
            <label>
              新知识点（留空不改）
              <input v-model="correctedKnowledgePoint" aria-label="新知识点" />
            </label>
            <label>
              新标准答案
              <textarea v-model="correctedAnswer" aria-label="新标准答案"></textarea>
            </label>
            <label>
              新题目解析
              <textarea v-model="correctedAnalysis" aria-label="新题目解析"></textarea>
            </label>
            <p v-if="statusMessage" class="form-message" aria-live="polite">{{ statusMessage }}</p>
            <div class="header-actions review-actions">
              <button type="button" data-action="accept-group" @click="acceptSelectedGroup">
                采纳并合并 {{ selectedGroup.feedbackCount }} 条
              </button>
              <button type="button" @click="rejectSelected">驳回最新一条</button>
              <button type="button" @click="markSelectedNeedsReview">标记最新一条待复核</button>
            </div>
          </template>
          <p v-else class="empty-state-copy">选择一组反馈后查看题目与用户作答上下文。</p>
        </article>
      </section>

      <section class="workspace-panel revision-history-panel">
        <div class="panel-header">
          <h2>修订历史</h2>
          <span class="panel-count">{{ revisions.length }} 次</span>
        </div>
        <ol v-if="revisions.length" class="revision-list">
          <li v-for="revision in revisions" :key="revision.id">
            <div>
              <strong>{{ revision.changeSummary }}</strong>
              <small>{{ formatDateTime(revision.revisedAt) }} · 管理员 #{{ revision.adminUserId }}</small>
            </div>
            <p>{{ revision.reviewNote }}</p>
            <p v-if="revision.beforeQuestion && revision.afterQuestion">
              答案：{{ revision.beforeQuestion.answer }} → {{ revision.afterQuestion.answer }}
            </p>
            <span v-if="revision.scoringAffected" class="revision-impact">影响历史评分</span>
          </li>
        </ol>
        <p v-else>暂无修订历史。</p>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  acceptQuestionFeedbackGroup,
  listPendingFeedbackGroups,
  listQuestionRevisions,
  markFeedbackNeedsReview,
  rejectQuestionFeedback,
  type AcceptFeedbackGroupPayload,
  type QuestionFeedbackGroup,
  type QuestionRevision
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'

const feedbackGroups = ref<QuestionFeedbackGroup[]>([])
const selectedGroupKey = ref('')
const revisions = ref<QuestionRevision[]>([])
const changeSummary = ref('答案从 A 修改为 B')
const reviewNote = ref('用户反馈属实')
const correctedTitle = ref('')
const correctedType = ref('')
const correctedDifficulty = ref('')
const correctedKnowledgePoint = ref('')
const correctedAnswer = ref('')
const correctedAnalysis = ref('')
const statusMessage = ref('')

const selectedGroup = computed(() => (
  feedbackGroups.value.find((group) => groupKey(group) === selectedGroupKey.value) ?? null
))
const selectedFeedbackId = computed(() => selectedGroup.value?.items[0]?.id ?? null)
const totalFeedbackCount = computed(() => (
  feedbackGroups.value.reduce((total, group) => total + group.feedbackCount, 0)
))

onMounted(loadFeedback)

async function loadFeedback() {
  statusMessage.value = ''
  try {
    feedbackGroups.value = await listPendingFeedbackGroups()
    const currentStillExists = feedbackGroups.value.some((group) => groupKey(group) === selectedGroupKey.value)
    if (!currentStillExists) {
      selectedGroupKey.value = feedbackGroups.value[0] ? groupKey(feedbackGroups.value[0]) : ''
    }
    await loadRevisions()
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载反馈失败，请检查本地后端是否启动。'
  }
}

async function selectGroup(group: QuestionFeedbackGroup) {
  selectedGroupKey.value = groupKey(group)
  statusMessage.value = ''
  correctedTitle.value = ''
  correctedType.value = ''
  correctedDifficulty.value = ''
  correctedKnowledgePoint.value = ''
  correctedAnswer.value = ''
  correctedAnalysis.value = ''
  await loadRevisions()
}

async function loadRevisions() {
  if (!selectedGroup.value) {
    revisions.value = []
    return
  }
  revisions.value = await listQuestionRevisions(selectedGroup.value.questionId)
}

async function acceptSelectedGroup() {
  const group = selectedGroup.value
  if (!group) {
    statusMessage.value = '请选择一组待处理反馈。'
    return
  }
  try {
    const payload: AcceptFeedbackGroupPayload = {
      feedbackIds: group.items.map((item) => item.id),
      changeSummary: changeSummary.value,
      reviewNote: reviewNote.value,
      correctedTitle: correctedTitle.value,
      correctedKnowledgePoint: correctedKnowledgePoint.value,
      correctedAnswer: correctedAnswer.value,
      correctedAnalysis: correctedAnalysis.value
    }
    if (correctedType.value) payload.correctedType = correctedType.value
    if (correctedDifficulty.value) payload.correctedDifficulty = correctedDifficulty.value
    const revision = await acceptQuestionFeedbackGroup(payload)
    await loadFeedback()
    if (!selectedGroup.value || selectedGroup.value.questionId === group.questionId) {
      revisions.value = [revision, ...revisions.value.filter((item) => item.id !== revision.id)]
    }
    statusMessage.value = `已采纳并合并 ${group.feedbackCount} 条反馈，题库已同步修订。`
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '采纳反馈失败，请检查本地后端是否启动。'
  }
}

async function rejectSelected() {
  if (selectedFeedbackId.value === null) {
    statusMessage.value = '请选择一组待处理反馈。'
    return
  }
  try {
    await rejectQuestionFeedback(selectedFeedbackId.value, { reviewNote: reviewNote.value })
    await loadFeedback()
    statusMessage.value = '最新一条反馈已驳回，审核备注已保存。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '驳回反馈失败。'
  }
}

async function markSelectedNeedsReview() {
  if (selectedFeedbackId.value === null) {
    statusMessage.value = '请选择一组待处理反馈。'
    return
  }
  try {
    await markFeedbackNeedsReview(selectedFeedbackId.value, { reviewNote: reviewNote.value })
    await loadFeedback()
    statusMessage.value = '最新一条反馈已标记为待复核，并继续保留在队列中。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '标记待复核失败。'
  }
}

function groupKey(group: QuestionFeedbackGroup) {
  return `${group.questionId}:${group.type}`
}

function typeText(type: string) {
  const names: Record<string, string> = {
    ANSWER_ERROR: '答案错误',
    EXPLANATION_ERROR: '解析错误',
    STEM_ERROR: '题干错误',
    OPTION_ERROR: '选项错误',
    KNOWLEDGE_POINT_ERROR: '知识点错误',
    DIFFICULTY_ERROR: '难度错误',
    OTHER: '其他'
  }
  return names[type] ?? type
}

function statusText(status: string) {
  const names: Record<string, string> = {
    PENDING: '待处理',
    ACCEPTED: '已采纳',
    REJECTED: '已驳回',
    NEEDS_REVIEW: '待复核'
  }
  return names[status] ?? status
}

function sourceText(source?: string) {
  const names: Record<string, string> = {
    PRACTICE: '练习',
    EXAM: '考试',
    MISTAKE_BOOK: '错题本',
    QUESTION_DETAIL: '题目详情',
    UNKNOWN: '未记录来源'
  }
  return names[source ?? 'UNKNOWN'] ?? source ?? '未记录来源'
}

function questionSourceText(source: string) {
  const names: Record<string, string> = {
    LOCAL_UPLOAD: '本地上传',
    KNOWLEDGE_FILE: '学习资料生成',
    ONLINE: '在线整理'
  }
  return names[source] ?? source
}

function questionTypeText(type: string) {
  const names: Record<string, string> = {
    SINGLE_CHOICE: '单选题',
    MULTIPLE_CHOICE: '多选题',
    TRUE_FALSE: '判断题',
    FILL_BLANK: '填空题',
    SHORT_ANSWER: '简答题',
    PROGRAMMING: '编程题'
  }
  return names[type] ?? type
}

function difficultyText(difficulty: string) {
  const names: Record<string, string> = {
    BEGINNER: '入门',
    INTERMEDIATE: '进阶',
    ADVANCED: '精通'
  }
  return names[difficulty] ?? difficulty
}

function formatDateTime(value?: string) {
  if (!value) return '时间未记录'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}
</script>

<style scoped>
.feedback-review-layout {
  align-items: start;
}

.table-panel td:first-child {
  display: grid;
  gap: 4px;
}

.table-panel th:last-child,
.table-panel td:last-child {
  min-width: 126px;
  white-space: nowrap;
}

.table-panel td small,
.feedback-detail-list small,
.revision-list small {
  color: var(--muted-text, #667085);
}

.question-context {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 20px;
  margin: 0;
  padding: 14px 0;
  border-block: 1px solid #e4e7ec;
}

.question-context div {
  min-width: 0;
}

.question-context .full-row {
  grid-column: 1 / -1;
}

.question-context dt {
  color: #667085;
  font-size: 13px;
}

.question-context dd {
  margin: 4px 0 0;
  overflow-wrap: anywhere;
}

.feedback-detail-section h3 {
  font-size: 15px;
  margin: 18px 0 10px;
}

.feedback-detail-list,
.revision-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.feedback-detail-list li,
.revision-list li {
  padding: 12px;
  border: 1px solid #e4e7ec;
  border-radius: 6px;
}

.feedback-detail-heading {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.feedback-detail-list p {
  margin: 7px 0;
}

.feedback-detail-list p span {
  color: #667085;
  margin-right: 8px;
}

.feedback-detail-list small {
  display: block;
  overflow-wrap: anywhere;
}

.review-actions {
  flex-wrap: wrap;
}

.revision-history-panel {
  margin-top: 20px;
}

.revision-list li {
  display: grid;
  gap: 8px;
}

.revision-list li > div {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.revision-list p {
  margin: 0;
}

.revision-impact {
  color: #b42318;
  font-weight: 600;
}

@media (max-width: 720px) {
  .question-context {
    grid-template-columns: 1fr;
  }

  .question-context .full-row {
    grid-column: auto;
  }

  .feedback-detail-heading,
  .revision-list li > div {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
