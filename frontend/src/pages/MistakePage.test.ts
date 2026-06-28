import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import MistakePage from './MistakePage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('MistakePage', () => {
  it('renders user mistake book workflow', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [
            {
              userId: 7,
              questionId: 1,
              questionTitle: 'HashMap 默认负载因子是多少？',
              knowledgePoint: '集合框架',
              status: 'PENDING'
            }
          ]
        })
      })
    )

    const wrapper = mount(MistakePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('集合框架')
    expect(wrapper.text()).toContain('重新练习')

    vi.unstubAllGlobals()
  })
})
