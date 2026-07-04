import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ExamTakingPage from './ExamTakingPage.vue'
import { recordMistake, submitUserPractice } from '../api'

vi.mock('../api', () => ({
  submitUserPractice: vi.fn(),
  recordMistake: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

describe('ExamTakingPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.mocked(submitUserPractice).mockReset()
    vi.mocked(recordMistake).mockReset()
  })

  it('submits every selected question in the custom exam paper', async () => {
    window.sessionStorage.setItem(
      'studyCollectionExamPaper',
      JSON.stringify({
        name: '集合专项测试',
        durationMinutes: 45,
        questionIds: [1, 2, 3],
        questions: [
          {
            id: 1,
            title: 'HashMap 默认负载因子是多少？',
            type: 'SINGLE_CHOICE',
            difficulty: 'INTERMEDIATE',
            knowledgePoint: '集合框架',
            answer: 'A',
            options: [
              { value: 'A', label: '0.75' },
              { value: 'B', label: '0.5' }
            ]
          },
          {
            id: 2,
            title: 'Java 局部变量必须先赋值再使用，这句话是否正确？',
            type: 'TRUE_FALSE',
            difficulty: 'BEGINNER',
            knowledgePoint: 'Java 基础',
            answer: 'true',
            options: [
              { value: 'true', label: '正确' },
              { value: 'false', label: '错误' }
            ]
          },
          {
            id: 3,
            title: 'ArrayList 扩容通常发生在什么时候？',
            type: 'SINGLE_CHOICE',
            difficulty: 'INTERMEDIATE',
            knowledgePoint: '集合框架',
            answer: 'A',
            analysis: 'ArrayList 在容量不足以容纳新增元素时会触发扩容。'
          }
        ]
      })
    )
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ userId: 7, role: 'USER', displayName: 'Alice' }))
    vi.mocked(submitUserPractice).mockResolvedValue({
      score: 20,
      totalScore: 30,
      items: [
        {
          questionId: 1,
          submittedAnswer: 'A',
          correctAnswer: 'A',
          correct: true,
          score: 10,
          analysis: 'HashMap 默认负载因子是 0.75。'
        },
        {
          questionId: 2,
          submittedAnswer: 'true',
          correctAnswer: 'true',
          correct: true,
          score: 10,
          analysis: 'Java 局部变量必须先赋值再使用。'
        },
        {
          questionId: 3,
          submittedAnswer: 'B',
          correctAnswer: 'A',
          correct: false,
          score: 0,
          analysis: 'ArrayList 在容量不足时扩容。'
        }
      ]
    })
    vi.mocked(recordMistake).mockResolvedValue({
      userId: 7,
      questionId: 3,
      questionTitle: 'ArrayList 扩容通常发生在什么时候？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })

    const wrapper = mount(ExamTakingPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('集合专项测试')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('Java 局部变量必须先赋值再使用')
    expect(wrapper.text()).toContain('ArrayList 扩容通常发生在什么时候？')
    expect(wrapper.findAll('input[type="radio"]:checked')).toHaveLength(0)

    await wrapper.find('button[type="button"]').trigger('click')
    await flushPromises()

    expect(submitUserPractice).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('请先完成所有题目后再提交。')

    await wrapper.find('input[name="question-1"][value="A"]').setValue()
    await wrapper.find('input[name="question-2"][value="true"]').setValue()
    await wrapper.find('input[name="question-3"][value="B"]').setValue()
    await wrapper.find('button[type="button"]').trigger('click')
    await flushPromises()

    expect(submitUserPractice).toHaveBeenCalledWith(7, [
      { questionId: 1, answer: 'A', correctAnswer: 'A', analysis: undefined },
      { questionId: 2, answer: 'true', correctAnswer: 'true', analysis: undefined },
      {
        questionId: 3,
        answer: 'B',
        correctAnswer: 'A',
        analysis: 'ArrayList 在容量不足以容纳新增元素时会触发扩容。'
      }
    ])
    expect(recordMistake).toHaveBeenCalledWith({
      userId: 7,
      questionId: 3,
      questionTitle: 'ArrayList 扩容通常发生在什么时候？',
      knowledgePoint: '集合框架',
      status: 'PENDING'
    })
    expect(wrapper.text()).toContain('20/30')
    expect(wrapper.text()).toContain('ArrayList 在容量不足时扩容。')
  })
  it('renders choice buttons for stored choice questions that have no options yet', () => {
    window.sessionStorage.setItem(
      'studyCollectionExamPaper',
      JSON.stringify({
        name: 'Java 基础测试',
        durationMinutes: 30,
        questionIds: [6],
        questions: [
          {
            id: 6,
            title: 'Java 中 int 默认值是多少？',
            type: 'SINGLE_CHOICE',
            difficulty: 'BEGINNER',
            knowledgePoint: 'Java 基础',
            answer: 'A',
            analysis: 'int 成员变量默认值为 0。'
          }
        ]
      })
    )

    const wrapper = mount(ExamTakingPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(4)
    expect(wrapper.find('.answer-field').exists()).toBe(false)
    expect(wrapper.text()).toContain('A. 选项 A（原题未提供选项内容）')
  })
})
