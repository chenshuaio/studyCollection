<template>
  <button class="logout-button" type="button" aria-label="退出登录" @click="logout">退出登录</button>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { clearCurrentUser } from '../session'

const router = useRouter()

function logout() {
  clearCurrentUser()
  router.push('/')
}
</script>
