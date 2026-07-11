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
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">导入与生成</p>
          <h1>题目导入</h1>
        </div>
        <div class="header-actions">
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="import-layout">
        <article class="workspace-panel import-source-panel">
          <h2>结构化题目导入</h2>
          <label class="file-upload">
            <span>上传 JSON / CSV / XLSX / TXT / MD</span>
            <input
              type="file"
              accept=".json,.csv,.xlsx,.txt,.md,application/json,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,text/plain,text/markdown"
              aria-label="上传结构化题目文件"
              @change="uploadStructuredQuestions"
            />
          </label>
          <label class="editor-label">
            粘贴 Markdown / TXT 题目
            <textarea v-model="rawContent" class="import-editor" aria-label="粘贴题目"></textarea>
          </label>
          <p v-if="previewStatus" class="form-message">{{ previewStatus }}</p>
          <div class="action-row">
            <button type="button" @click="generatePreview">解析文本</button>
            <button type="button" aria-label="提交预览题审核" @click="savePreviewQuestions">提交审核</button>
          </div>
        </article>

        <EditableQuestionTable v-model="previewQuestions" title="解析预览" />

        <article class="workspace-panel import-source-panel">
          <h2>学习内容生成题库</h2>
          <label class="file-upload">
            <span>上传 Java 学习资料</span>
            <input
              type="file"
              accept=".txt,.md,.csv,.xlsx,.docx,.pdf,text/plain,text/markdown,text/csv,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/pdf"
              aria-label="上传 Java 学习资料"
              @change="uploadKnowledgeMaterial"
            />
          </label>
          <label class="editor-label">
            Java 学习知识内容
            <textarea v-model="knowledgeContent" class="import-editor" aria-label="Java 学习知识内容"></textarea>
          </label>
          <p v-if="generationStatus" class="form-message">{{ generationStatus }}</p>
          <div class="action-row">
            <button type="button" @click="generateQuestionBank">分析生成题库</button>
            <button type="button" aria-label="提交生成题审核" @click="saveGeneratedQuestions">提交生成题审核</button>
          </div>
        </article>

        <EditableQuestionTable v-model="generatedQuestions" title="生成题库预览" />
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import EditableQuestionTable from '../components/EditableQuestionTable.vue'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import {
  generateKnowledgeQuestions,
  previewImport,
  submitPendingQuestion,
  uploadKnowledgeFile,
  uploadQuestionFile,
  type QuestionPayload
} from '../api'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()

const rawContent = ref(`## 单选题
题目: Java 中 int 默认值是多少？
A. 0
B. null
答案: A
解析: Java 成员变量 int 的默认值为 0。
知识点: Java 基础
难度: BEGINNER`)
const knowledgeContent = ref('HashMap 是 Java 集合框架中的常用 Map 实现。HashMap 默认负载因子是 0.75，达到阈值后会进行扩容。Java 中局部变量没有默认值，必须先赋值再使用。')
const previewStatus = ref('')
const generationStatus = ref('')
const previewQuestions = ref<QuestionPayload[]>([
  {
    title: 'Java 中 int 默认值是多少？\nA. 0\nB. null',
    type: 'SINGLE_CHOICE',
    difficulty: 'BEGINNER',
    knowledgePoint: 'Java 基础',
    answer: 'A',
    analysis: '由导入预览提交审核'
  }
])
const generatedQuestions = ref<QuestionPayload[]>([])

async function generatePreview() {
  previewStatus.value = ''
  try {
    previewQuestions.value = await previewImport(rawContent.value)
    previewStatus.value = `已解析 ${previewQuestions.value.length} 道题，请编辑确认后提交审核。`
  } catch (error) {
    previewStatus.value = error instanceof Error ? error.message : '生成预览失败，请检查题目格式或本地后端。'
  }
}

async function uploadStructuredQuestions(event: Event) {
  previewStatus.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    previewQuestions.value = await uploadQuestionFile(file)
    previewStatus.value = `已从 ${file.name} 解析 ${previewQuestions.value.length} 道题，请编辑确认后提交审核。`
  } catch (error) {
    previewStatus.value = error instanceof Error ? error.message : '题目文件解析失败，请检查格式和字段。'
  } finally {
    input.value = ''
  }
}

async function savePreviewQuestions() {
  previewStatus.value = ''
  if (previewQuestions.value.length === 0) {
    previewStatus.value = '请先解析或新增至少一道题目。'
    return
  }
  try {
    for (const question of previewQuestions.value) {
      await submitPendingQuestion({ ...question })
    }
    previewStatus.value = `已提交管理员审核 ${previewQuestions.value.length} 道预览题。`
  } catch (error) {
    previewStatus.value = error instanceof Error ? error.message : '预览题提交审核失败，请检查题目字段。'
  }
}

async function generateQuestionBank() {
  generationStatus.value = ''
  try {
    generatedQuestions.value = await generateKnowledgeQuestions(knowledgeContent.value)
    generationStatus.value = `已生成 ${generatedQuestions.value.length} 道题，请编辑确认后提交审核。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '分析失败，请检查本地后端是否启动。'
  }
}

async function uploadKnowledgeMaterial(event: Event) {
  generationStatus.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) return
  try {
    generatedQuestions.value = await uploadKnowledgeFile(file)
    generationStatus.value = `已从 ${file.name} 生成 ${generatedQuestions.value.length} 道题，请预览后提交审核。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '上传分析失败，请检查文件格式或本地后端。'
  } finally {
    input.value = ''
  }
}

async function saveGeneratedQuestions() {
  generationStatus.value = ''
  if (generatedQuestions.value.length === 0) {
    generationStatus.value = '请先分析学习内容或新增至少一道题目。'
    return
  }
  try {
    for (const question of generatedQuestions.value) {
      await submitPendingQuestion({ ...question })
    }
    generationStatus.value = `已提交管理员审核 ${generatedQuestions.value.length} 道生成题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '提交审核失败，请检查题目字段。'
  }
}
</script>
