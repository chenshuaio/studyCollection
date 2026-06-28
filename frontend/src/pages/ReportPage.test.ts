import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ReportPage from './ReportPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ReportPage', () => {
  it('renders report workflow and selectable analysis mode', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            weakestKnowledgePoint: 'JVM',
            recommendation: '建议优先强化 JVM',
            adviceSource: 'RULES',
            adviceContent: '规则分析建议：请针对 JVM 继续练习。'
          }
        })
      })
    )

    const wrapper = mount(ReportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('学习报告')
    expect(wrapper.text()).toContain('规则分析')
    expect(wrapper.text()).toContain('在线模型')
    expect(wrapper.text()).toContain('生成报告')

    await wrapper.find('form').trigger('submit')
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('JVM')
    expect(wrapper.text()).toContain('RULES')
    expect(wrapper.text()).toContain('建议优先强化 JVM')

    vi.unstubAllGlobals()
  })
})
