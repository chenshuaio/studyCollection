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
          <p class="eyebrow">导入预览</p>
          <h1>题目导入</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="generatePreview">生成预览</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="import-layout">
        <article class="workspace-panel">
          <h2>粘贴题目</h2>
          <p>当前支持 Markdown/TXT 风格的结构化题目文本，后续接入 PDF、DOCX、XLSX 解析。</p>
          <textarea class="import-editor" v-model="rawContent" aria-label="粘贴题目"></textarea>
          <p v-if="previewStatus" class="form-message">{{ previewStatus }}</p>
          <button type="button" @click="generatePreview">生成预览</button>
        </article>

        <article class="table-panel">
          <div class="panel-header">
            <h2>解析预览</h2>
            <button class="button-link" type="button" aria-label="提交预览题审核" @click="savePreviewQuestions">提交审核</button>
          </div>
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>答案</th>
                <th>知识点</th>
                <th>难度</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in previewQuestions" :key="question.title">
                <td>{{ question.title }}</td>
                <td>{{ question.answer }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.difficulty }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <article class="workspace-panel">
          <h2>学习内容生成题库</h2>
          <p>粘贴或上传 Java 学习资料，本地规则会提取知识点并生成可入库题目。</p>
          <label class="file-upload">
            <span>上传学习资料</span>
            <input
              type="file"
              accept=".txt,.md,.csv,.docx,.pdf,text/plain,text/markdown,text/csv,application/vnd.openxmlformats-officedocument.wordprocessingml.document,application/pdf"
              aria-label="上传 Java 学习资料"
              @change="uploadKnowledgeMaterial"
            />
          </label>
          <textarea class="import-editor" v-model="knowledgeContent" aria-label="Java 学习知识内容"></textarea>
          <p v-if="generationStatus" class="form-message">{{ generationStatus }}</p>
          <div class="action-row">
            <button type="button" @click="generateQuestionBank">分析生成题库</button>
            <button type="button" @click="saveGeneratedQuestions">提交生成题审核</button>
          </div>
        </article>

        <article class="table-panel">
          <div class="panel-header">
            <h2>生成题库</h2>
            <span class="panel-count">{{ generatedQuestions.length }} 题</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>题目</th>
                <th>题型</th>
                <th>知识点</th>
                <th>答案</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in generatedQuestions" :key="question.title">
                <td>{{ question.title }}</td>
                <td>{{ question.type }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.answer }}</td>
              </tr>
            </tbody>
          </table>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import {
  generateKnowledgeQuestions,
  previewImport,
  submitPendingQuestion,
  uploadKnowledgeFile,
  type PreviewQuestion,
  type QuestionPayload
} from '../api'
import { getCurrentUser } from '../session'
import { isAdmin } from '../permissions'

const isAdminUser = isAdmin()

const rawContent = ref(`## 单选题
题目: Java 中 int 默认值是多少？
A. 0
B. null
答案: A
知识点: Java 基础
难度: BEGINNER`)
const knowledgeContent = ref('HashMap 是 Java 集合框架中的常用 Map 实现。HashMap 默认负载因子是 0.75，达到阈值后会进行扩容。Java 中局部变量没有默认值，必须先赋值再使用。')
const previewStatus = ref('')
const generationStatus = ref('')
const previewQuestions = ref<PreviewQuestion[]>([
  {
    title: 'Java 中 int 默认值是多少？',
    answer: 'A',
    knowledgePoint: 'Java 基础',
    difficulty: 'BEGINNER'
  }
])
const generatedQuestions = ref<QuestionPayload[]>([])

async function generatePreview() {
  previewStatus.value = ''
  try {
    previewQuestions.value = await previewImport(rawContent.value)
    previewStatus.value = '预览已由本地后端生成。'
  } catch (error) {
    previewStatus.value = error instanceof Error ? error.message : '生成预览失败，请检查本地后端是否启动。'
  }
}

async function savePreviewQuestions() {
  previewStatus.value = ''
  try {
    for (const question of previewQuestions.value) {
      await submitPendingQuestion({
        submitterUserId: currentUserId(),
        title: question.title,
        type: 'SINGLE_CHOICE',
        difficulty: question.difficulty,
        knowledgePoint: question.knowledgePoint,
        answer: question.answer,
        analysis: '由导入预览提交审核'
      })
    }
    previewStatus.value = `已提交管理员审核 ${previewQuestions.value.length} 道预览题。`
  } catch (error) {
    previewStatus.value = error instanceof Error ? error.message : '预览题提交审核失败，请检查本地后端是否启动。'
  }
}

async function generateQuestionBank() {
  generationStatus.value = ''
  try {
    generatedQuestions.value = await generateKnowledgeQuestions(knowledgeContent.value)
    generationStatus.value = `已生成 ${generatedQuestions.value.length} 道题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '分析失败，请检查本地后端是否启动。'
  }
}

async function uploadKnowledgeMaterial(event: Event) {
  generationStatus.value = ''
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  try {
    generatedQuestions.value = await uploadKnowledgeFile(file)
    generationStatus.value = `已从 ${file.name} 生成 ${generatedQuestions.value.length} 道题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '上传分析失败，请检查文件格式或本地后端。'
  } finally {
    input.value = ''
  }
}

async function saveGeneratedQuestions() {
  generationStatus.value = ''
  try {
    for (const question of generatedQuestions.value) {
      await submitPendingQuestion({
        submitterUserId: currentUserId(),
        ...question
      })
    }
    generationStatus.value = `已提交管理员审核 ${generatedQuestions.value.length} 道生成题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '提交审核失败，请检查本地后端是否启动。'
  }
}

function currentUserId() {
  return getCurrentUser()?.userId ?? 7
}
</script>
