<template>
  <main class="login-shell">
    <section class="login-hero">
      <p class="eyebrow">StudyCollection</p>
      <h1>创建学习账号</h1>
      <p class="hero-copy">建立你的个人 Java 题库、错题本、考试卷和学习报告。</p>
      <div class="feature-grid">
        <span>个人题库</span>
        <span>错题追踪</span>
        <span>报告分析</span>
      </div>
    </section>
    <section class="login-card" aria-label="注册表单">
      <h2>开始学习</h2>
      <p>本地模式会先创建学习账号，后续可接入 MySQL 持久化。</p>
      <form @submit.prevent="goDashboard">
        <label>
          账号
          <input autocomplete="username" placeholder="请输入账号" />
        </label>
        <label>
          昵称
          <input autocomplete="name" placeholder="例如 Java 学习者" />
        </label>
        <label>
          密码
          <input autocomplete="new-password" type="password" placeholder="至少 6 位" />
        </label>
        <button type="submit">注册并进入</button>
      </form>
      <RouterLink to="/">返回登录</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { RouterLink, useRouter } from 'vue-router'

const router = useRouter()

function goDashboard() {
  router.push('/dashboard')
}
</script>
