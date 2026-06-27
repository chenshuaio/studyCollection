<template>
  <main class="login-shell">
    <section class="login-hero">
      <p class="eyebrow">StudyCollection</p>
      <h1>Java 学习题库平台</h1>
      <p class="hero-copy">从入门到精通，汇总题库、智能组卷、错题报告和薄弱点强化训练。</p>
      <div class="feature-grid">
        <span>题库导入</span>
        <span>自动出题</span>
        <span>错题报告</span>
      </div>
    </section>
    <section class="login-card" aria-label="登录表单">
      <h2>欢迎回来</h2>
      <p>登录后继续你的 Java 训练计划</p>
      <form @submit.prevent="goDashboard">
        <label>
          账号
          <input autocomplete="username" placeholder="admin 或 user" />
        </label>
        <label>
          密码
          <input autocomplete="current-password" type="password" placeholder="请输入密码" />
        </label>
        <button type="submit">登录</button>
      </form>
      <a href="/register">创建学习账号</a>
    </section>
  </main>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

function goDashboard() {
  router.push('/dashboard')
}
</script>
