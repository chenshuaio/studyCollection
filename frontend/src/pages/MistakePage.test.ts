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
    knowledgePoint: '集合框架',
    status: 'PENDING'
  },
  {
    userId: 7,
    questionId: 2,
    questionTitle: 'JVM 栈内存主要保存什么？',
    knowledgePoint: 'JVM',
    status: 'PENDING'
  },
  {
    userId: 7,
    questionId: 3,
    questionTitle: 'JVM 堆内存保存什么？',
    knowledgePoint: 'JVM',
    status: 'MASTERED'
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
    expect(wrapper.text()).toContain('重新练习')
    expect(wrapper.text()).toContain('全部错题3')
    expect(wrapper.text()).toContain('待巩固2')
    expect(wrapper.text()).toContain('已掌握1')

    vi.unstubAllGlobals()
  })

  it('filters mistakes by knowledge point and mastery status', async () => {
    const wrapper = mountMistakePage()

    await flushPromises()
    await wrapper.find('select[aria-label="错题知识点筛选"]').setValue('JVM')
    await wrapper.find('select[aria-label="错题状态筛选"]').setValue('PENDING')

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
        body: JSON.stringify({ userId: 7, questionId: 1, status: 'MASTERED' })
      })
    )
    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes?userId=7', expect.objectContaining({ method: 'GET' }))
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
})
