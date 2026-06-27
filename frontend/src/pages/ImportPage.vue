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
          <p>当前支持 Markdown/TXT 风格的结构化文本，后续接入 PDF、DOCX、XLSX 解析。</p>
          <textarea class="import-editor" v-model="rawContent" aria-label="粘贴题目"></textarea>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
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
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { RouterLink } from 'vue-router'
import { previewImport, type PreviewQuestion } from '../api'

const rawContent = ref(`## 单选题
题目: Java 中 int 默认值是多少？
A. 0
B. null
答案: A
知识点: Java 基础
难度: BEGINNER`)
const statusMessage = ref('')
const previewQuestions = ref<PreviewQuestion[]>([
  {
    title: 'Java 中 int 默认值是多少？',
    answer: 'A',
    knowledgePoint: 'Java 基础',
    difficulty: 'BEGINNER'
  }
])

async function generatePreview() {
  statusMessage.value = ''
  try {
    previewQuestions.value = await previewImport(rawContent.value)
    statusMessage.value = '预览已由本地后端生成。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '生成预览失败，请检查本地后端是否启动。'
  }
}
</script>
