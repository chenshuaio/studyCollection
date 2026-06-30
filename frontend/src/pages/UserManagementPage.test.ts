import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import UserManagementPage from './UserManagementPage.vue'
import { listUsers } from '../api'

vi.mock('../api', () => ({
  listUsers: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('UserManagementPage', () => {
  it('loads and renders user summaries without password data', async () => {
    vi.mocked(listUsers).mockResolvedValue([
      { id: 1, username: 'admin', displayName: '系统管理员', role: 'ADMIN' },
      { id: 2, username: 'alice', displayName: 'Alice', role: 'USER' }
    ])

    const wrapper = mount(UserManagementPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          CurrentAccount: true,
          LogoutButton: true
        }
      }
    })

    await flushPromises()

    expect(listUsers).toHaveBeenCalled()
    expect(wrapper.text()).toContain('用户管理')
    expect(wrapper.text()).toContain('平台账号')
    expect(wrapper.text()).toContain('系统管理员')
    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('管理员')
    expect(wrapper.text()).toContain('普通用户')
    expect(wrapper.text()).not.toContain('password')
    expect(wrapper.text()).not.toContain('{plain}')
  })
})
