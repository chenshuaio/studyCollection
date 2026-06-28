import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PracticePage from './PracticePage.vue'
import { recordMistake, submitPractice } from '../api'

vi.mock('../api', () => ({
  submitPractice: vi.fn(),
  submitQuestionFeedback: vi.fn(),
  recordMistake: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('PracticePage', () => {
  it('renders practice answering and question feedback workflow', () => {
    const wrapper = mount(PracticePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('练习中心')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('提交答案')
    expect(wrapper.text()).toContain('答案解析')
    expect(wrapper.text()).toContain('得分')
    expect(wrapper.text()).toContain('反馈题目问题')
    expect(wrapper.find('textarea[aria-label="题目反馈内容"]').exists()).toBe(true)
  })

  it('records a mistake when the submitted answer is wrong', async () => {
    vi.mocked(submitPractice).mockResolvedValue({
      score: 0,
      totalScore: 10,
      items: [
        {
          questionId: 1,
          submittedAnswer: 'B',
          correctAnswer: 'A',
          correct: false,
          score: 0,
          analysis: 'HashMap 默认负载因子是 0.75'
        }
      ]
    })
    vi.mocked(recordMistake).mockResolvedValue({
      userId: 7,
      questionId: 1,
      questionTitle: 'HashMap 默认负载因子是多少？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })

    const wrapper = mount(PracticePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await wrapper.find('input[value="B"]').setValue()
    await wrapper.findAll('button')[1].trigger('click')
    await flushPromises()

    expect(recordMistake).toHaveBeenCalledWith({
      userId: 7,
      questionId: 1,
      questionTitle: expect.stringContaining('HashMap'),
      knowledgePoint: expect.any(String),
      status: 'PENDING'
    })
  })
})
