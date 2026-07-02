import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import KnowledgePointPage from './KnowledgePointPage.vue'
import { createKnowledgePoint, disableKnowledgePoint, listKnowledgePoints } from '../api'

vi.mock('../api', () => ({
  createKnowledgePoint: vi.fn(),
  disableKnowledgePoint: vi.fn(),
  listKnowledgePoints: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('KnowledgePointPage', () => {
  it('loads, creates and disables knowledge point categories', async () => {
    vi.mocked(listKnowledgePoints).mockResolvedValue([
      { id: 1, name: 'JVM', description: '运行时内存与类加载', enabled: true }
    ])
    vi.mocked(createKnowledgePoint).mockResolvedValue({
      id: 2,
      name: '并发编程',
      description: '线程与锁',
      enabled: true
    })
    vi.mocked(disableKnowledgePoint).mockResolvedValue({
      id: 1,
      name: 'JVM',
      description: '运行时内存与类加载',
      enabled: false
    })

    const wrapper = mount(KnowledgePointPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          CurrentAccount: true,
          LogoutButton: true
        }
      }
    })

    await flushPromises()

    expect(listKnowledgePoints).toHaveBeenCalled()
    expect(wrapper.text()).toContain('知识点管理')
    expect(wrapper.text()).toContain('JVM')
    expect(wrapper.text()).toContain('运行时内存与类加载')

    await wrapper.get('[aria-label="知识点名称"]').setValue('并发编程')
    await wrapper.get('[aria-label="知识点说明"]').setValue('线程与锁')
    await wrapper.get('form').trigger('submit')
    await flushPromises()

    expect(createKnowledgePoint).toHaveBeenCalledWith({ name: '并发编程', description: '线程与锁' })
    expect(wrapper.text()).toContain('知识点已创建')

    await wrapper.get('[aria-label="停用知识点"]').trigger('click')
    await flushPromises()

    expect(disableKnowledgePoint).toHaveBeenCalledWith(1)
    expect(wrapper.text()).toContain('已停用')
  })
})
