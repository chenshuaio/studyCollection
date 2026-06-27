<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <a href="#new-question">新增题目</a>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">题库工作台</p>
          <h1>题库管理</h1>
        </div>
        <RouterLink class="button-link" to="/import">导入文件</RouterLink>
      </header>

      <section class="filter-bar" aria-label="题目筛选">
        <label>
          知识点
          <select>
            <option>集合框架</option>
            <option>JVM</option>
          </select>
        </label>
        <label>
          难度
          <select>
            <option>INTERMEDIATE</option>
            <option>BEGINNER</option>
            <option>ADVANCED</option>
          </select>
        </label>
        <label>
          题型
          <select>
            <option>SINGLE_CHOICE</option>
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
              </tr>
            </thead>
            <tbody>
              <tr v-for="question in questions" :key="question.id">
                <td>{{ question.title }}</td>
                <td>{{ question.knowledgePoint }}</td>
                <td>{{ question.difficulty }}</td>
                <td>{{ question.type }}</td>
                <td>{{ question.answer }}</td>
              </tr>
            </tbody>
          </table>
        </article>

        <aside id="new-question" class="workspace-panel">
          <h2>新增题目</h2>
          <form class="question-form">
            <label>
              题干
              <textarea>HashMap 默认负载因子是多少？</textarea>
            </label>
            <label>
              标准答案
              <input value="0.75" />
            </label>
            <label>
              解析
              <textarea>HashMap 默认负载因子是 0.75。</textarea>
            </label>
            <button type="button">保存题目</button>
          </form>
        </aside>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'

const questions = [
  {
    id: 1,
    title: 'HashMap 默认负载因子是多少？',
    knowledgePoint: '集合框架',
    difficulty: 'INTERMEDIATE',
    type: 'SINGLE_CHOICE',
    answer: '0.75'
  },
  {
    id: 2,
    title: 'Java 中 String 为什么不可变？',
    knowledgePoint: 'Java 基础',
    difficulty: 'BEGINNER',
    type: 'SHORT_ANSWER',
    answer: '安全性、缓存和哈希稳定性'
  }
]
</script>
