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
          <p class="eyebrow">导入预览</p>
          <h1>题目导入</h1>
        </div>
        <button type="button" @click="generatePreview">生成预览</button>
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
            <RouterLink class="button-link" to="/questions">确认入库</RouterLink>
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
          <p>粘贴 Java 学习知识内容，本地规则会提取知识点并生成可入库题目。</p>
          <textarea class="import-editor" v-model="knowledgeContent" aria-label="Java 学习知识内容"></textarea>
          <p v-if="generationStatus" class="form-message">{{ generationStatus }}</p>
          <div class="action-row">
            <button type="button" @click="generateQuestionBank">分析生成题库</button>
            <button type="button" @click="saveGeneratedQuestions">确认生成题入库</button>
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
import {
  createQuestion,
  generateKnowledgeQuestions,
  previewImport,
  type PreviewQuestion,
  type QuestionPayload
} from '../api'

const rawContent = ref(`## 单选题
题目: Java 中 int 默认值是多少？
A. 0
B. null
答案: A
知识点: Java 基础
难度: BEGINNER`)
const knowledgeContent = ref(`HashMap 是 Java 集合框架中的常用 Map 实现。
HashMap 默认负载因子是 0.75，达到阈值后会进行扩容。
Java 中局部变量没有默认值，必须先赋值再使用。`)
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

async function generateQuestionBank() {
  generationStatus.value = ''
  try {
    generatedQuestions.value = await generateKnowledgeQuestions(knowledgeContent.value)
    generationStatus.value = `已生成 ${generatedQuestions.value.length} 道题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '分析失败，请检查本地后端是否启动。'
  }
}

async function saveGeneratedQuestions() {
  generationStatus.value = ''
  try {
    for (const question of generatedQuestions.value) {
      await createQuestion(question)
    }
    generationStatus.value = `已入库 ${generatedQuestions.value.length} 道生成题。`
  } catch (error) {
    generationStatus.value = error instanceof Error ? error.message : '入库失败，请检查本地后端是否启动。'
  }
}
</script>
