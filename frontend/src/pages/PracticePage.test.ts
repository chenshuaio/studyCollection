import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import PracticePage from './PracticePage.vue'
import {
  generatePractice,
  listKnowledgePoints,
  recordMistake,
  searchQuestions,
  submitUserPractice,
  type GeneratedPracticeQuestion
} from '../api'

vi.mock('../api', () => ({
  generatePractice: vi.fn(),
  listKnowledgePoints: vi.fn(),
  searchQuestions: vi.fn(),
  submitUserPractice: vi.fn(),
  submitQuestionFeedback: vi.fn(),
  recordMistake: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

function mockGeneratedQuestions(questions: GeneratedPracticeQuestion[]) {
  vi.mocked(generatePractice).mockResolvedValue({
    requestedCount: 10,
    actualCount: questions.length,
    questions
  })
}

describe('PracticePage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    window.history.replaceState({}, '', '/practice')
    vi.mocked(generatePractice).mockReset()
    vi.mocked(listKnowledgePoints).mockReset()
    vi.mocked(searchQuestions).mockReset()
    vi.mocked(submitUserPractice).mockReset()
    vi.mocked(recordMistake).mockReset()
    vi.mocked(listKnowledgePoints).mockResolvedValue([
      { id: 1, name: 'Java 基础', description: '语言基础', enabled: true },
      { id: 2, name: 'JVM', description: '虚拟机', enabled: true }
    ])
    vi.mocked(searchQuestions).mockResolvedValue([])
    mockGeneratedQuestions([
      {
        id: 88,
        title: 'JVM 栈内存主要保存什么？',
        type: 'SHORT_ANSWER',
        difficulty: 'BEGINNER',
        knowledgePoint: 'JVM'
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
    expect(listKnowledgePoints).toHaveBeenCalledWith()
    expect(generatePractice).toHaveBeenCalledWith({ count: 10 })
    expect(wrapper.text()).toContain('JVM 栈内存主要保存什么？')
    expect(wrapper.text()).toContain('提交答案')
    expect(wrapper.text()).toContain('答案解析')
    expect(wrapper.text()).toContain('得分')
    expect(wrapper.text()).toContain('反馈题目问题')
    expect(wrapper.find('textarea[aria-label="题目反馈内容"]').exists()).toBe(true)
  })

  it('renders unselected radio options for a single choice question', async () => {
    mockGeneratedQuestions([
      {
        id: 89,
        title: 'Java 中 int 成员变量的默认值是多少？\nA. 0\nB. null\nC. 1\nD. 不确定',
        type: 'SINGLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础'
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

    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(4)
    expect(wrapper.findAll('input[type="radio"]:checked')).toHaveLength(0)
    expect(wrapper.find('input[aria-label="练习答案"]').exists()).toBe(false)
    expect(wrapper.find('.practice-question h2').text()).toBe('Java 中 int 成员变量的默认值是多少？')
  })

  it('renders checkboxes for a multiple choice practice question', async () => {
    mockGeneratedQuestions([
      {
        id: 90,
        title: '以下哪些属于 Java 集合接口？\nA. List\nB. Set\nC. Thread\nD. Map',
        type: 'MULTIPLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: '集合框架'
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

    expect(wrapper.findAll('input[type="checkbox"]')).toHaveLength(4)
    expect(wrapper.findAll('input[type="checkbox"]:checked')).toHaveLength(0)
    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(0)
  })

  it('renders fallback options when a stored choice question has no option text', async () => {
    mockGeneratedQuestions([
      {
        id: 91,
        title: 'HashMap 默认负载因子是多少？',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架'
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

    expect(wrapper.findAll('input[type="radio"]')).toHaveLength(4)
    expect(wrapper.find('input[aria-label="练习答案"]').exists()).toBe(false)
    expect(wrapper.text()).toContain('A. 选项 A（原题未提供选项内容）')
  })

  it('records a mistake when the submitted answer is wrong', async () => {
    mockGeneratedQuestions([{
      id: 88,
      title: 'JVM 栈内存主要保存什么？',
      type: 'FILL_BLANK',
      difficulty: 'BEGINNER',
      knowledgePoint: 'JVM'
    }])
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({
      token: 'user-token', userId: 7, username: 'alice', role: 'USER', displayName: 'Alice'
    }))
    vi.mocked(submitUserPractice).mockResolvedValue({
      score: 0,
      totalScore: 10,
      items: [
        {
          questionId: 88,
          submittedAnswer: '堆对象',
          correctAnswer: '栈帧',
          autoGraded: true,
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
    await wrapper.get('[data-action="submit-answer"]').trigger('click')
    await flushPromises()

    expect(submitUserPractice).toHaveBeenCalledWith([{
      questionId: 88,
      answer: '堆对象'
    }])
    expect(recordMistake).toHaveBeenCalledWith({
      questionId: 88,
      questionTitle: 'JVM 栈内存主要保存什么？',
      knowledgePoint: 'JVM',
      status: 'PENDING'
    })
  })

  it('shows a subjective reference answer without recording a mistake', async () => {
    vi.mocked(submitUserPractice).mockResolvedValue({
      score: 0,
      totalScore: 0,
      items: [
        {
          questionId: 88,
          submittedAnswer: '我的理解',
          correctAnswer: '栈帧',
          autoGraded: false,
          correct: null,
          score: 0,
          analysis: '虚拟机栈保存方法调用的栈帧。'
        }
      ]
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

    await wrapper.find('input[aria-label="练习答案"]').setValue('我的理解')
    await wrapper.get('[data-action="submit-answer"]').trigger('click')
    await flushPromises()

    expect(wrapper.text()).toContain('本题不自动评分，请结合参考答案自行核对。')
    expect(wrapper.text()).toContain('参考答案')
    expect(recordMistake).not.toHaveBeenCalled()
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

  it('generates a practice using knowledge point, difficulty, type and count filters', async () => {
    const wrapper = mount(PracticePage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    await wrapper.get('select[aria-label="知识点筛选"]').setValue('JVM')
    await wrapper.get('select[aria-label="难度筛选"]').setValue('ADVANCED')
    await wrapper.get('select[aria-label="题型筛选"]').setValue('PROGRAMMING')
    await wrapper.get('input[aria-label="练习题数"]').setValue(3)
    await wrapper.get('[data-action="generate-practice"]').trigger('click')
    await flushPromises()

    expect(generatePractice).toHaveBeenLastCalledWith({
      knowledgePoint: 'JVM',
      difficulty: 'ADVANCED',
      type: 'PROGRAMMING',
      count: 3
    })
  })

  it('automatically generates a targeted practice from the report knowledge point query', async () => {
    window.history.replaceState({}, '', '/practice?knowledgePoint=JVM')

    const wrapper = mount(PracticePage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    const knowledgePointSelect = wrapper.get('select[aria-label="知识点筛选"]').element as HTMLSelectElement
    expect(knowledgePointSelect.value).toBe('JVM')
    expect(generatePractice).toHaveBeenCalledWith({ knowledgePoint: 'JVM', count: 10 })
  })

  it('answers generated questions in sequence and shows a completion summary', async () => {
    mockGeneratedQuestions([
      { id: 1, title: '第一题', type: 'FILL_BLANK', difficulty: 'BEGINNER', knowledgePoint: 'Java 基础' },
      { id: 2, title: '第二题', type: 'SHORT_ANSWER', difficulty: 'INTERMEDIATE', knowledgePoint: 'JVM' },
      { id: 3, title: '第三题', type: 'PROGRAMMING', difficulty: 'ADVANCED', knowledgePoint: '并发编程' }
    ])
    vi.mocked(submitUserPractice)
      .mockResolvedValueOnce({ score: 10, totalScore: 10, items: [{ questionId: 1, submittedAnswer: '答案一', correctAnswer: '答案一', autoGraded: true, correct: true, score: 10, analysis: '解析一' }] })
      .mockResolvedValueOnce({ score: 0, totalScore: 0, items: [{ questionId: 2, submittedAnswer: '答案二', correctAnswer: '参考二', autoGraded: false, correct: null, score: 0, analysis: '解析二' }] })
      .mockResolvedValueOnce({ score: 0, totalScore: 0, items: [{ questionId: 3, submittedAnswer: '答案三', correctAnswer: '参考三', autoGraded: false, correct: null, score: 0, analysis: '解析三' }] })

    const wrapper = mount(PracticePage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('第 1 / 3 题')
    await wrapper.get('input[aria-label="练习答案"]').setValue('答案一')
    await wrapper.get('[data-action="submit-answer"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-action="next-question"]').trigger('click')
    expect(wrapper.text()).toContain('第 2 / 3 题')

    await wrapper.get('input[aria-label="练习答案"]').setValue('答案二')
    await wrapper.get('[data-action="submit-answer"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-action="next-question"]').trigger('click')
    expect(wrapper.text()).toContain('第 3 / 3 题')

    await wrapper.get('input[aria-label="练习答案"]').setValue('答案三')
    await wrapper.get('[data-action="submit-answer"]').trigger('click')
    await flushPromises()
    await wrapper.get('[data-action="finish-practice"]').trigger('click')

    expect(wrapper.text()).toContain('本次练习已完成')
    expect(wrapper.text()).toContain('共完成 3 题')
  })
})
