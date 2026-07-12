import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import FeedbackReviewPage from './FeedbackReviewPage.vue'
import {
  acceptQuestionFeedbackGroup,
  listPendingFeedbackGroups,
  listQuestionRevisions,
  markFeedbackNeedsReview,
  rejectQuestionFeedback,
  type QuestionFeedbackGroup
} from '../api'

vi.mock('../api', () => ({
  acceptQuestionFeedbackGroup: vi.fn(),
  listPendingFeedbackGroups: vi.fn(),
  listQuestionRevisions: vi.fn(),
  markFeedbackNeedsReview: vi.fn(),
  rejectQuestionFeedback: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

const group: QuestionFeedbackGroup = {
  questionId: 101,
  questionTitle: 'Java 中 int 成员变量默认值是多少？',
  questionType: 'SINGLE_CHOICE',
  difficulty: 'BEGINNER',
  knowledgePoint: 'Java 基础',
  currentAnswer: 'A',
  currentAnalysis: '旧解析',
  questionSource: 'LOCAL_UPLOAD',
  type: 'ANSWER_ERROR',
  feedbackCount: 2,
  latestAt: '2026-07-12T09:00:00Z',
  items: [
    {
      id: 2,
      userId: 8,
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '答案 A 不正确',
      submittedAnswer: 'B',
      sourceContext: 'EXAM',
      sourceReference: 'exam-12',
      status: 'NEEDS_REVIEW',
      createdAt: '2026-07-12T09:00:00Z',
      reviewNote: '交给教研复核'
    },
    {
      id: 1,
      userId: 7,
      questionId: 101,
      type: 'ANSWER_ERROR',
      content: '标准答案应为 B',
      submittedAnswer: 'B',
      sourceContext: 'PRACTICE',
      sourceReference: 'practice-91',
      status: 'PENDING',
      createdAt: '2026-07-10T08:00:00Z'
    }
  ]
}

describe('FeedbackReviewPage', () => {
  it('reviews grouped feedback with answer context and complete revision history', async () => {
    vi.mocked(listPendingFeedbackGroups).mockResolvedValue([group])
    vi.mocked(listQuestionRevisions).mockResolvedValue([])
    vi.mocked(acceptQuestionFeedbackGroup).mockResolvedValue({
      id: 11,
      questionId: 101,
      feedbackId: 2,
      relatedFeedbackIds: [2, 1],
      adminUserId: 1,
      changeSummary: '答案从 A 修改为 B',
      reviewNote: '用户反馈属实',
      beforeQuestion: { id: 101, title: group.questionTitle, type: group.questionType, difficulty: group.difficulty, knowledgePoint: group.knowledgePoint, answer: 'A', analysis: '旧解析', source: 'LOCAL_UPLOAD' },
      afterQuestion: { id: 101, title: group.questionTitle, type: group.questionType, difficulty: group.difficulty, knowledgePoint: group.knowledgePoint, answer: 'B', analysis: '新解析', source: 'LOCAL_UPLOAD' },
      scoringAffected: true,
      revisedAt: '2026-07-12T10:00:00Z'
    })
    vi.mocked(rejectQuestionFeedback).mockResolvedValue({ ...group.items[0], status: 'REJECTED' })
    vi.mocked(markFeedbackNeedsReview).mockResolvedValue({ ...group.items[0], status: 'NEEDS_REVIEW' })

    const wrapper = mount(FeedbackReviewPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          CurrentAccount: true,
          LogoutButton: true
        }
      }
    })

    await flushPromises()

    expect(wrapper.text()).toContain('反馈审核')
    expect(wrapper.text()).toContain('2 条重复反馈')
    expect(wrapper.text()).toContain('Java 中 int 成员变量默认值是多少？')
    expect(wrapper.text()).toContain('当前答案A')
    expect(wrapper.text()).toContain('用户答案B')
    expect(wrapper.text()).toContain('考试')
    expect(wrapper.text()).toContain('exam-12')
    expect(wrapper.text()).toContain('暂无修订历史')

    await wrapper.find('textarea[aria-label="新标准答案"]').setValue('B')
    await wrapper.find('textarea[aria-label="新题目解析"]').setValue('Java 基本类型 int 的默认值是 0。')
    await wrapper.find('button[data-action="accept-group"]').trigger('click')
    await flushPromises()

    expect(acceptQuestionFeedbackGroup).toHaveBeenCalledWith(expect.objectContaining({
      feedbackIds: [2, 1],
      correctedAnswer: 'B',
      correctedAnalysis: 'Java 基本类型 int 的默认值是 0。'
    }))
    expect(wrapper.text()).toContain('修订历史')
    expect(wrapper.text()).toContain('答案从 A 修改为 B')
  })
})
