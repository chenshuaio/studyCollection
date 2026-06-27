import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import LoginPage from './LoginPage.vue'

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>'
  },
  useRouter: () => ({
    push: vi.fn()
  })
}))

describe('LoginPage', () => {
  it('renders professional Java learning platform copy', () => {
    const wrapper = mount(LoginPage)

    expect(wrapper.text()).toContain('Java 学习题库平台')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('错题报告')
    expect(wrapper.find('button[type="submit"]').text()).toBe('登录')
  })
})
