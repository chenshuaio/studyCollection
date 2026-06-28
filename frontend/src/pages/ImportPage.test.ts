import { describe, expect, it } from 'vitest'
import { mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ImportPage', () => {
  it('renders import preview and knowledge generation workflow', () => {
    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    expect(wrapper.text()).toContain('题目导入')
    expect(wrapper.text()).toContain('粘贴题目')
    expect(wrapper.text()).toContain('解析预览')
    expect(wrapper.text()).toContain('确认入库')
    expect(wrapper.text()).toContain('学习内容生成题库')
    expect(wrapper.text()).toContain('上传学习资料')
    expect(wrapper.text()).toContain('分析生成题库')
    const knowledgeEditor = wrapper.find('textarea[aria-label="Java 学习知识内容"]').element as HTMLTextAreaElement
    expect(knowledgeEditor.value).toContain('HashMap 默认负载因子是 0.75')
    const fileInput = wrapper.find('input[aria-label="上传 Java 学习资料"]')
    expect(fileInput.exists()).toBe(true)
    expect(fileInput.attributes('accept')).toContain('.docx')
    expect(fileInput.attributes('accept')).toContain('.pdf')
  })
})
