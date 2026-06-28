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
        <RouterLink to="/feedback">反馈审核</RouterLink>
        <a href="#report">AI 分析</a>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">本地学习工作台</p>
          <h1>学习控制台</h1>
        </div>
        <div class="header-actions">
          <RouterLink class="button-link" to="/practice">新建练习</RouterLink>
          <LogoutButton />
        </div>
      </header>

      <section id="overview" class="metric-grid" aria-label="学习概览">
        <article>
          <span>今日练习</span>
          <strong>24 题</strong>
          <small>正确率 82%</small>
        </article>
        <article>
          <span>待处理错题</span>
          <strong>6 题</strong>
          <small>优先复习集合框架</small>
        </article>
        <article>
          <span>管理员反馈</span>
          <strong>3 条</strong>
          <small>答案错误待审核</small>
        </article>
      </section>

      <section class="workspace-grid">
        <article id="import" class="workspace-panel">
          <h2>题库导入</h2>
          <p>支持 JSON、CSV、XLSX、TXT、MD、PDF、DOCX 文件预览后入库。</p>
          <RouterLink class="button-link" to="/import">导入题目</RouterLink>
        </article>
        <article id="exam" class="workspace-panel">
          <h2>自定义组卷</h2>
          <p>按知识点、难度、题型筛选题目，也可以手动组合生成考试卷。</p>
          <RouterLink class="button-link" to="/exams">创建考试卷</RouterLink>
        </article>
        <article id="feedback" class="workspace-panel">
          <h2>错题反馈</h2>
          <p>发现答案或解析错误时提交反馈，管理员审核后修订题库。</p>
          <RouterLink class="button-link" to="/feedback">查看反馈</RouterLink>
        </article>
        <article id="report" class="workspace-panel">
          <h2>AI 分析</h2>
          <p>在线模型开启时生成学习建议；关闭时使用规则分析薄弱点。</p>
          <button type="button">生成报告</button>
        </article>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink } from 'vue-router'
import LogoutButton from '../components/LogoutButton.vue'
</script>
