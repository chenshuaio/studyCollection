import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import QuestionBankPage from './QuestionBankPage.vue'
import { approvePendingQuestion, listPendingQuestions, rejectPendingQuestion, searchQuestions } from '../api'

vi.mock('../api', () => ({
  approvePendingQuestion: vi.fn(),
  createQuestion: vi.fn(),
  listPendingQuestions: vi.fn(),
  rejectPendingQuestion: vi.fn(),
  searchQuestions: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('QuestionBankPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(listPendingQuestions).mockResolvedValue([])
  })

  it('renders searchable question bank management workspace', () => {
    vi.mocked(searchQuestions).mockResolvedValue([])

    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('题库管理')
    expect(wrapper.text()).toContain('题干搜索')
    expect(wrapper.text()).toContain('知识点')
    expect(wrapper.text()).toContain('难度')
    expect(wrapper.text()).toContain('题型')
    expect(wrapper.text()).toContain('新增题目')
    expect(wrapper.text()).toContain('HashMap 默认负载因子')
  })

  it('loads all questions by default and searches with fuzzy title keyword', async () => {
    vi.mocked(searchQuestions)
      .mockResolvedValueOnce([
        {
          id: 1,
          title: 'HashMap 默认负载因子是多少？',
          type: 'SINGLE_CHOICE',
          difficulty: 'INTERMEDIATE',
          knowledgePoint: '集合框架',
          answer: 'A',
          analysis: '0.75'
        }
      ])
      .mockResolvedValueOnce([
        {
          id: 2,
          title: 'ConcurrentHashMap 如何降低锁粒度？',
          type: 'SHORT_ANSWER',
          difficulty: 'ADVANCED',
          knowledgePoint: '并发编程',
          answer: '分段或桶级控制',
          analysis: '降低锁竞争'
        }
      ])

    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    await wrapper.find('input[aria-label="题干搜索"]').setValue('Concurrent')
    await wrapper.find('button[aria-label="搜索题库"]').trigger('click')
    await flushPromises()

    expect(searchQuestions).toHaveBeenNthCalledWith(1, {
      keyword: '',
      knowledgePoint: '',
      difficulty: '',
      type: ''
    })
    expect(searchQuestions).toHaveBeenNthCalledWith(2, {
      keyword: 'Concurrent',
      knowledgePoint: '',
      difficulty: '',
      type: ''
    })
    expect(wrapper.text()).toContain('ConcurrentHashMap')
  })

  it('reviews pending imported questions and refreshes the formal question bank after approval', async () => {
    vi.mocked(searchQuestions)
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([
        {
          id: 10,
          title: 'HashMap 默认负载因子是多少？',
          type: 'SINGLE_CHOICE',
          difficulty: 'INTERMEDIATE',
          knowledgePoint: '集合框架',
          answer: 'A',
          analysis: '0.75'
        }
      ])
    vi.mocked(listPendingQuestions).mockResolvedValue([
      {
        id: 1,
        submitterUserId: 7,
        title: 'HashMap 默认负载因子是多少？',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        answer: 'A',
        analysis: '由导入预览提交审核',
        status: 'PENDING'
      }
    ])
    vi.mocked(approvePendingQuestion).mockResolvedValue({
      id: 10,
      title: 'HashMap 默认负载因子是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: '集合框架',
      answer: 'A',
      analysis: '0.75'
    })

    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    expect(wrapper.text()).toContain('待审核导入')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')

    await wrapper.find('button[aria-label="通过待审核题目"]').trigger('click')
    await flushPromises()

    expect(approvePendingQuestion).toHaveBeenCalledWith(1)
    expect(searchQuestions).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('审核通过，题目已入库。')
  })

  it('rejects pending imported questions without saving them', async () => {
    vi.mocked(searchQuestions).mockResolvedValue([])
    vi.mocked(listPendingQuestions).mockResolvedValue([
      {
        id: 2,
        submitterUserId: 7,
        title: '错误题目',
        type: 'SINGLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        answer: 'B',
        analysis: '待拒绝',
        status: 'PENDING'
      }
    ])
    vi.mocked(rejectPendingQuestion).mockResolvedValue({
      id: 2,
      submitterUserId: 7,
      title: '错误题目',
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: 'Java 基础',
      answer: 'B',
      analysis: '待拒绝',
      status: 'REJECTED'
    })

    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    await wrapper.find('button[aria-label="拒绝待审核题目"]').trigger('click')
    await flushPromises()

    expect(rejectPendingQuestion).toHaveBeenCalledWith(2)
    expect(wrapper.text()).toContain('已拒绝该导入题。')
  })
})
