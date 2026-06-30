import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PracticePage from './PracticePage.vue'
import { recordMistake, searchQuestions, submitUserPractice } from '../api'

vi.mock('../api', () => ({
  searchQuestions: vi.fn(),
  submitUserPractice: vi.fn(),
  submitQuestionFeedback: vi.fn(),
  recordMistake: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('PracticePage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.mocked(searchQuestions).mockReset()
    vi.mocked(submitUserPractice).mockReset()
    vi.mocked(recordMistake).mockReset()
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 88,
        title: 'JVM 栈内存主要保存什么？',
        type: 'SHORT_ANSWER',
        difficulty: 'BEGINNER',
        knowledgePoint: 'JVM',
        answer: '栈帧',
        analysis: '虚拟机栈保存方法调用的栈帧。'
      }
    ])
  })

  it('renders practice answering and question feedback workflow', async () => {
    const wrapper = mount(PracticePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('练习中心')
    expect(searchQuestions).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('JVM 栈内存主要保存什么？')
    expect(wrapper.text()).toContain('提交答案')
    expect(wrapper.text()).toContain('答案解析')
    expect(wrapper.text()).toContain('得分')
    expect(wrapper.text()).toContain('反馈题目问题')
    expect(wrapper.find('textarea[aria-label="题目反馈内容"]').exists()).toBe(true)
  })

  it('records a mistake when the submitted answer is wrong', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ userId: 7, role: 'USER', displayName: 'Alice' }))
    vi.mocked(submitUserPractice).mockResolvedValue({
      score: 0,
      totalScore: 10,
      items: [
        {
          questionId: 88,
          submittedAnswer: '堆对象',
          correctAnswer: '栈帧',
          correct: false,
          score: 0,
          analysis: '虚拟机栈保存方法调用的栈帧。'
        }
      ]
    })
    vi.mocked(recordMistake).mockResolvedValue({
      userId: 7,
      questionId: 88,
      questionTitle: 'JVM 栈内存主要保存什么？',
      knowledgePoint: 'JVM',
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
    await flushPromises()

    await wrapper.find('input[aria-label="练习答案"]').setValue('堆对象')
    await wrapper.findAll('button')[1].trigger('click')
    await flushPromises()

    expect(submitUserPractice).toHaveBeenCalledWith(7, [{
      questionId: 88,
      answer: '堆对象',
      correctAnswer: '栈帧',
      analysis: '虚拟机栈保存方法调用的栈帧。'
    }])
    expect(recordMistake).toHaveBeenCalledWith({
      userId: 7,
      questionId: 88,
      questionTitle: 'JVM 栈内存主要保存什么？',
      knowledgePoint: 'JVM',
      status: 'PENDING'
    })
  })

  it('prioritizes the selected mistake when retrying from mistake book', async () => {
    window.sessionStorage.setItem('studyCollectionRetryMistake', JSON.stringify({
      questionId: 2,
      questionTitle: 'HashMap 默认负载因子是多少？'
    }))
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 1,
        title: 'JVM 栈内存主要保存什么？',
        type: 'SHORT_ANSWER',
        difficulty: 'BEGINNER',
        knowledgePoint: 'JVM',
        answer: '栈帧',
        analysis: '虚拟机栈保存方法调用的栈帧。'
      },
      {
        id: 2,
        title: 'HashMap 默认负载因子是多少？',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        answer: '0.75',
        analysis: 'HashMap 默认负载因子是 0.75。'
      }
    ])

    const wrapper = mount(PracticePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('错题重练')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).not.toContain('JVM 栈内存主要保存什么？')
  })
})
