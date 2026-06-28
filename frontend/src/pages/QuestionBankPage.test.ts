import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import QuestionBankPage from './QuestionBankPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('QuestionBankPage', () => {
  it('renders question bank management workspace', () => {
    const wrapper = mount(QuestionBankPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('题库管理')
    expect(wrapper.text()).toContain('知识点')
    expect(wrapper.text()).toContain('难度')
    expect(wrapper.text()).toContain('题型')
    expect(wrapper.text()).toContain('新增题目')
    expect(wrapper.text()).toContain('HashMap 默认负载因子')
  })
})
