import { beforeEach, describe, expect, it, vi } from 'vitest'
import { mount } from '@vue/test-utils'
import DashboardPage from './DashboardPage.vue'

const mocks = vi.hoisted(() => ({
  push: vi.fn()
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

describe('DashboardPage', () => {
  beforeEach(() => {
    mocks.push.mockReset()
    window.localStorage.clear()
  })

  it('renders the main local learning workspace', () => {
    const wrapper = mount(DashboardPage)

    expect(wrapper.text()).toContain('学习控制台')
    expect(wrapper.text()).toContain('题库导入')
    expect(wrapper.text()).toContain('自定义组卷')
    expect(wrapper.text()).toContain('错题反馈')
    expect(wrapper.text()).toContain('AI 分析')
  })

  it('logs out and returns to login page', async () => {
    window.localStorage.setItem('studyCollectionUser', JSON.stringify({ role: 'USER', displayName: '学习用户' }))
    const wrapper = mount(DashboardPage)

    await wrapper.get('button[aria-label="退出登录"]').trigger('click')

    expect(window.localStorage.getItem('studyCollectionUser')).toBeNull()
    expect(mocks.push).toHaveBeenCalledWith('/')
  })
})
