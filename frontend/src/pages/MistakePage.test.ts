import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
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

    await flushPromises()

    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('集合框架')
    expect(wrapper.text()).toContain('重新练习')

    vi.unstubAllGlobals()
  })

  it('marks pending mistakes as mastered and refreshes the list', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
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
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            userId: 7,
            questionId: 1,
            questionTitle: 'HashMap 默认负载因子是多少？',
            knowledgePoint: '集合框架',
            status: 'MASTERED'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [
            {
              userId: 7,
              questionId: 1,
              questionTitle: 'HashMap 默认负载因子是多少？',
              knowledgePoint: '集合框架',
              status: 'MASTERED'
            }
          ]
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(MistakePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    await wrapper.find('button[aria-label="标记已掌握"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/mistakes/status',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ userId: 7, questionId: 1, status: 'MASTERED' })
      })
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes?userId=7', expect.objectContaining({ method: 'GET' }))
    expect(wrapper.text()).toContain('已掌握')

    vi.unstubAllGlobals()
  })
})
