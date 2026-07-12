import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import QuestionBankPage from './QuestionBankPage.vue'
import { approvePendingQuestion, deleteQuestion, listPendingQuestions, rejectPendingQuestion, searchQuestions } from '../api'

vi.mock('../api', () => ({
  approvePendingQuestion: vi.fn(),
  createQuestion: vi.fn(),
  deleteQuestion: vi.fn(),
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
    window.localStorage.clear()
    vi.mocked(listPendingQuestions).mockResolvedValue([])
  })

  it('renders searchable question bank management workspace', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' })
    )
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
    expect((wrapper.get('#new-question textarea').element as HTMLTextAreaElement).value)
      .toContain('HashMap 默认负载因子')
  })

  it('loads all questions by default and searches with fuzzy title keyword', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' })
    )
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
      type: '',
      scope: 'PUBLIC'
    })
    expect(searchQuestions).toHaveBeenNthCalledWith(2, {
      keyword: 'Concurrent',
      knowledgePoint: '',
      difficulty: '',
      type: '',
      scope: 'PUBLIC'
    })
    expect(wrapper.text()).toContain('ConcurrentHashMap')
  })

  it('allows administrators to delete formal questions and refreshes the current list', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' })
    )
    vi.stubGlobal('confirm', vi.fn().mockReturnValue(true))
    vi.mocked(searchQuestions)
      .mockResolvedValueOnce([
        {
          id: 9,
          title: 'HashMap 默认负载因子是多少？',
          type: 'SINGLE_CHOICE',
          difficulty: 'INTERMEDIATE',
          knowledgePoint: '集合框架',
          answer: '0.75',
          analysis: 'HashMap 默认负载因子是 0.75。'
        }
      ])
      .mockResolvedValueOnce([])
    vi.mocked(deleteQuestion).mockResolvedValue(9)

    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')

    await wrapper.find('button[aria-label="删除题目"]').trigger('click')
    await flushPromises()

    expect(window.confirm).toHaveBeenCalledWith('确认删除这道题目吗？删除后题库中将不再展示。')
    expect(deleteQuestion).toHaveBeenCalledWith(9)
    expect(searchQuestions).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('题目已删除。')
  })

  it('shows normal users public and personal scopes but only lets them delete owned questions', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
    )
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 9,
        title: 'HashMap 默认负载因子是多少？',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        answer: '0.75',
        analysis: 'HashMap 默认负载因子是 0.75。',
        ownerUserId: null
      },
      {
        id: 10,
        title: '我的个人题',
        type: 'SHORT_ANSWER',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        answer: '',
        analysis: '',
        ownerUserId: 7
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

    expect(wrapper.text()).toContain('我的题库')
    expect(wrapper.text()).toContain('全部可用')
    expect(wrapper.text()).toContain('公共题库')
    expect(searchQuestions).toHaveBeenCalledWith(expect.objectContaining({ scope: 'ALL' }))
    expect(wrapper.findAll('button[aria-label="删除题目"]')).toHaveLength(1)
    expect(wrapper.text()).not.toContain('新增题目')
    expect(wrapper.text()).not.toContain('待审核导入')
  })

  it('reviews pending imported questions and refreshes the formal question bank after approval', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' })
    )
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
        targetScope: 'PERSONAL',
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
    expect(wrapper.text()).toContain('个人题库')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')

    await wrapper.find('button[aria-label="通过待审核题目"]').trigger('click')
    await flushPromises()

    expect(approvePendingQuestion).toHaveBeenCalledWith(1)
    expect(searchQuestions).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('审核通过，题目已入库。')
  })

  it('rejects pending imported questions without saving them', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', userId: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' })
    )
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
        targetScope: 'PUBLIC',
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
      targetScope: 'PUBLIC',
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
