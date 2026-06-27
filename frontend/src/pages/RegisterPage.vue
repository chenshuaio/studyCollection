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
          <input v-model="form.username" autocomplete="username" placeholder="请输入账号" />
        </label>
        <label>
          昵称
          <input v-model="form.displayName" autocomplete="name" placeholder="例如 Java 学习者" />
        </label>
        <label>
          密码
          <input v-model="form.password" autocomplete="new-password" type="password" placeholder="至少 6 位" />
        </label>
        <p v-if="errorMessage" class="form-message">{{ errorMessage }}</p>
        <button type="submit" :disabled="submitting">{{ submitting ? '注册中' : '注册并进入' }}</button>
      </form>
      <RouterLink to="/">返回登录</RouterLink>
    </section>
  </main>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { RouterLink, useRouter } from 'vue-router'
import { register } from '../api'

const router = useRouter()
const submitting = ref(false)
const errorMessage = ref('')
const form = reactive({
  username: 'java-user',
  displayName: 'Java 学习者',
  password: 'user123'
})

async function goDashboard() {
  submitting.value = true
  errorMessage.value = ''
  try {
    await register(form)
    router.push('/dashboard')
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '注册失败，请检查本地后端是否启动。'
  } finally {
    submitting.value = false
  }
}
</script>
