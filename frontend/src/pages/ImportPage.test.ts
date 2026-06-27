import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ImportPage', () => {
  it('renders import preview workflow', () => {
    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub
        }
      }
    })

    expect(wrapper.text()).toContain('题目导入')
    expect(wrapper.text()).toContain('粘贴题目')
    expect(wrapper.text()).toContain('解析预览')
    expect(wrapper.text()).toContain('确认入库')
    expect(wrapper.text()).toContain('Java 中 int 默认值是多少？')
  })
})
