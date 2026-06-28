import { beforeEach, describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import CurrentAccount from './CurrentAccount.vue'

describe('CurrentAccount', () => {
  beforeEach(() => {
    window.localStorage.clear()
  })

  it('shows the current signed-in account and localized role', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({
        username: 'alice',
        displayName: 'Alice',
        role: 'USER'
      })
    )

    const wrapper = mount(CurrentAccount)

    expect(wrapper.text()).toContain('Alice')
    expect(wrapper.text()).toContain('alice')
    expect(wrapper.text()).toContain('普通用户')
  })

  it('shows administrator role label', () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({
        username: 'admin',
        displayName: '系统管理员',
        role: 'ADMIN'
      })
    )

    const wrapper = mount(CurrentAccount)

    expect(wrapper.text()).toContain('系统管理员')
    expect(wrapper.text()).toContain('管理员')
  })
})
