import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import ImportPage from './ImportPage.vue'
import { submitPendingQuestion, uploadKnowledgeFile, uploadQuestionFile } from '../api'

vi.mock('../api', () => ({
  createQuestion: vi.fn(),
  generateKnowledgeQuestions: vi.fn(),
  previewImport: vi.fn(),
  submitPendingQuestion: vi.fn(),
  uploadKnowledgeFile: vi.fn(),
  uploadQuestionFile: vi.fn()
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
    expect(wrapper.text()).toContain('个人题库')
    expect(wrapper.text()).toContain('申请公开')
    expect((wrapper.get('input[value="PERSONAL"]').element as HTMLInputElement).checked).toBe(true)
    expect(wrapper.text()).toContain('结构化题目导入')
    expect(wrapper.text()).toContain('解析预览')
    expect(wrapper.text()).toContain('提交审核')
    expect(wrapper.text()).toContain('学习内容生成题库')
    expect(wrapper.text()).toContain('上传 Java 学习资料')
    expect(wrapper.text()).toContain('分析生成题库')
    const questionFileInput = wrapper.find('input[aria-label="上传结构化题目文件"]')
    expect(questionFileInput.exists()).toBe(true)
    expect(questionFileInput.attributes('accept')).toContain('.json')
    expect(questionFileInput.attributes('accept')).toContain('.xlsx')
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
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
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
      targetScope: 'PUBLIC',
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

    await wrapper.get('input[value="PUBLIC"]').setValue(true)
    await wrapper.get('textarea[aria-label="解析预览第 1 题题干"]').setValue('说明 Java 中 final 的作用。')
    await wrapper.get('select[aria-label="解析预览第 1 题题型"]').setValue('SHORT_ANSWER')
    await wrapper.get('select[aria-label="解析预览第 1 题难度"]').setValue('INTERMEDIATE')
    await wrapper.get('input[aria-label="解析预览第 1 题知识点"]').setValue('Java 基础')
    await wrapper.get('textarea[aria-label="解析预览第 1 题答案"]').setValue('可修饰类、方法和变量。')
    await wrapper.get('textarea[aria-label="解析预览第 1 题解析"]').setValue('根据修饰目标含义不同。')
    await wrapper.get('button[aria-label="提交预览题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith({
      title: '说明 Java 中 final 的作用。',
      type: 'SHORT_ANSWER',
      difficulty: 'INTERMEDIATE',
      knowledgePoint: 'Java 基础',
      answer: '可修饰类、方法和变量。',
      analysis: '根据修饰目标含义不同。',
      targetScope: 'PUBLIC'
    })
    expect(wrapper.text()).toContain('已提交管理员审核')
    expect(wrapper.text()).toContain('申请公开')
  })

  it('previews a structured question file without submitting it', async () => {
    vi.mocked(uploadQuestionFile).mockResolvedValue([
      {
        title: '下列哪个是 Java 关键字？\nA. class\nB. hello',
        type: 'SINGLE_CHOICE',
        difficulty: 'BEGINNER',
        knowledgePoint: 'Java 基础',
        answer: 'A',
        analysis: 'class 是关键字。'
      }
    ])
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })
    const file = new File(['[]'], 'questions.json', { type: 'application/json' })
    const input = wrapper.get('input[aria-label="上传结构化题目文件"]')
    Object.defineProperty(input.element, 'files', { configurable: true, value: [file] })

    await input.trigger('change')
    await flushPromises()

    expect(uploadQuestionFile).toHaveBeenCalledWith(file)
    expect(submitPendingQuestion).not.toHaveBeenCalled()
    expect(wrapper.text()).toContain('解析 1 道题')
    expect((wrapper.get('textarea[aria-label="解析预览第 1 题题干"]').element as HTMLTextAreaElement).value)
      .toContain('Java 关键字')
  })

  it('adds and removes editable preview rows', async () => {
    const wrapper = mount(ImportPage, {
      global: { stubs: { RouterLink: routerLinkStub, LogoutButton: true } }
    })

    await wrapper.get('button[aria-label="新增解析预览题目"]').trigger('click')
    expect(wrapper.findAll('textarea[data-draft-field="title"]')).toHaveLength(2)
    await wrapper.get('button[aria-label="删除解析预览第 1 题"]').trigger('click')
    expect(wrapper.findAll('textarea[data-draft-field="title"]')).toHaveLength(1)
  })

  it('previews uploaded generated questions before submitting them for review', async () => {
    window.localStorage.setItem(
      'studyCollectionUser',
      JSON.stringify({ token: 'user-token', userId: 7, username: 'alice', displayName: 'Alice', role: 'USER' })
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
      targetScope: 'PERSONAL',
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
    expect((wrapper.get('textarea[aria-label="生成题库预览第 1 题题干"]').element as HTMLTextAreaElement).value)
      .toContain('JVM 栈和堆通常分别保存什么内容？')
    expect(wrapper.text()).toContain('请预览后提交审核')

    await wrapper.get('textarea[aria-label="生成题库预览第 1 题解析"]').setValue('用户已修正的 JVM 解析')

    await wrapper.find('button[aria-label="提交生成题审核"]').trigger('click')
    await flushPromises()

    expect(submitPendingQuestion).toHaveBeenCalledWith(expect.objectContaining({
      title: 'JVM 栈和堆通常分别保存什么内容？',
      knowledgePoint: 'JVM',
      analysis: '用户已修正的 JVM 解析',
      targetScope: 'PERSONAL'
    }))
  })
})
