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
})
