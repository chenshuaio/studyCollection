<template>
  <main class="dashboard-shell">
    <aside class="dashboard-sidebar" aria-label="主导航">
      <p class="brand">StudyCollection</p>
      <nav>
        <RouterLink to="/dashboard">学习控制台</RouterLink>
        <RouterLink to="/questions">题库管理</RouterLink>
        <RouterLink to="/knowledge-points">知识点管理</RouterLink>
        <RouterLink to="/exam-rules/manage">考试规则</RouterLink>
        <RouterLink to="/ai-settings">AI 设置</RouterLink>
        <RouterLink to="/feedback">反馈审核</RouterLink>
        <RouterLink to="/users">用户管理</RouterLink>
        <RouterLink to="/import">题目导入</RouterLink>
        <RouterLink to="/practice">练习中心</RouterLink>
        <RouterLink to="/exams">考试中心</RouterLink>
        <RouterLink to="/mistakes">错题本</RouterLink>
        <RouterLink to="/reports">学习报告</RouterLink>
      </nav>
    </aside>

    <section class="dashboard-main">
      <header class="dashboard-header">
        <div>
          <p class="eyebrow">在线分析管理</p>
          <h1>AI 设置</h1>
        </div>
        <div class="header-actions">
          <CurrentAccount />
          <LogoutButton />
        </div>
      </header>

      <section class="metric-grid" aria-label="AI 配置概览">
        <article>
          <span>接口协议</span>
          <strong>{{ providerLabel }}</strong>
          <small>Chat Completions</small>
        </article>
        <article>
          <span>API 密钥</span>
          <strong>{{ settings?.apiKeyConfigured ? '密钥已配置' : '密钥未配置' }}</strong>
          <small>仅从环境变量读取</small>
        </article>
        <article>
          <span>最后更新</span>
          <strong>{{ settings?.updatedAt ? formatDateTime(settings.updatedAt) : '使用默认值' }}</strong>
          <small>敏感信息不会写入数据库</small>
        </article>
      </section>

      <section class="ai-settings-layout">
        <article class="workspace-panel">
          <div class="panel-header">
            <div>
              <p class="eyebrow">非敏感配置</p>
              <h2>在线模型</h2>
            </div>
          </div>

          <form class="question-form" @submit.prevent="saveSettings">
            <label>
              在线模型端点
              <input
                v-model.trim="form.endpoint"
                aria-label="在线模型端点"
                autocomplete="url"
                placeholder="https://api.example.com/v1/chat/completions"
              />
            </label>
            <label>
              在线模型名称
              <input
                v-model.trim="form.modelName"
                aria-label="在线模型名称"
                autocomplete="off"
                placeholder="例如 qwen-plus"
              />
            </label>
            <div class="ai-form-actions">
              <button type="submit" :disabled="saving || testing">
                {{ saving ? '保存中...' : '保存设置' }}
              </button>
              <button
                class="secondary-button"
                type="button"
                aria-label="测试在线模型连接"
                :disabled="saving || testing"
                @click="runConnectionTest"
              >
                {{ testing ? '测试中...' : '测试连接' }}
              </button>
            </div>
          </form>
          <p v-if="statusMessage" class="form-message" aria-live="polite">{{ statusMessage }}</p>
        </article>

        <aside class="workspace-panel ai-key-panel">
          <p class="eyebrow">密钥注入</p>
          <h2>{{ settings?.apiKeyConfigured ? '环境密钥可用' : '等待环境密钥' }}</h2>
          <p>
            后端只读取 <code>STUDY_COLLECTION_AI_API_KEY</code>，页面不会接收、显示或保存密钥。
          </p>
        </aside>
      </section>

      <section class="table-panel ai-audit-panel">
        <div class="panel-header">
          <div>
            <p class="eyebrow">最近 50 次</p>
            <h2>调用审计</h2>
          </div>
          <button class="secondary-button" type="button" :disabled="loading" @click="loadData">
            {{ loading ? '刷新中...' : '刷新' }}
          </button>
        </div>
        <div class="table-scroll">
          <table>
            <thead>
              <tr>
                <th>时间</th>
                <th>用途</th>
                <th>状态</th>
                <th>模型</th>
                <th>耗时</th>
                <th>失败原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="audit in audits" :key="audit.id">
                <td>{{ formatDateTime(audit.createdAt) }}</td>
                <td>{{ purposeLabel(audit.purpose) }}</td>
                <td>
                  <span :class="['status-pill', audit.status === 'SUCCESS' ? 'status-success' : 'status-warning']">
                    {{ statusLabel(audit.status) }}
                  </span>
                </td>
                <td>{{ audit.modelName || '未配置' }}</td>
                <td>{{ audit.durationMs }} ms</td>
                <td>{{ audit.failureReason || '无' }}</td>
              </tr>
              <tr v-if="!audits.length">
                <td colspan="6">暂无在线模型调用记录。</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </section>
  </main>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import CurrentAccount from '../components/CurrentAccount.vue'
