import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import ExamPage from './ExamPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ExamPage', () => {
  it('renders custom exam composition workflow', async () => {
    vi.stubGlobal(
      'fetch',
      vi.fn().mockResolvedValue({
        ok: true,
        json: async () => ({
          code: 'OK',
          data: {
            name: '集合专项测试',
            durationMinutes: 45,
            questionIds: [1, 2, 3]
          }
        })
      })
    )

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
    expect(wrapper.text()).toContain('集合框架')
    expect(wrapper.text()).toContain('生成考试卷')

    await wrapper.find('form').trigger('submit')
    await new Promise((resolve) => setTimeout(resolve, 0))

    expect(wrapper.text()).toContain('集合专项测试')
    expect(wrapper.text()).toContain('45 分钟')
    expect(wrapper.text()).toContain('共 3 题')

    vi.unstubAllGlobals()
  })
})
