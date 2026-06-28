<template>
  <div v-if="currentUser" class="current-account" aria-label="当前登录账户">
    <span class="account-name">{{ currentUser.displayName }}</span>
    <small v-if="currentUser.username">@{{ currentUser.username }}</small>
    <strong>{{ roleLabel }}</strong>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { getCurrentUser } from '../session'

const currentUser = getCurrentUser()
const roleLabel = computed(() => {
  if (currentUser?.role === 'ADMIN') {
    return '管理员'
  }
  return '普通用户'
})
</script>
