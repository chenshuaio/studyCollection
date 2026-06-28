import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import FeedbackReviewPage from './FeedbackReviewPage.vue'
import { acceptQuestionFeedback, listPendingFeedback, markFeedbackNeedsReview, rejectQuestionFeedback } from '../api'

vi.mock('../api', () => ({
  acceptQuestionFeedback: vi.fn(),
  listPendingFeedback: vi.fn(),
  markFeedbackNeedsReview: vi.fn(),
  rejectQuestionFeedback: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('FeedbackReviewPage', () => {
  it('renders and handles feedback review actions', async () => {
    vi.mocked(listPendingFeedback).mockResolvedValue([
      {
        id: 1,
        userId: 7,
        questionId: 101,
        type: 'ANSWER_ERROR',
        content: '标准答案应为 B',
        status: 'PENDING'
      }
    ])
    vi.mocked(acceptQuestionFeedback).mockResolvedValue({
      id: 1,
      questionId: 101,
      feedbackId: 1,
      adminUserId: 1,
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '用户反馈属实'
    })
    vi.mocked(rejectQuestionFeedback).mockResolvedValue({
      id: 1,
      userId: 7,
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '标准答案应为 B',
      status: 'REJECTED'
    })
    vi.mocked(markFeedbackNeedsReview).mockResolvedValue({
      id: 1,
      userId: 7,
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '标准答案应为 B',
      status: 'NEEDS_REVIEW'
    })

    const wrapper = mount(FeedbackReviewPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()

    expect(wrapper.text()).toContain('反馈审核')
    expect(wrapper.text()).toContain('待处理反馈')
    expect(wrapper.text()).toContain('标准答案应为 B')
    expect(wrapper.find('textarea[aria-label="修订说明"]').exists()).toBe(true)
    expect(wrapper.text()).toContain('采纳并记录修订')
    expect(wrapper.text()).toContain('驳回反馈')
    expect(wrapper.text()).toContain('标记待复核')

    await wrapper.findAll('button').find((button) => button.text() === '采纳并记录修订')?.trigger('click')
    await wrapper.findAll('button').find((button) => button.text() === '驳回反馈')?.trigger('click')
    await wrapper.findAll('button').find((button) => button.text() === '标记待复核')?.trigger('click')
    await flushPromises()

    expect(acceptQuestionFeedback).toHaveBeenCalledWith(1, expect.objectContaining({ adminUserId: 1 }))
    expect(rejectQuestionFeedback).toHaveBeenCalledWith(1, expect.objectContaining({ adminUserId: 1 }))
    expect(markFeedbackNeedsReview).toHaveBeenCalledWith(1, expect.objectContaining({ adminUserId: 1 }))
  })
})
