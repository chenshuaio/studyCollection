import { describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RegisterPage from './RegisterPage.vue'

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>'
  },
  useRouter: () => ({
    push: vi.fn()
  })
}))

describe('RegisterPage', () => {
  it('renders account creation form for local learners', () => {
    const wrapper = mount(RegisterPage)

    expect(wrapper.text()).toContain('创建学习账号')
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('昵称')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.find('button[type="submit"]').text()).toBe('注册并进入')
  })
})
