import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import MistakePage from './MistakePage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

const mistakes = [
  {
    userId: 7,
    questionId: 1,
    questionTitle: 'HashMap 默认负载因子是多少？',
    questionType: 'SINGLE_CHOICE',
    knowledgePoint: '集合框架',
    lastSubmittedAnswer: 'A',
    sourceContext: 'PRACTICE',
    status: 'PENDING',
    wrongCount: 2,
    firstWrongAt: '2026-07-10T08:00:00Z',
    lastWrongAt: '2026-07-12T08:00:00Z'
  },
  {
    userId: 7,
    questionId: 2,
    questionTitle: 'JVM 栈内存主要保存什么？',
    questionType: 'FILL_BLANK',
    knowledgePoint: 'JVM',
    lastSubmittedAnswer: '堆对象',
    sourceContext: 'EXAM',
    status: 'PENDING',
    wrongCount: 1,
    firstWrongAt: '2026-07-11T08:00:00Z',
    lastWrongAt: '2026-07-11T08:00:00Z'
  },
  {
    userId: 7,
    questionId: 3,
    questionTitle: 'JVM 堆内存保存什么？',
    questionType: 'SHORT_ANSWER',
    knowledgePoint: 'JVM',
    lastSubmittedAnswer: '线程',
    sourceContext: 'PRACTICE',
    status: 'MASTERED',
    wrongCount: 3,
    firstWrongAt: '2026-07-01T08:00:00Z',
    lastWrongAt: '2026-07-09T08:00:00Z'
  }
]

function mountMistakePage(fetchMock = vi.fn().mockResolvedValue({
  ok: true,
  json: async () => ({ code: 'OK', data: mistakes })
})) {
  vi.stubGlobal('fetch', fetchMock)
  return mount(MistakePage, {
    global: {
      stubs: {
        RouterLink: routerLinkStub,
        LogoutButton: true
      }
    }
  })
}

describe('MistakePage', () => {
  beforeEach(() => {
    window.sessionStorage.clear()
  })

  it('renders user mistake book workflow with summary counts', async () => {
    const wrapper = mountMistakePage()

    await flushPromises()

    expect(wrapper.text()).toContain('错题本')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('集合框架')
    expect(wrapper.text()).toContain('单选题')
    expect(wrapper.text()).toContain('答错 2 次')
    expect(wrapper.text()).toContain('2026-07-10')
    expect(wrapper.text()).toContain('2026-07-12')
    expect(wrapper.text()).toContain('重新练习')
    expect(wrapper.text()).toContain('全部错题3')
    expect(wrapper.text()).toContain('待巩固2')
    expect(wrapper.text()).toContain('已掌握1')

    vi.unstubAllGlobals()
  })

  it('filters mistakes by knowledge point, type, date and mastery status', async () => {
    const wrapper = mountMistakePage()

    await flushPromises()
    await wrapper.find('select[aria-label="错题知识点筛选"]').setValue('JVM')
    await wrapper.find('select[aria-label="错题题型筛选"]').setValue('FILL_BLANK')
    await wrapper.find('select[aria-label="错题状态筛选"]').setValue('PENDING')
    await wrapper.find('input[aria-label="最近错误开始日期"]').setValue('2026-07-10')
    await wrapper.find('input[aria-label="最近错误结束日期"]').setValue('2026-07-11')

    expect(wrapper.text()).toContain('JVM 栈内存主要保存什么？')
    expect(wrapper.text()).not.toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).not.toContain('JVM 堆内存保存什么？')

    vi.unstubAllGlobals()
  })

  it('marks pending mistakes as mastered and refreshes the list', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [mistakes[0]]
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            ...mistakes[0],
            status: 'MASTERED'
          }
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [{ ...mistakes[0], status: 'MASTERED' }]
        })
      })

    const wrapper = mountMistakePage(fetchMock)

    await flushPromises()
    await wrapper.find('button[aria-label="标记已掌握"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/mistakes/status',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ questionId: 1, status: 'MASTERED' })
      })
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes', expect.objectContaining({ method: 'GET' }))
    expect(wrapper.text()).toContain('已掌握')

    vi.unstubAllGlobals()
  })

  it('stores selected mistake before retrying practice', async () => {
    const wrapper = mountMistakePage()

    await flushPromises()
    await wrapper.find('a[aria-label="重练 HashMap 默认负载因子是多少？"]').trigger('click')

    expect(window.sessionStorage.getItem('studyCollectionRetryMistake')).toBe(JSON.stringify({
      questionId: 1,
      questionTitle: 'HashMap 默认负载因子是多少？'
    }))

    vi.unstubAllGlobals()
  })

  it('submits suspected question issues with the latest wrong answer context', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({ code: 'OK', data: [mistakes[0]] })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            id: 10,
            userId: 7,
            questionId: 1,
            type: 'ANSWER_ERROR',
            content: '标准答案可能有误',
            status: 'PENDING'
          }
        })
      })
    const wrapper = mountMistakePage(fetchMock)
    await flushPromises()

    await wrapper.find('button[aria-label="反馈 HashMap 默认负载因子是多少？"]').trigger('click')
    await wrapper.find('textarea[aria-label="错题反馈内容"]').setValue('标准答案可能有误')
    await wrapper.find('button[data-action="submit-mistake-feedback"]').trigger('click')
    await flushPromises()

    expect(fetchMock).toHaveBeenNthCalledWith(
      2,
      '/api/questions/feedback',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          questionId: 1,
          type: 'ANSWER_ERROR',
          content: '标准答案可能有误',
          submittedAnswer: 'A',
          sourceContext: 'MISTAKE_BOOK',
          sourceReference: 'mistake-1'
        })
      })
    )
    expect(wrapper.text()).toContain('反馈已提交')

    vi.unstubAllGlobals()
  })
})
