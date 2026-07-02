<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/knowledge-points">知识点管理</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">管理员工作台</p>
          <h1>知识点管理</h1>
        </div>
        <div class="header-actions">
          <button type="button" @click="loadKnowledgePoints">刷新知识点</button>
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="question-layout">
        <article class="table-panel">
          <div class="panel-header">
            <h2>知识点分类</h2>
            <span class="panel-count">{{ knowledgePoints.length }} 个</span>
          </div>
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>说明</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="point in knowledgePoints" :key="point.id">
                <td>{{ point.id }}</td>
                <td>{{ point.name }}</td>
                <td>{{ point.description || '暂无说明' }}</td>
                <td>{{ point.enabled ? '启用中' : '已停用' }}</td>
                <td>
                  <button
                    v-if="point.enabled"
                    type="button"
                    aria-label="停用知识点"
                    @click="disablePoint(point.id)"
                  >
                    停用
                  </button>
                  <span v-else>已停用</span>
                </td>
              </tr>
              <tr v-if="knowledgePoints.length === 0">
                <td colspan="5">暂无知识点分类。</td>
              </tr>
            </tbody>
          </table>
          <p v-if="statusMessage" class="form-message">{{ statusMessage }}</p>
        </article>

        <aside class="workspace-panel">
          <h2>新增知识点</h2>
          <form class="question-form" @submit.prevent="saveKnowledgePoint">
            <label>
              名称
              <input v-model.trim="draft.name" aria-label="知识点名称" placeholder="例如：JVM" />
            </label>
            <label>
              说明
              <textarea
                v-model.trim="draft.description"
                aria-label="知识点说明"
                placeholder="描述这个分类覆盖的学习内容"
              ></textarea>
            </label>
            <button type="submit">创建知识点</button>
          </form>
        </aside>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import {
  createKnowledgePoint,
  disableKnowledgePoint,
  listKnowledgePoints,
  type KnowledgePoint
} from '../api'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'

const knowledgePoints = ref<KnowledgePoint[]>([])
const statusMessage = ref('')
const draft = reactive({
  name: '',
  description: ''
})

onMounted(loadKnowledgePoints)

async function loadKnowledgePoints() {
  statusMessage.value = ''
  try {
    knowledgePoints.value = await listKnowledgePoints()
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '加载知识点失败，请检查本地后端是否启动。'
  }
}

async function saveKnowledgePoint() {
  if (!draft.name) {
    statusMessage.value = '请填写知识点名称。'
    return
  }
  statusMessage.value = ''
  try {
    const saved = await createKnowledgePoint({ name: draft.name, description: draft.description })
    knowledgePoints.value = [...knowledgePoints.value.filter((point) => point.id !== saved.id), saved]
    draft.name = ''
    draft.description = ''
    statusMessage.value = '知识点已创建。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '创建知识点失败。'
  }
}

async function disablePoint(id: number) {
  statusMessage.value = ''
  try {
    const disabled = await disableKnowledgePoint(id)
    knowledgePoints.value = knowledgePoints.value.map((point) => (point.id === id ? disabled : point))
    statusMessage.value = '知识点已停用。'
  } catch (error) {
    statusMessage.value = error instanceof Error ? error.message : '停用知识点失败。'
  }
}
</script>
