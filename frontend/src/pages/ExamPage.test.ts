import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises } from '@vue/test-utils'
import { mount } from '@vue/test-utils'
import ExamPage from './ExamPage.vue'
import { composeCustomExam, searchQuestions } from '../api'

vi.mock('../api', () => ({
  composeCustomExam: vi.fn(),
  searchQuestions: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a :href="to"><slot /></a>'
}

describe('ExamPage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
    vi.mocked(searchQuestions).mockReset()
    vi.mocked(composeCustomExam).mockReset()
  })

  afterEach(() => {
    vi.clearAllMocks()
  })

  it('renders custom exam composition workflow', async () => {
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 42,
        title: 'ConcurrentHashMap 如何降低锁粒度？',
        type: 'SHORT_ANSWER',
        difficulty: 'ADVANCED',
        knowledgePoint: '并发编程',
        answer: '分段或桶级控制',
        analysis: '通过更细粒度的同步降低锁竞争。'
      }
    ])
    vi.mocked(composeCustomExam).mockResolvedValue({
      name: '集合专项测试',
      durationMinutes: 45,
      questionIds: [42]
    })

    const wrapper = mount(ExamPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('考试中心')
    expect(wrapper.text()).toContain('自定义组卷')
    expect(wrapper.text()).toContain('生成考试卷')
    await flushPromises()
    expect(searchQuestions).toHaveBeenCalledWith()
    expect(wrapper.text()).toContain('ConcurrentHashMap 如何降低锁粒度？')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('集合专项测试')
    expect(wrapper.text()).toContain('共 1 题')
    expect(wrapper.find('a[href="/exams/take"]').exists()).toBe(true)

    const savedPaper = JSON.parse(window.sessionStorage.getItem('studyCollectionExamPaper') ?? '{}')
    expect(savedPaper.questionIds).toEqual([42])
    expect(savedPaper.questions).toHaveLength(1)
    expect(savedPaper.questions[0].title).toBe('ConcurrentHashMap 如何降低锁粒度？')
    expect(savedPaper.questions[0].answer).toBe('分段或桶级控制')
  })
  it('stores generated choice options as structured exam question options', async () => {
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 7,
        title: 'HashMap 默认负载因子是多少？\nA. 0.5\nB. 0.75\nC. 1.0\nD. 2.0',
        type: 'SINGLE_CHOICE',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: '集合框架',
        answer: 'B',
        analysis: 'HashMap 默认负载因子是 0.75。'
      }
    ])
    vi.mocked(composeCustomExam).mockResolvedValue({
      name: '集合专项测试',
      durationMinutes: 45,
      questionIds: [7]
    })

    const wrapper = mount(ExamPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const savedPaper = JSON.parse(window.sessionStorage.getItem('studyCollectionExamPaper') ?? '{}')
    expect(savedPaper.questions[0].title).toBe('HashMap 默认负载因子是多少？')
    expect(savedPaper.questions[0].options).toEqual([
      { value: 'A', label: '0.5' },
      { value: 'B', label: '0.75' },
      { value: 'C', label: '1.0' },
      { value: 'D', label: '2.0' }
    ])
  })

  it('keeps choice questions answerable when old question data has no option text', async () => {
    vi.mocked(searchQuestions).mockResolvedValue([
      {
        id: 8,
        title: 'Java 中 int 默认值是多少？',
        type: 'SINGLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        answer: 'A',
        analysis: 'int 成员变量默认值为 0。'
      }
    ])
    vi.mocked(composeCustomExam).mockResolvedValue({
      name: 'Java 基础测试',
      durationMinutes: 30,
      questionIds: [8]
    })

    const wrapper = mount(ExamPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    const savedPaper = JSON.parse(window.sessionStorage.getItem('studyCollectionExamPaper') ?? '{}')
    expect(savedPaper.questions[0].options).toEqual([
      { value: 'A', label: '选项 A（原题未提供选项内容）' },
      { value: 'B', label: '选项 B（原题未提供选项内容）' },
      { value: 'C', label: '选项 C（原题未提供选项内容）' },
      { value: 'D', label: '选项 D（原题未提供选项内容）' }
    ])
  })
})
