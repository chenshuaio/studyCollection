import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import LoginPage from './LoginPage.vue'
import { login } from '../api'

vi.mock('../api', () => ({
  login: vi.fn()
}))

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

  it('shows account or password error when login fails', async () => {
    vi.mocked(login).mockRejectedValue(new Error('用户名或密码错误'))

    const wrapper = mount(LoginPage)

    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(wrapper.text()).toContain('账号或密码错误')
  })
})