import LogoutButton from '../components/LogoutButton.vue'
import {
  getAiSettings,
  listAiAudits,
  testAiConnection,
  updateAiSettings,
  type AiCallAudit,
  type AiSettings
} from '../api'

const settings = ref<AiSettings | null>(null)
const audits = ref<AiCallAudit[]>([])
const loading = ref(false)
const saving = ref(false)
const testing = ref(false)
const statusMessage = ref('')
const form = reactive({ endpoint: '', modelName: '' })

const providerLabel = computed(() => (
  settings.value?.provider === 'OPENAI_COMPATIBLE' ? 'OpenAI 兼容' : settings.value?.provider || 'OpenAI 兼容'
))

onMounted(loadData)

async function loadData() {
  loading.value = true
  statusMessage.value = ''
  try {
    const [nextSettings, nextAudits] = await Promise.all([getAiSettings(), listAiAudits(50)])
    applySettings(nextSettings)
    audits.value = Array.isArray(nextAudits) ? nextAudits : []
  } catch (error) {
    statusMessage.value = messageFrom(error, 'AI 设置加载失败，请稍后重试。')
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  if (!form.endpoint || !form.modelName) {
    statusMessage.value = '请填写在线模型端点和模型名称。'
    return
  }
  saving.value = true
  statusMessage.value = ''
  try {
    const saved = await updateAiSettings({ endpoint: form.endpoint, modelName: form.modelName })
    applySettings(saved)
    statusMessage.value = 'AI 设置已保存。'
  } catch (error) {
    statusMessage.value = messageFrom(error, 'AI 设置保存失败，请稍后重试。')
  } finally {
    saving.value = false
  }
}

async function runConnectionTest() {
  testing.value = true
  statusMessage.value = ''
  try {
    const result = await testAiConnection()
    statusMessage.value = result.message
    audits.value = await listAiAudits(50)
  } catch (error) {
    statusMessage.value = messageFrom(error, '连接测试失败，请检查当前设置。')
  } finally {
    testing.value = false
  }
}

function applySettings(nextSettings: AiSettings) {
  settings.value = nextSettings
  form.endpoint = nextSettings.endpoint ?? ''
  form.modelName = nextSettings.modelName ?? ''
}

function purposeLabel(purpose: string) {
  if (purpose === 'LEARNING_REPORT') return '学习报告'
  if (purpose === 'CONFIG_TEST') return '配置测试'
  return purpose || '未知用途'
}

function statusLabel(status: string) {
  return status === 'SUCCESS' ? '成功' : '已回退'
}

function formatDateTime(value: string) {
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function messageFrom(error: unknown, fallback: string) {
  return error instanceof Error && error.message ? error.message : fallback
}
</script>

<style scoped>
.ai-settings-layout {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(260px, 0.8fr);
  gap: 16px;
  margin-bottom: 16px;
}

.ai-form-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.ai-form-actions button {
  flex: 1 1 180px;
}

.ai-key-panel code {
  overflow-wrap: anywhere;
}

.ai-audit-panel {
  min-width: 0;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 4px 9px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 700;
  white-space: nowrap;
}

.status-success {
  background: #e7f6ee;
  color: #17633a;
}

.status-warning {
  background: #fff3d6;
  color: #805800;
}

@media (max-width: 800px) {
  .ai-settings-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .ai-form-actions {
    display: grid;
    grid-template-columns: 1fr;
  }

  .ai-form-actions button {
    width: 100%;
  }
}
</style>
