import { describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'
import { createQuestion } from '../api'

vi.mock('../api', () => ({
  createQuestion: vi.fn(),
  generateKnowledgeQuestions: vi.fn(),
  previewImport: vi.fn(),
  uploadKnowledgeFile: vi.fn()
}))

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

  it('saves parsed preview questions when confirming import', async () => {
    vi.mocked(createQuestion).mockResolvedValue({
      id: 1,
      title: 'Java 中 int 默认值是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: 'Java 基础',
      answer: 'A',
      analysis: '由导入预览确认入库'
    })

    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await wrapper.find('button[aria-label="确认预览题入库"]').trigger('click')
    await flushPromises()

    expect(createQuestion).toHaveBeenCalledWith({
      title: expect.stringContaining('Java'),
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: expect.any(String),
      answer: 'A',
      analysis: '由导入预览确认入库'
    })
    expect(wrapper.text()).toContain('已入库')
  })
})
