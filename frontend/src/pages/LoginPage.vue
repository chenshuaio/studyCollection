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
          <input v-model="form.username" autocomplete="username" placeholder="admin 或 user" />
        </label>
        <label>
          密码
          <input v-model="form.password" autocomplete="current-password" type="password" placeholder="请输入密码" />
        </label>
        <p v-if="errorMessage" class="form-message">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '登录中' : '登录' }}</button>
      </form>
      <RouterLink to="/register">创建学习账号</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { login } from '../api'

const router = useRouter()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: 'user',
  password: 'user123'
})

async function goDashboard() {
  submitting.value = true
  errorMessage.value = ''
  try {
    const result = await login(form)
    window.localStorage.setItem('studyCollectionUser', JSON.stringify(result))
    router.push('/dashboard')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败，请检查本地后端是否启动。'
  } finally {
    submitting.value = false
  }
}
</script>
