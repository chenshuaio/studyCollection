import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import AiSettingsPage from './AiSettingsPage.vue'

const mocks = vi.hoisted(() => ({
  getAiSettings: vi.fn(),
  updateAiSettings: vi.fn(),
  testAiConnection: vi.fn(),
  listAiAudits: vi.fn()
}))

vi.mock('../api', () => ({
  getAiSettings: mocks.getAiSettings,
  updateAiSettings: mocks.updateAiSettings,
  testAiConnection: mocks.testAiConnection,
  listAiAudits: mocks.listAiAudits
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :data-to="to"><slot /></a>'
}

describe('AiSettingsPage', () => {
  beforeEach(() => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'admin-token', userId: 1, username: 'admin', role: 'ADMIN', displayName: '系统管理员'
    }))
    mocks.getAiSettings.mockReset()
    mocks.updateAiSettings.mockReset()
    mocks.testAiConnection.mockReset()
    mocks.listAiAudits.mockReset()
    mocks.getAiSettings.mockResolvedValue({
      provider: 'OPENAI_COMPATIBLE',
      endpoint: 'https://api.example/v1/chat/completions',
      modelName: 'qwen-plus',
      apiKeyConfigured: true,
      updatedBy: 1,
      updatedAt: '2026-07-12T08:00:00Z'
    })
    mocks.updateAiSettings.mockResolvedValue({
      provider: 'OPENAI_COMPATIBLE',
      endpoint: 'https://new.example/v1/chat/completions',
      modelName: 'deepseek-chat',
      apiKeyConfigured: true,
      updatedBy: 1,
      updatedAt: '2026-07-12T08:10:00Z'
    })
    mocks.testAiConnection.mockResolvedValue({
      success: true,
      source: 'ONLINE_MODEL',
      message: '在线模型连接成功。'
    })
    mocks.listAiAudits.mockResolvedValue([
      {
        id: 3,
        userId: 1,
        purpose: 'CONFIG_TEST',
        provider: 'OPENAI_COMPATIBLE',
        modelName: 'qwen-plus',
        status: 'SUCCESS',
        failureReason: null,
        durationMs: 128,
        createdAt: '2026-07-12T08:01:00Z'
      }
    ])
  })

  it('loads non-sensitive settings and recent audits', async () => {
    const wrapper = mount(AiSettingsPage, {
      global: { stubs: { RouterLink: routerLinkStub, CurrentAccount: true, LogoutButton: true } }
    })

    await flushPromises()

    expect(wrapper.text()).toContain('AI 设置')
    expect(wrapper.text()).toContain('OpenAI 兼容')
    expect(wrapper.text()).toContain('密钥已配置')
    expect(wrapper.text()).toContain('配置测试')
    expect(wrapper.text()).toContain('成功')
    expect(wrapper.text()).toContain('128 ms')
    expect(wrapper.find('input[type="password"]').exists()).toBe(false)
    expect(wrapper.html()).not.toContain('configured-secret')
  })

  it('saves settings and tests the current connection', async () => {
    const wrapper = mount(AiSettingsPage, {
      global: { stubs: { RouterLink: routerLinkStub, CurrentAccount: true, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.get('input[aria-label="在线模型端点"]').setValue('https://new.example/v1/chat/completions')
    await wrapper.get('input[aria-label="在线模型名称"]').setValue('deepseek-chat')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(mocks.updateAiSettings).toHaveBeenCalledWith({
      endpoint: 'https://new.example/v1/chat/completions',
      modelName: 'deepseek-chat'
    })
    expect(wrapper.text()).toContain('AI 设置已保存')

    await wrapper.get('button[aria-label="测试在线模型连接"]').trigger('click')
    await flushPromises()

    expect(mocks.testAiConnection).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('在线模型连接成功')
  })

  it('shows the real fallback message when connection testing fails', async () => {
    mocks.testAiConnection.mockResolvedValue({
      success: false,
      source: 'RULES',
      message: '在线模型暂不可用，已自动切换为规则分析。'
    })
    const wrapper = mount(AiSettingsPage, {
      global: { stubs: { RouterLink: routerLinkStub, CurrentAccount: true, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.get('button[aria-label="测试在线模型连接"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('在线模型暂不可用')
  })
})
