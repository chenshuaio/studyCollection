import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'
import { submitPendingQuestion, uploadKnowledgeFile } from '../api'

vi.mock('../api', () => ({
  createQuestion: vi.fn(),
  generateKnowledgeQuestions: vi.fn(),
  previewImport: vi.fn(),
  submitPendingQuestion: vi.fn(),
  uploadKnowledgeFile: vi.fn()
}))

const routerLinkStub = {
  props: ['to'],
  template: '<a><slot /></a>'
}

describe('ImportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
  })

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
    expect(wrapper.text()).toContain('提交审核')
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

  it('submits parsed preview questions for administrator review', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
    )
    vi.mocked(submitPendingQuestion).mockResolvedValue({
      id: 1,
      submitterUserId: 7,
      title: 'Java 中 int 默认值是多少？',
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: 'Java 基础',
      answer: 'A',
      analysis: '由导入预览提交审核',
      status: 'PENDING'
    })

    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })

    await wrapper.find('button[aria-label="提交预览题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith({
      submitterUserId: 7,
      title: expect.stringContaining('Java'),
      type: 'SINGLE_CHOICE',
      difficulty: 'BEGINNER',
      knowledgePoint: expect.any(String),
      answer: 'A',
      analysis: '由导入预览提交审核'
    })
    expect(wrapper.text()).toContain('已提交管理员审核')
  })

  it('previews uploaded generated questions before submitting them for review', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
    )
    vi.mocked(uploadKnowledgeFile).mockResolvedValue([
      {
        title: 'JVM 栈和堆通常分别保存什么内容？',
        type: 'SHORT_ANSWER',
        difficulty: 'INTERMEDIATE',
        knowledgePoint: 'JVM',
        answer: '栈保存方法调用栈帧，堆保存对象实例',
        analysis: '上传资料生成的预览题'
      }
    ])
    vi.mocked(submitPendingQuestion).mockResolvedValue({
      id: 9,
      submitterUserId: 7,
      title: 'JVM 栈和堆通常分别保存什么内容？',
      type: 'SHORT_ANSWER',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: 'JVM',
      answer: '栈保存方法调用栈帧，堆保存对象实例',
      analysis: '上传资料生成的预览题',
      status: 'PENDING'
    })

    const wrapper = mount(ImportPage, {
      global: {
        stubs: {
          RouterLink: routerLinkStub,
          LogoutButton: true
        }
      }
    })
    const file = new File(['JVM 栈保存局部变量表，堆保存对象实例。'], 'jvm.xlsx', {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const fileInput = wrapper.find('input[aria-label="上传 Java 学习资料"]')
    Object.defineProperty(fileInput.element, 'files', {
      configurable: true,
      value: [file]
    })

    await fileInput.trigger('change')
    await flushPromises()

    expect(uploadKnowledgeFile).toHaveBeenCalledWith(file)
    expect(submitPendingQuestion).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('JVM 栈和堆通常分别保存什么内容？')
    expect(wrapper.text()).toContain('请预览后提交审核')

    await wrapper.find('button[aria-label="提交生成题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith(expect.objectContaining({
      submitterUserId: 7,
      title: 'JVM 栈和堆通常分别保存什么内容？',
      knowledgePoint: 'JVM'
    }))
  })
})
