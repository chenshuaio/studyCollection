import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import DashboardPage from './DashboardPage.vue'

describe('DashboardPage', () => {
  it('renders the main local learning workspace', () => {
    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).toContain('学习控制台')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('自定义组卷')
    expect(wrapper.text()).toContain('错题反馈')
    expect(wrapper.text()).toContain('AI 分析')
  })
})
