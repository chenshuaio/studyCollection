import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import PracticePage from './PracticePage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('PracticePage', () => {
  it('renders practice answering workflow', () => {
    const wrapper = mount(PracticePage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub
        }
      }
    })

    expect(wrapper.text()).toContain('练习中心')
    expect(wrapper.text()).toContain('HashMap 默认负载因子是多少？')
    expect(wrapper.text()).toContain('提交答案')
    expect(wrapper.text()).toContain('答案解析')
    expect(wrapper.text()).toContain('得分')
  })
})
