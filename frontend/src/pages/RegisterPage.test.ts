import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import RegisterPage from './RegisterPage.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn(),
  register: vi.fn()
}))

vi.mock('vue-router', () => ({
  RouterLink: {
    props: ['to'],
    template: '<a><slot /></a>'
  },
  useRouter: () => ({
    push: mocks.push
  })
}))

vi.mock('../api', () => ({
  register: mocks.register
}))

describe('RegisterPage', () => {
  beforeEach(() => {
    mocks.push.mockReset()
    mocks.register.mockReset()
    window.localStorage.clear()
  })

  it('renders account creation form for local learners', () => {
    const wrapper = mount(RegisterPage)

    expect(wrapper.text()).toContain('创建学习账号')
    expect(wrapper.text()).toContain('账号')
    expect(wrapper.text()).toContain('昵称')
    expect(wrapper.text()).toContain('密码')
    expect(wrapper.find('button[type="submit"]').text()).toBe('注册并进入')
  })

  it('stores registered learners as ordinary users before entering workspace', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'admin-token', role: 'ADMIN', displayName: '系统管理员' })
    )
    mocks.register.mockResolvedValue({
      userId: 8,
      username: 'alice',
      displayName: 'Alice',
      role: 'USER'
    })

    const wrapper = mount(RegisterPage)

    await wrapper.find('form').trigger('submit')

    const stored = JSON.parse(window.localStorage.getItem('studyCollectionUser') ?? '{}')
    expect(stored.role).toBe('USER')
    expect(stored.displayName).toBe('Alice')
    expect(mocks.push).toHaveBeenCalledWith('/dashboard')
  })
})
