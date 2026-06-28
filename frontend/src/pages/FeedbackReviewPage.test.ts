import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import FeedbackReviewPage from './FeedbackReviewPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('FeedbackReviewPage', () => {
  it('renders pending feedback review workflow', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({
        code: 'OK',
        data: [
          {
            id: 1,
            userId: 7,
            questionId: 101,
            type: 'ANSWER_ERROR',
            content: '标准答案应为 B',
            status: 'PENDING'
          }
        ]
      })
    }))

    const wrapper = mount(FeedbackReviewPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('反馈审核')
    expect(wrapper.text()).toContain('待处理反馈')
    expect(wrapper.text()).toContain('标准答案应为 B')
    expect(wrapper.find('textarea[aria-label="修订说明"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('采纳并记录修订')

    vi.unstubAllGlobals()
  })
})
