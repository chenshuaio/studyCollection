import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ReportPage from './ReportPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ReportPage', () => {
  it('generates report from current user mistake statuses', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ userId: 7, role: 'USER', displayName: 'Alice' }))
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: [
            {
              userId: 7,
              questionId: 1,
              questionTitle: 'HashMap 默认负载因子是多少？',
              knowledgePoint: '集合框架',
              status: 'MASTERED'
            },
            {
              userId: 7,
              questionId: 2,
              questionTitle: 'JVM 栈内存主要保存什么？',
              knowledgePoint: 'JVM',
              status: 'PENDING'
            }
          ]
        })
      })
      .mockResolvedValueOnce({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            weakestKnowledgePoint: 'JVM',
            recommendation: '建议优先强化 JVM',
            adviceSource: 'RULES',
            adviceContent: '规则分析建议：请针对 JVM 继续练习。'
          }
        })
      })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(ReportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await flushPromises()
    expect(wrapper.text()).toContain('学习报告')
    expect(wrapper.text()).toContain('错题样本2')
    expect(wrapper.text()).toContain('待巩固1')

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(fetchMock).toHaveBeenCalledWith('/api/mistakes?userId=7', expect.objectContaining({ method: 'GET' }))
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/reports/learning',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({
          mode: 'OFFLINE_RULES',
          results: [
            { knowledgePoint: '集合框架', correct: true },
            { knowledgePoint: 'JVM', correct: false }
          ]
        })
      })
    )
    expect(wrapper.text()).toContain('JVM')
    expect(wrapper.text()).toContain('RULES')
    expect(wrapper.text()).toContain('建议优先强化 JVM')

    vi.unstubAllGlobals()
    window.localStorage.clear()
  })

  it('does not generate report when there is no mistake data', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ userId: 7, role: 'USER', displayName: 'Alice' }))
    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      json: async () => ({ code: 'OK', data: [] })
    })
    vi.stubGlobal('fetch', fetchMock)

    const wrapper = mount(ReportPage, {
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

    expect(wrapper.text()).toContain('暂无错题数据，先完成练习或考试后再生成报告。')
    expect(fetchMock).toHaveBeenCalledTimes(1)

    vi.unstubAllGlobals()
    window.localStorage.clear()
  })
})
