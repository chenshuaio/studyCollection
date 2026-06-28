import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import QuestionBankPage from './QuestionBankPage.vue'
import { searchQuestions } from '../api'

vi.mock('../api', () => ({
  createQuestion: vi.fn(),
  searchQuestions: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('QuestionBankPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
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
})
